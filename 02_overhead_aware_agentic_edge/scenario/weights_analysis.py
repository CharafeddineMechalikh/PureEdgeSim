"""Analyse the objective-weight sensitivity experiment."""
from __future__ import annotations

import sys
from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
from research_common import tables  # noqa: E402

RESULTS = ROOT / "02_overhead_aware_agentic_edge" / "results"
frame = pd.read_csv(RESULTS / "runs" / "weights" / "raw_runs.csv")

energy = frame[frame["container_penalty_s"] == 0.02].copy()
container = frame[frame["energy_weight"] == 0.02].copy()

energy_rows = []
for value, block in energy.groupby("energy_weight"):
    energy_rows.append({"Parameter": "energy $\\lambda$",
                        "Value": f"{value:g}",
                        "Success": block["success_ratio"].mean(),
                        "Fidelity-wtd.": block["fid_weighted"].mean()
                        if "fid_weighted" in block else float("nan"),
                        "Mean decisions": block["overhead_decisions"].mean()})
container_rows = []
for value, block in container.groupby("container_penalty_s"):
    container_rows.append({"Parameter": "container $\\beta$ (s)",
                           "Value": f"{value:g}",
                           "Success": block["success_ratio"].mean(),
                           "Fidelity-wtd.": block["fid_weighted"].mean()
                           if "fid_weighted" in block else float("nan"),
                           "Mean decisions": block["overhead_decisions"].mean()})

out = pd.DataFrame(energy_rows + container_rows)
for column in ("Success", "Fidelity-wtd.", "Mean decisions"):
    out[column] = out[column].map(
        lambda v: "--" if pd.isna(v) else f"{v:.4f}")
tables.write(out, RESULTS / "tables" / "weights.tex",
             "Sensitivity to the two scalar objective weights (aware policy, "
             "2x overhead, 10 seeds). Each block varies one parameter.",
             "tab:p2-weights", wide=False, size=r"\footnotesize",
             notes="If success and the decision counts are flat across a wide "
                   "weight range, the terms do not change the selected host; "
                   "that is evidence about this configuration, not about the "
                   "parameters in general.")
print(out.to_string(index=False))
