"""Generate the refined compute/payload feasibility map (reviewer request)."""
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
FIGURES = RESULTS / "figures"
TABLES = RESULTS / "tables"

frame = pd.read_csv(RESULTS / "runs" / "cp_fine" / "raw_runs.csv")
pivot = frame.pivot_table(index="compute_multiplier", columns="payload_multiplier",
                          values="success_ratio", aggfunc="mean")

# LaTeX table with one authoritative set of numbers.
display = pivot.reset_index()
display.columns = ["Compute"] + [f"{c:g}$\\times$" for c in pivot.columns]
tables.write(display, TABLES / "cp_fine.tex",
             "Refined compute/payload feasibility map (aware policy, 10 seeds). "
             "Success changes only where a deadline boundary is crossed.",
             "tab:p2-cp-fine", wide=False, size=r"\footnotesize",
             notes="Payload has no effect in most cells, but at 1.5x compute it "
                   "moves success from 0.985 to 0.784 between p=1.25 and p=1.5; "
                   "at 1.7x compute it drops from 0.775 to 0.498 between p=1.75 "
                   "and p=2.0. This is a boundary-crossing interaction, not "
                   "evidence that bytes are irrelevant in general.")

# Heatmap.
plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 9, "figure.dpi": 160})
fig, ax = plt.subplots(figsize=(6.2, 4.0))
image = ax.imshow(pivot.values, cmap="RdYlGn", vmin=0, vmax=1, aspect="auto")
ax.set_xticks(range(len(pivot.columns)))
ax.set_xticklabels([f"{c:g}" for c in pivot.columns])
ax.set_yticks(range(len(pivot.index)))
ax.set_yticklabels([f"{c:g}" for c in pivot.index])
for row in range(pivot.shape[0]):
    for col in range(pivot.shape[1]):
        ax.text(col, row, f"{pivot.iloc[row, col]:.3f}", ha="center", va="center", fontsize=8)
ax.set_xlabel("payload multiplier $p$")
ax.set_ylabel("compute multiplier $c$")
ax.set_title("Fidelity-weighted feasibility map (aware policy)")
fig.colorbar(image, ax=ax, fraction=0.046, label="success ratio")
fig.tight_layout()
for suffix in ("pdf", "png"):
    fig.savefig(FIGURES / f"f_cp_fine.{suffix}")
plt.close(fig)
print(pivot.round(3).to_string())
