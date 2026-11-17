"""Figures that the crossed analysis needs but the table code does not emit.

Kept separate so the figure code cannot silently fail inside the table writer:
each figure is built from an explicit frame and its existence is asserted.
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from research_common import figures, stats  # noqa: E402

RESULTS = ROOT / "01_sim2deploy_sagin_uav_benchmark" / "results"
LABEL = {"random": "random", "round_robin": "round robin", "trade_off": "trade-off",
         "minmin": "min-min", "leastloaded": "least loaded", "mct": "earliest finish",
         "qos": "Q-learning"}


def _frame() -> pd.DataFrame:
    return pd.read_csv(RESULTS / "runs" / "algo" / "raw_runs.csv")


def algorithm_interval() -> Path:
    frame = _frame()
    frame = frame[frame["architecture"] == "ALL"]
    rows = []
    for (policy, interval), block in frame.groupby(["policy", "update_interval"]):
        values = block["success_ratio"].to_numpy(dtype=float)
        low, high = stats.bootstrap_ci(values, np.mean)
        rows.append({"policy": LABEL.get(policy, policy), "update_interval": interval,
                     "mean": float(values.mean()), "ci_lo": float(low),
                     "ci_hi": float(high)})
    summary = pd.DataFrame(rows).sort_values("update_interval")
    path = RESULTS / "figures" / "f13_algo_interval"
    figures.line_with_ci(summary, "update_interval", "mean", "policy", path,
                         xlabel="Control interval (s)", ylabel="Success ratio",
                         title=None, logx=True, ylim=(0.0, 1.05), columns=2)
    assert path.with_suffix(".pdf").exists()
    return path.with_suffix(".pdf")


def latency_stages() -> Path:
    frame = pd.read_csv(RESULTS / "tables" / "latency_decomposition.csv")
    frame = frame[(frame["Architecture"] == "ALL") & (frame["Interval (s)"] == 1.0)]
    tidy = frame.melt(id_vars=["Algorithm", "Class"],
                      value_vars=["Release to decision (s)", "Decision to dispatch (s)",
                                  "Dispatch to exec (s)"],
                      var_name="Stage", value_name="Seconds")
    tidy["Stage"] = tidy["Stage"].str.replace(" (s)", "", regex=False)
    tidy["Sensing only"] = tidy["Class"] == "Sensing"
    path = RESULTS / "figures" / "f14_latency_stages"
    figures.stacked_bars(tidy, "Algorithm", "Seconds", "Stage", path,
                         xlabel="Algorithm (sensing class, 1 s interval)",
                         ylabel="Seconds", title=None, normalise=False)
    assert path.with_suffix(".pdf").exists()
    return path.with_suffix(".pdf")


def calibration() -> Path:
    frame = pd.read_csv(RESULTS / "tables" / "temporal_fidelity.csv")
    frame = frame[frame["Class"] != "Aggregation"].copy()
    order = ["0.1-0.25", "0.25-0.5", "0.5-1", "1-2", "2-4", ">4"]
    frame["bin"] = pd.Categorical(frame["Fidelity ratio bin"], categories=order, ordered=True)
    frame = frame.dropna(subset=["bin"]).sort_values(["Class", "bin"])
    path = RESULTS / "figures" / "f11_temporal_fidelity"
    figures.line_with_ci(frame, "Fidelity ratio bin", "mean_success", "Class", path,
                         xlabel="Decision latency / deadline", ylabel="Success ratio",
                         title=None, ylim=(0.0, 1.02), columns=2,
                         ci_lo=None, ci_hi=None)
    assert path.with_suffix(".pdf").exists()
    return path.with_suffix(".pdf")


def heterogeneity() -> Path:
    frame = pd.read_csv(RESULTS / "tables" / "heterogeneity.csv")
    frame["cell"] = (frame["Profile"].str.replace("_", " ") + "\n"
                     + frame["Interval (s)"].map("{:g} s".format))
    path = RESULTS / "figures" / "f16_heterogeneity"
    figures.bar_with_ci(frame, "cell", "Effect (pp)", "Interval (s)", path,
                        xlabel="Infrastructure profile", ylabel="Architecture effect (pp)",
                        title=None, ci_lo=None, ci_hi=None)
    assert path.with_suffix(".pdf").exists()
    return path.with_suffix(".pdf")


if __name__ == "__main__":
    for builder in (algorithm_interval, latency_stages, calibration, heterogeneity):
        print(builder.__name__, builder())
