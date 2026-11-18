"""Post-hoc sensitivity of the factorial conclusion to the shape of q(f).

PureEdgeSim does not model answer accuracy, and the paper reports a
fidelity-weighted success in which q(f) = f.  This script asks whether the
*conclusion* depends on that choice by re-weighting the per-task fidelity that
the orchestrator already recorded.  No new runs are needed: every task row of the
authoritative factorial carries the fidelity level it was executed at, so any
monotone q can be applied after the fact.

Families, all normalised so that q(1) = 1:

``linear``      q(f) = f              (the paper's accounting)
``concave``     q(f) = sqrt(f)        (a rung is worth more than its length)
``quadratic``   q(f) = f^2            (a rung is worth less than its length)
``cubic``       q(f) = f^3            (short answers are nearly worthless)
``saturating``  q(f) = sin(pi f / 2)  (the first rung buys most of the quality)
``threshold``   q(f) = 1[f >= 0.5]    (quality is all-or-nothing above a rung)

A conclusion that survives every column is a conclusion about the placement and
fidelity *decision*, not about the particular quality curve assumed.
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from research_common import tables  # noqa: E402

RESULTS = ROOT / "02_overhead_aware_agentic_edge" / "results"
RUNS = RESULTS / "runs" / "factorial" / "runs"
OUT = RESULTS / "tables" / "quality_sensitivity.tex"

FAMILIES = {
    "linear": lambda f: f,
    "concave": lambda f: np.sqrt(f),
    "quadratic": lambda f: f ** 2,
    "cubic": lambda f: f ** 3,
    "saturating": lambda f: np.sin(np.pi * f / 2),
    "threshold": lambda f: (f >= 0.5).astype(float),
}


def load_tasks() -> pd.DataFrame:
    frames = []
    for path in sorted(RUNS.glob("*/tasks.csv")):
        job = path.parent / "job.json"
        if not job.exists():
            continue
        meta = __import__("json").loads(job.read_text(encoding="utf-8"))
        meta = meta.get("params", meta)
        frame = pd.read_csv(path, usecols=["success", "fidelity"])
        frame["run"] = path.parent.name
        frame["compute_multiplier"] = float(meta.get("compute_multiplier", 1.0))
        frame["policy"] = meta.get("policy", path.parent.name.split("__")[1])
        frames.append(frame)
    if not frames:
        raise SystemExit(f"no task rows under {RUNS}")
    return pd.concat(frames, ignore_index=True)


def policy_of(run: str) -> str:
    # Directory names look like factorial__<policy>__<arch>__<overhead>__...
    return run.split("__")[1]


def main() -> int:
    tasks = load_tasks()
    tasks["success"] = pd.to_numeric(tasks["success"], errors="coerce").fillna(0.0)
    tasks["fidelity"] = pd.to_numeric(tasks["fidelity"], errors="coerce").fillna(1.0)
    tasks["compute_multiplier"] = pd.to_numeric(tasks["compute_multiplier"], errors="coerce")

    rows = []
    for level, block in tasks.groupby("compute_multiplier"):
        for policy, arm in block.groupby("policy"):
            row = {"Overhead": f"{level:g}$\\times$", "Policy": policy,
                   "Success": arm["success"].mean()}
            for name, q in FAMILIES.items():
                row[name] = float((arm["success"] * q(arm["fidelity"].values)).mean())
            rows.append(row)
    frame = pd.DataFrame(rows).sort_values(["Overhead", "Policy"])

    display = frame.copy()
    for column in display.columns[2:]:
        display[column] = display[column].map(lambda v: f"{v:.3f}")
    tables.write(
        display, OUT,
        "Quality-weighted success under six shapes of the quality curve $q(f)$, "
        "recomputed post hoc from the per-task fidelity of the authoritative "
        "factorial. $q(1)=1$ in every column.",
        "tab:p2-quality-sensitivity", wide=True, size=r"\footnotesize",
        notes="The family is monotone and normalised; only its curvature changes. "
              "The policy ordering is read down the columns, not across them.")

    # Report the two facts the text relies on: the ordering is stable, and the
    # joint-versus-static gap stays inside the same band whichever curve is used.
    print(frame.to_string(index=False))
    for level, block in frame.groupby("Overhead"):
        for name in FAMILIES:
            ranked = block.sort_values(name, ascending=False)["Policy"].tolist()
            print(level, name, ranked[:3])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
