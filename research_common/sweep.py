"""Deterministic experiment grids, parallel execution and resume.

A grid is described by a base job plus a mapping ``name -> [values]``.  The
cartesian product is expanded in a stable order, hashed into a short identifier,
and executed either serially or in a process pool.  Each job writes its own
directory, so an interrupted sweep resumes without recomputation.
"""
from __future__ import annotations

import hashlib
import importlib
import importlib.util
import json
import os
import time
import traceback
from concurrent.futures import ProcessPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Iterable

import pandas as pd


def expand_grid(base: dict[str, Any], sweep: dict[str, Iterable[Any]]) -> list[dict[str, Any]]:
    """Cartesian product with a deterministic (declaration-order) traversal."""
    keys = list(sweep)
    jobs: list[dict[str, Any]] = [dict(base)]
    for key in keys:
        values = list(sweep[key])
        if not values:
            raise ValueError(f"Sweep dimension {key!r} is empty")
        jobs = [{**job, key: value} for job in jobs for value in values]
    return jobs


def job_id(job: dict[str, Any], length: int = 12) -> str:
    payload = json.dumps(_normalise(job), sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()[:length]


def readable_name(job: dict[str, Any], keys: Iterable[str] | None = None) -> str:
    keys = list(keys or job)
    parts = []
    for key in keys:
        if key not in job:
            continue
        value = job[key]
        if isinstance(value, float):
            text = f"{value:g}".replace(".", "p")
        else:
            text = str(value)
        parts.append(f"{key}-{text}")
    return "__".join(parts)


def _normalise(value):
    if isinstance(value, dict):
        return {str(key): _normalise(item) for key, item in sorted(value.items())}
    if isinstance(value, (list, tuple)):
        return [_normalise(item) for item in value]
    if isinstance(value, Path):
        return str(value)
    if isinstance(value, (str, int, float, bool)) or value is None:
        return value
    return repr(value)


def _resolve(path: str) -> Callable:
    module_name, _, attribute = path.rpartition(":")
    if module_name.endswith(".py") or "/" in module_name or "\\" in module_name:
        import sys

        file_path = Path(module_name).resolve()
        if str(file_path.parent) not in sys.path:
            sys.path.insert(0, str(file_path.parent))
        spec = importlib.util.spec_from_file_location(f"experiment_{abs(hash(str(file_path)))}", file_path)
        if spec is None or spec.loader is None:
            raise ImportError(f"Cannot load experiment module: {file_path}")
        module = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = module
        spec.loader.exec_module(module)
        return getattr(module, attribute)
    module = importlib.import_module(module_name)
    return getattr(module, attribute)


def _worker(payload: dict) -> dict:
    import sys

    root = Path(payload["root"])
    if str(root) not in sys.path:
        sys.path.insert(0, str(root))
    from research_common.paths import ensure_pureedgesim

    ensure_pureedgesim()
    job = payload["job"]
    out_dir = Path(payload["out_dir"])
    function = _resolve(payload["function"])
    started = time.perf_counter()
    try:
        result = function(job, out_dir)
        error = None
    except Exception:  # pragma: no cover - surfaced in the summary
        result = None
        error = traceback.format_exc()
    elapsed = time.perf_counter() - started
    if result is not None and isinstance(result, dict):
        result.setdefault("job_id", payload["job_id"])
        result.setdefault("wall_clock_s", round(elapsed, 3))
    return {"job_id": payload["job_id"], "out_dir": str(out_dir), "result": result,
            "error": error, "elapsed_s": round(elapsed, 3), "job": job}


@dataclass
class SweepResult:
    frame: pd.DataFrame
    failures: list[dict]
    seconds: float
    grid_path: Path

    def __len__(self) -> int:  # pragma: no cover - convenience
        return len(self.frame)


def execute(jobs: list[dict[str, Any]], function_path: str, out_root: Path | str,
            *, prefix: str = "job", workers: int = 1, resume: bool = True,
            keep_failed: bool = True, name_keys: Iterable[str] | None = None,
            verbose: bool = True) -> SweepResult:
    """Run every job; results are appended to ``raw_runs.csv`` under ``out_root``."""
    started = time.perf_counter()
    root = Path.cwd()
    out_root = Path(out_root)
    out_root.mkdir(parents=True, exist_ok=True)
    grid_path = out_root / "grid.json"
    grid_path.write_text(json.dumps([_normalise(job) for job in jobs], indent=2), encoding="utf-8")

    prepared = []
    for job in jobs:
        identifier = job_id(job)
        name = readable_name(job, name_keys)
        directory = out_root / "runs" / (f"{prefix}__{name}-{identifier[:6]}" if name
                                         else f"{prefix}__{identifier}")
        directory.mkdir(parents=True, exist_ok=True)
        (directory / "job.json").write_text(
            json.dumps({"job_id": identifier, "params": _normalise(job)}, indent=2), encoding="utf-8")
        prepared.append((identifier, directory, job))

    pending = []
    for identifier, directory, job in prepared:
        row_path = directory / "row.json"
        if resume and row_path.is_file():
            continue
        pending.append((identifier, directory, job))

    rows: list[dict] = []
    failures: list[dict] = []
    payloads = [{"root": str(root), "function": function_path, "job_id": identifier,
                 "out_dir": str(directory), "job": job} for identifier, directory, job in pending]

    def collect(payload, outcome: dict) -> None:
        if outcome["error"]:
            failures.append({"job_id": outcome["job_id"], "job": outcome["job"],
                             "error": outcome["error"], "out_dir": outcome["out_dir"]})
            return
        result = outcome["result"]
        if result:
            rows.append({"job_id": outcome["job_id"], **outcome["job"], **_flatten(result)})

    if workers <= 1:
        for payload in payloads:
            collect(payload, _worker(payload))
    else:
        with ProcessPoolExecutor(max_workers=workers) as pool:
            futures = {pool.submit(_worker, payload): payload for payload in payloads}
            for future in as_completed(futures):
                collect(futures[future], future.result())

    if resume:
        rows.extend(_load_existing(out_root))

    frame = pd.DataFrame(_deduplicate(rows))
    if not frame.empty:
        frame.to_csv(out_root / "raw_runs.csv", index=False)
    if failures:
        (out_root / "failures.json").write_text(json.dumps(failures, indent=2), encoding="utf-8")
        if not keep_failed:
            raise RuntimeError(f"{len(failures)} job(s) failed; see {out_root / 'failures.json'}")
    summary = {
        "jobs": len(jobs),
        "executed": len(pending),
        "succeeded": len(frame),
        "failed": len(failures),
        "workers": workers,
        "seconds": round(time.perf_counter() - started, 3),
    }
    (out_root / "sweep_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    if verbose:
        print(f"[sweep] {summary['succeeded']}/{summary['jobs']} jobs, "
              f"{summary['failed']} failed, {summary['seconds']:.1f}s, {out_root}")
    return SweepResult(frame=frame, failures=failures, seconds=time.perf_counter() - started,
                       grid_path=grid_path)


def _load_existing(out_root: Path) -> list[dict]:
    rows = []
    for row_path in sorted((out_root / "runs").glob("*/row.json")):
        try:
            payload = json.loads(row_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        job_path = row_path.parent / "job.json"
        job = {}
        identifier = row_path.parent.name.split("-")[-1]
        if job_path.is_file():
            try:
                job_payload = json.loads(job_path.read_text(encoding="utf-8"))
                identifier = job_payload.get("job_id", identifier)
                job = job_payload.get("params", {})
            except (OSError, json.JSONDecodeError):
                job = {}
        rows.append({"job_id": identifier, **job, **_flatten(payload)})
    return rows


def _flatten(result: dict) -> dict:
    flat = {}
    for key, value in result.items():
        if key == "job":
            continue
        if isinstance(value, dict):
            flat[key] = json.dumps(_normalise(value), sort_keys=True)
        elif isinstance(value, (list, tuple)):
            flat[key] = json.dumps(_normalise(value))
        else:
            flat[key] = value
    return flat


def _deduplicate(rows: list[dict]) -> list[dict]:
    seen: dict[str, dict] = {}
    for row in rows:
        identifier = row.get("job_id")
        if identifier:
            seen[identifier] = row
        else:
            seen[str(id(row))] = row
    return [seen[key] for key in sorted(seen)]


def default_workers() -> int:
    return max(1, min(8, (os.cpu_count() or 2) - 1))
