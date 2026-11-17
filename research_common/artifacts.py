"""Artifact provenance: hashes, environment capture, manifest, data dictionary, zip."""
from __future__ import annotations

import hashlib
import json
import platform
import subprocess
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Mapping, Sequence

SKIP = {".git", "__pycache__", ".pytest_cache", ".mypy_cache", ".ipynb_checkpoints"}


def sha256_file(path: Path | str, chunk: int = 1 << 20) -> str:
    digest = hashlib.sha256()
    with Path(path).open("rb") as handle:
        while True:
            block = handle.read(chunk)
            if not block:
                break
            digest.update(block)
    return digest.hexdigest()


def git_commit(repo: Path | str) -> dict:
    repo = Path(repo)
    try:
        commit = subprocess.run(["git", "-C", str(repo), "rev-parse", "HEAD"], capture_output=True,
                                text=True, check=True).stdout.strip()
        branch = subprocess.run(["git", "-C", str(repo), "rev-parse", "--abbrev-ref", "HEAD"],
                                capture_output=True, text=True, check=True).stdout.strip()
        dirty = bool(subprocess.run(["git", "-C", str(repo), "status", "--porcelain"],
                                    capture_output=True, text=True, check=True).stdout.strip())
        return {"commit": commit, "branch": branch, "dirty": dirty}
    except (OSError, subprocess.CalledProcessError):
        return {"commit": None, "branch": None, "dirty": None}


def environment() -> dict:
    packages = {}
    for name in ("numpy", "pandas", "scipy", "matplotlib"):
        try:
            module = __import__(name)
            packages[name] = getattr(module, "__version__", None)
        except ImportError:  # pragma: no cover
            packages[name] = None
    from .paths import ROOT, PY_SRC

    return {
        "timestamp": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "python": sys.version.split()[0],
        "platform": platform.platform(),
        "machine": platform.machine(),
        "processor": platform.processor(),
        "packages": packages,
        "repository": git_commit(ROOT),
        "pureedgesim_python": git_commit(PY_SRC.parent.parent) if PY_SRC.exists() else None,
        "pureedgesim_java": git_commit(ROOT / "PureEdgeSim") if (ROOT / "PureEdgeSim").exists() else None,
    }


def iter_files(root: Path | str, skip: Sequence[str] = ("manifest.json",)):
    root = Path(root)
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(root)
        if any(part in SKIP for part in relative.parts):
            continue
        if relative.as_posix() in skip:
            continue
        yield path, relative


def write_manifest(root: Path | str, *, extra: Mapping | None = None,
                   skip: Sequence[str] = ("manifest.json",)) -> dict:
    """Hash every artifact under ``root`` and write ``manifest.json``."""
    root = Path(root)
    root.mkdir(parents=True, exist_ok=True)
    files = []
    for path, relative in iter_files(root, skip):
        files.append({"path": relative.as_posix(), "bytes": path.stat().st_size,
                      "sha256": sha256_file(path)})
    manifest = {
        "root": root.name,
        "generated": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "environment": environment(),
        "file_count": len(files),
        "total_bytes": sum(item["bytes"] for item in files),
        "files": files,
    }
    if extra:
        manifest["extra"] = dict(extra)
    (root / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    return manifest


def write_data_dictionary(path: Path | str, columns: Mapping[str, Mapping | str],
                          title: str = "Data dictionary") -> Path:
    """Write a Markdown table describing every column of the result files."""
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    lines = [f"# {title}", "",
             "| Column | Unit | Meaning | Source |",
             "|---|---|---|---|"]
    for name, spec in columns.items():
        if isinstance(spec, str):
            spec = {"meaning": spec}
        lines.append("| `{}` | {} | {} | {} |".format(
            name, spec.get("unit", "-"), spec.get("meaning", "-"), spec.get("source", "-")))
    target.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return target


def bundle(root: Path | str, destination: Path | str, *, include: Sequence[str] = ("**/*",)) -> Path:
    """Zip an artifact directory, preserving relative paths and a top-level folder."""
    root = Path(root)
    destination = Path(destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        seen: set[Path] = set()
        for pattern in include:
            for path in sorted(root.glob(pattern)):
                if not path.is_file() or path in seen:
                    continue
                relative = path.relative_to(root)
                if any(part in SKIP for part in relative.parts):
                    continue
                seen.add(path)
                archive.write(path, (Path(root.name) / relative).as_posix())
    return destination


def verify_manifest(root: Path | str) -> list[str]:
    """Return mismatching paths relative to the recorded manifest (empty = intact)."""
    root = Path(root)
    manifest_path = root / "manifest.json"
    if not manifest_path.is_file():
        return ["manifest.json"]
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    mismatches = []
    for item in manifest.get("files", []):
        path = root / item["path"]
        if not path.is_file() or sha256_file(path) != item["sha256"]:
            mismatches.append(item["path"])
    return mismatches
