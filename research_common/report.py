"""Shared reporting helpers: aggregation, acceptance checks and result indexes."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Iterable, Mapping, Sequence

import numpy as np
import pandas as pd

from . import stats


def aggregate(frame: pd.DataFrame, group: Sequence[str], metrics: Sequence[str],
              n_boot: int = 2000) -> pd.DataFrame:
    """Mean, SD, 95% bootstrap CI and n for each metric and group."""
    rows = []
    for keys, subset in frame.groupby(list(group), dropna=False, sort=True):
        keys = keys if isinstance(keys, tuple) else (keys,)
        base = dict(zip(group, keys))
        for metric in metrics:
            if metric not in subset:
                continue
            sample = subset[metric].to_numpy(dtype=float)
            sample = sample[np.isfinite(sample)]
            if sample.size == 0:
                continue
            lo, hi = stats.bootstrap_ci(sample, np.mean, n_boot=n_boot)
            rows.append({
                **base,
                "metric": metric,
                "n": int(sample.size),
                "mean": float(sample.mean()),
                "sd": float(sample.std(ddof=1)) if sample.size > 1 else float("nan"),
                "median": float(np.median(sample)),
                "ci_lo": lo,
                "ci_hi": hi,
            })
    return pd.DataFrame(rows)


def wide_aggregate(frame: pd.DataFrame, group: Sequence[str], metrics: Sequence[str],
                   n_boot: int = 2000) -> pd.DataFrame:
    """One row per group with ``<metric>``, ``<metric>_lo``/``_hi`` columns."""
    long = aggregate(frame, group, metrics, n_boot)
    if long.empty:
        return long
    wide = long.pivot_table(index=list(group), columns="metric", values="mean", aggfunc="mean").reset_index()
    for metric in metrics:
        if metric not in long["metric"].values:
            continue
        subset = long[long["metric"] == metric].set_index(list(group))
        wide[f"{metric}_lo"] = wide.set_index(list(group)).index.map(subset["ci_lo"])
        wide[f"{metric}_hi"] = wide.set_index(list(group)).index.map(subset["ci_hi"])
        wide[f"{metric}_sd"] = wide.set_index(list(group)).index.map(subset["sd"])
    return wide


def acceptance(criteria: Mapping[str, bool], details: Mapping[str, str] | None = None) -> dict:
    details = details or {}
    passed = sum(bool(value) for value in criteria.values())
    return {
        "criteria": {key: {"passed": bool(value), "detail": details.get(key, "")}
                     for key, value in criteria.items()},
        "passed": passed,
        "total": len(criteria),
        "all_passed": passed == len(criteria),
    }


def write_json(path: Path | str, payload) -> Path:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(payload, indent=2, default=_default), encoding="utf-8")
    return target


def _default(value):
    if isinstance(value, (np.integer, np.floating)):
        return value.item()
    if isinstance(value, np.ndarray):
        return value.tolist()
    if isinstance(value, (set, tuple)):
        return list(value)
    if isinstance(value, float) and not np.isfinite(value):
        return None
    return str(value)


def headline(frame: pd.DataFrame, metric: str, group: Sequence[str]) -> pd.DataFrame:
    """Latest-value summary used in the results README."""
    if metric not in frame:
        return pd.DataFrame(columns=[*group, metric])
    return (frame.groupby(list(group), dropna=False)[metric]
            .agg(["count", "mean", "std", "min", "max"]).reset_index())


def metric_dictionary(columns: Iterable[str]) -> dict:
    """Fallback descriptions for derived metric names."""
    descriptions = {
        "success_ratio": ("fraction", "Successfully executed tasks / generated tasks", "aggregate row"),
        "task_success_fraction": ("fraction", "Per-task success fraction", "task records"),
        "energy_per_success_wh": ("Wh/task", "Energy per successfully executed task", "aggregate row"),
        "energy_per_task_wh": ("Wh/task", "Energy per generated task", "aggregate row"),
        "success_per_wh": ("tasks/Wh", "Success throughput per watt-hour", "aggregate row"),
        "task_delay_p95_s": ("s", "95th percentile task delivery delay", "task records"),
        "task_delay_mean_s": ("s", "Mean task delivery delay", "task records"),
        "wan_usage_s": ("s", "Cumulative WAN usage time", "aggregate row"),
        "traffic_mb": ("MB", "Total network traffic", "aggregate row"),
        "request_success_ratio": ("fraction", "Successful requests / generated requests", "request records"),
        "request_makespan_mean_s": ("s", "Mean successful end-to-end request makespan", "request records"),
        "request_makespan_p95_s": ("s", "95th percentile request makespan", "request records"),
        "wall_clock_s": ("s", "Harness wall-clock runtime", "harness"),
    }
    out = {}
    for column in columns:
        unit, meaning, source = descriptions.get(column, ("-", "Derived by the harness", "harness"))
        out[column] = {"unit": unit, "meaning": meaning, "source": source}
    return out
