"""Aggregate the P1-CBS common-scenario results across simulator adapters.

Each adapter writes a per-job CSV with the schema in crosssim/definitions.md
plus the P1-CBS fields (scenario, arm, platform). This script produces the
cross-simulator exhibits for Levels B and C:

* ``tables/common_level_b.tex`` — success by simulator x architecture x tick.
* ``tables/common_level_c.tex`` — deployment/tier and latency comparison.
* ``results/common_summary.csv`` — machine-readable aggregate.

Only simulators whose adapter has actually run appear in the output.
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import pandas as pd

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from research_common import tables  # noqa: E402
RESULTS = HERE / "results"
TABLES = HERE / "tables"
TABLES.mkdir(parents=True, exist_ok=True)

LEVEL_B_ORDER = ["1 s", "200 ms", "50 ms", "event-driven"]


def load() -> pd.DataFrame:
    frames = []
    for path in sorted(RESULTS.glob("common_*/*.csv")):
        if path.name.startswith("ev_"):
            continue
        frame = pd.read_csv(path)
        if "platform" in frame and "tick_s" in frame:
            frame["source_file"] = path.name
            frames.append(frame)
    if not frames:
        return pd.DataFrame()
    frame = pd.concat(frames, ignore_index=True)
    # EdgeCloudSim has no global periodic tick; NaN is recorded as 0 (event-driven
    # limit) with the platform's capability limitation documented in CAPABILITIES.md.
    frame["tick_s"] = frame["tick_s"].fillna(0.0)
    # A smoke run may have appended a cell before the full grid; keep one row
    # per (platform, architecture, tick, seed, workflow).
    subset = ["platform", "architecture", "tick_s", "seed", "workflow_id"]
    return frame.drop_duplicates(subset=subset).reset_index(drop=True)


def tick_label(value) -> str:
    if pd.isna(value):
        return "event-driven"
    return {1.0: "1 s", 0.2: "200 ms", 0.05: "50 ms", 0.0: "event-driven"}.get(
        float(value), f"{value:g} s")


def summarise(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    keys = ["platform", "architecture", "tick_s"]
    if frame.empty:
        return pd.DataFrame()
    for (platform, arch, tick), block in frame.groupby(keys):
        per_seed = block.groupby("seed")["success"].mean()
        lat = block["orchestration_latency_s"].dropna()
        rows.append({
            "platform": platform,
            "architecture": arch,
            "tick_s": tick,
            "tick": tick_label(tick),
            "success": float(per_seed.mean()),
            "success_sd": float(per_seed.std(ddof=1)) if len(per_seed) > 1 else float("nan"),
            "jobs": int(len(block)),
            "seeds": int(block["seed"].nunique()),
            "orch_latency_s": float(lat.mean()) if len(lat) else float("nan"),
            "delay_p95_s": float(np.percentile(block["completion_delay_s"].dropna(), 95))
            if block["completion_delay_s"].notna().any() else float("nan"),
        })
    table = pd.DataFrame(rows)
    return table.sort_values(["platform", "architecture", "tick_s"]).reset_index(drop=True)


def build() -> dict[str, Path]:
    frame = load()
    if frame.empty:
        print("[common] no adapter results yet")
        return {}
    table = summarise(frame)
    table.to_csv(RESULTS / "common_summary.csv", index=False)

    # Level B: success by simulator x architecture x tick
    pivot = table.pivot_table(index=["platform", "architecture"], columns="tick",
                              values="success", aggfunc="mean").reset_index()
    pivot = pivot[[c for c in ["platform", "architecture", *LEVEL_B_ORDER] if c in pivot]]
    outputs = {}
    outputs["level_b"] = tables.write(
        pivot, TABLES / "common_level_b.tex",
        "Common-scenario temporal calibration (Level B): deadline success by "
        "simulator, deployment architecture and temporal setting.",
        "tab:common-level-b", wide=True, size=r"\footnotesize",
        notes="Each cell is the mean of seed-level success ratios over the "
              "shared P1-CBS trace. Only adapters that have been executed "
              "appear; a blank cell is not a zero.")

    # Level C: platform x architecture at the default tick, with tier and latency
    default = table[table["tick_s"] == 1.0]
    if not default.empty:
        display = default[["platform", "architecture", "success", "orch_latency_s",
                           "delay_p95_s", "jobs", "seeds"]]
        outputs["level_c"] = tables.write(
            display, TABLES / "common_level_c.tex",
            "Common-scenario behavioural comparison (Level C) at the 1 s default "
            "control interval.", "tab:common-level-c", wide=True,
            size=r"\footnotesize",
            notes="The reference workload, infrastructure and deployment sets are "
                  "identical by specification; simulator-internal event semantics "
                  "are deliberately allowed to differ.")
    print(table.to_string(index=False))
    return {key: Path(value) for key, value in outputs.items()}


if __name__ == "__main__":
    build()
