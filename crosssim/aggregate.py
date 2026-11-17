"""Aggregate the YAFS replication into the cross-simulator exhibits.

Inputs are the per-task CSVs written by ``yafs_impl/run_yafs.py``.  The
deadline factor is part of the filename because a scaled-deadline run is a
different experiment, not a different seed.

Everything reported here is computed from those rows; nothing is transcribed
by hand.
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd

HERE = Path(__file__).resolve().parent
RESULTS = HERE / "results"
TABLES = HERE / "tables"
TABLES.mkdir(parents=True, exist_ok=True)

ARCH_LABEL = {"ALL": "all tiers", "EDGE_AND_CLOUD": "edge+cloud",
              "EDGE_ONLY": "edge only"}
TICK_LABEL = {0.0: "event-driven", 0.05: "50 ms", 0.2: "200 ms", 1.0: "1 s"}


def load() -> pd.DataFrame:
    frames = []
    for path in sorted((RESULTS / "yafs").glob("tasks_df*.csv")):
        frame = pd.read_csv(path)
        factor = float(path.stem.replace("tasks_df", "").replace("p", "."))
        frame["deadline_factor"] = factor
        frames.append(frame)
    return pd.concat(frames, ignore_index=True)


def _ci(values: np.ndarray, n_boot: int = 4000, seed: int = 0):
    values = values[np.isfinite(values)]
    if values.size < 2:
        return (float("nan"), float("nan"))
    rng = np.random.default_rng(seed)
    draws = rng.choice(values, size=(n_boot, values.size), replace=True).mean(axis=1)
    return (float(np.percentile(draws, 2.5)), float(np.percentile(draws, 97.5)))


def summarise(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for (arch, tick, factor), block in frame.groupby(["architecture", "tick_s", "deadline_factor"]):
        runs = block.groupby(["run_id", "seed"])["success"].mean()
        delay = block.loc[block["success"] == 1, "completion_delay_s"]
        lat = block["orchestration_latency_s"].dropna()
        rows.append({
            "platform": "YAFS-3.1",
            "architecture": ARCH_LABEL.get(arch, arch),
            "temporal_setting": TICK_LABEL.get(float(tick), f"{tick:g} s"),
            "deadline_factor": factor,
            "success_ratio": float(runs.mean()),
            "success_ci_lo": _ci(runs.to_numpy(dtype=float))[0],
            "success_ci_hi": _ci(runs.to_numpy(dtype=float))[1],
            "delay_mean_s": float(delay.mean()) if len(delay) else float("nan"),
            "delay_p95_s": float(np.percentile(delay, 95)) if len(delay) else float("nan"),
            "decision_latency_s": float(lat.mean()) if len(lat) else float("nan"),
            "tasks": int(len(block)),
            "n_runs": int(block["run_id"].nunique()),
        })
    order = {"all tiers": 0, "edge+cloud": 1, "edge only": 2}
    table = pd.DataFrame(rows)
    table["_arch"] = table["architecture"].map(order)
    table = (table.sort_values(["deadline_factor", "temporal_setting", "_arch"])
             .drop(columns="_arch").reset_index(drop=True))
    return table


def effect(frame: pd.DataFrame) -> pd.DataFrame:
    wide = frame.pivot_table(index=["deadline_factor", "temporal_setting"],
                             columns="architecture", values="success_ratio")
    rows = []
    for index, row in wide.iterrows():
        if not {"all tiers", "edge only"} <= set(wide.columns):
            continue
        if pd.isna(row["all tiers"]) or pd.isna(row["edge only"]):
            continue
        rows.append({"deadline_factor": index[0], "temporal_setting": index[1],
                     "all_tiers": row["all tiers"],
                     "edge_only": row["edge only"],
                     "effect_pp": 100.0 * (row["all tiers"] - row["edge only"])})
    return pd.DataFrame(rows)


def latex(table: pd.DataFrame, path: Path) -> None:
    lines = [
        r"\begin{table}[t]",
        r"\centering",
        r"\footnotesize",
        r"\caption{Independent replication in YAFS~3.1 of the temporal-resolution "
        r"experiment. The control loop is the same logical parameter as in "
        r"PureEdgeSim; ``event-driven'' means the placement is made at release with "
        r"no periodic tick. Three seeds per cell.}",
        r"\label{tab:crosssim-replication}",
        r"\begin{tabular}{@{}llcccc@{}}",
        r"\toprule",
        r"DF & Setting & Arch. & Success & p95 (s) & Dec. (s)\\",
        r"\midrule",
    ]
    for _, row in table.iterrows():
        lines.append(
            f"{row['deadline_factor']:g} & {row['temporal_setting']} & {row['architecture']} & "
            f"{row['success_ratio']:.3f} & {row['delay_p95_s']:.2f} & {row['decision_latency_s']:.2f}\\\\")
    lines += [r"\bottomrule", r"\end{tabular}", r"\end{table}", ""]
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    frame = load()
    table = summarise(frame)
    table.to_csv(RESULTS / "replication.csv", index=False)
    effect(table).to_csv(RESULTS / "replication_effect.csv", index=False)
    latex(table, TABLES / "replication.tex")
    print(table.to_string(index=False))
    print()
    print(effect(table).to_string(index=False))


if __name__ == "__main__":
    main()
