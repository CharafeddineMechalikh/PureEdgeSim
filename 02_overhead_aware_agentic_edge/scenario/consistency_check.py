"""Machine-generated headline-number consistency check for P2.

The principal factorial (20 seeds) is authoritative. Other grids use 10 seeds
and different populations; the report distinguishes them explicitly rather than
silently replacing one value with another.
"""
from __future__ import annotations

import sys
from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))
RESULTS = ROOT / "02_overhead_aware_agentic_edge" / "results"
TABLES = RESULTS / "tables"
PAPER = (ROOT / "02_overhead_aware_agentic_edge" / "paper" / "main.tex").read_text(
    encoding="utf-8")


def load(name: str) -> pd.DataFrame:
    return pd.read_csv(TABLES / f"{name}.csv")


def main() -> None:
    print("=" * 78)
    print("P2 HEADLINE NUMBER AUDIT")
    print("=" * 78)
    factorial = load("factorial")
    print("\n--- PRINCIPAL FACTORIAL (20 seeds, authoritative) ---")
    for level in (1.0, 1.5, 2.0, 3.0):
        block = factorial[factorial["Overhead"] == f"{level:g}$\\times$"]
        if block.empty:
            block = factorial[factorial["Overhead"] == f"{level}$\\times$"]
        for policy in ("edge", "aware", "naive_fid", "aware_nofid"):
            row = block[block["Arm"] == policy] if "Arm" in block else pd.DataFrame()
            if row.empty:
                for column in block.columns:
                    values = block[column].astype(str).tolist()
                    if policy.replace("_", "") in "".join(values).replace("_", ""):
                        print(f"{level:4g}x  {policy:14s} -> {values}")
                        break
            else:
                print(f"{level:4g}x  {policy:14s} -> {row.to_dict('records')}")

    print("\n--- RAW FACTORIAL CSV (exact) ---")
    raw = pd.read_csv(RESULTS / "runs" / "factorial" / "raw_runs.csv")
    summary = raw.pivot_table(index="compute_multiplier", columns="policy",
                              values="success_ratio", aggfunc="mean")
    print(summary.round(4).to_string())
    quality = pd.read_csv(RESULTS / "runs" / "factorial" / "raw_runs.csv")
    if "fid_weighted" in quality:
        print("\nfidelity-weighted success:")
        print(quality.pivot_table(index="compute_multiplier", columns="policy",
                                  values="fid_weighted", aggfunc="mean").round(4).to_string())

    print("\n--- OVERHEAD-SCALING GRID (10 seeds) ---")
    scaled = pd.read_csv(RESULTS / "runs" / "multiplier" / "raw_runs.csv")
    print(scaled.pivot_table(index="compute_multiplier", columns="policy",
                             values="success_ratio", aggfunc="mean").round(4).to_string())

    print("\n--- PAPER CLAIM COUNTS ---")
    for claim in ("0.496", "0.498", "0.782", "0.784", "0.373", "0.374",
                  "0.248", "0.249", "0.400", "0.401", "76\\%", "24\\%"):
        print(f"  {claim:8s} appears {PAPER.count(claim):2d} times")


if __name__ == "__main__":
    main()
