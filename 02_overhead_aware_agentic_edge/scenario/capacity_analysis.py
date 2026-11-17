"""Analyse the capacity-ratio experiment and emit a table plus regime figure."""
from __future__ import annotations

import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pandas as pd

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from research_common import tables  # noqa: E402

RESULTS = ROOT / "02_overhead_aware_agentic_edge" / "results"
frame = pd.read_csv(RESULTS / "runs" / "capacity" / "raw_runs.csv")
frame["rho"] = frame["edge_capacity_factor"] / frame["cloud_capacity_factor"]

pivot = frame.pivot_table(index=["rho", "compute_multiplier"], columns="policy",
                          values="success_ratio", aggfunc="mean").reset_index()
pivot = pivot.rename(columns={"rho": "$\\rho$ (edge/cloud MIPS)",
                              "compute_multiplier": "Overhead"})
display = pivot.copy()
for column in display.columns[2:]:
    display[column] = display[column].map(lambda v: f"{v:.3f}")
tables.write(display, RESULTS / "tables" / "capacity.tex",
             "Success by edge/cloud capacity ratio $\\rho$ at two overhead "
             "levels. The cloud MIPS are fixed; the edge tier is scaled.",
             "tab:p2-capacity", wide=True, size=r"\footnotesize",
             notes="A small $\\rho$ is a weak edge; a large $\\rho$ is a strong "
                   "edge. The comparison uses identical arrivals and workload.")

plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 9, "figure.dpi": 160})
fig, axes = plt.subplots(1, 2, figsize=(10, 3.6), sharey=True)
for ax, level in zip(axes, (2.0, 3.0)):
    subset = frame[frame["compute_multiplier"] == level]
    for policy, block in subset.groupby("policy"):
        grouped = block.groupby("rho")["success_ratio"].agg(["mean", "std"])
        ax.errorbar(grouped.index, grouped["mean"], yerr=1.96 * grouped["std"] / 3,
                    marker="o", label=policy)
    ax.set_xscale("log")
    ax.set_title(f"{level:g}x overhead")
    ax.set_xlabel(r"$\rho$ = edge MIPS / cloud MIPS")
    ax.grid(alpha=0.15)
axes[0].set_ylabel("success ratio")
axes[0].legend(frameon=False, fontsize=7)
fig.suptitle("Capacity-dependent orchestration regime")
fig.tight_layout()
for suffix in ("pdf", "png"):
    fig.savefig(RESULTS / "figures" / f"f_capacity.{suffix}")
plt.close(fig)
print(pivot.to_string(index=False))
