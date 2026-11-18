"""Analyse the fidelity-ladder granularity and resource-contention grids."""
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
LADDER = RESULTS / "runs" / "fidelity_ladder" / "raw_runs.csv"
CONTENTION = RESULTS / "runs" / "contention" / "raw_runs.csv"
MULTIPLIER = RESULTS / "runs" / "multiplier" / "raw_runs.csv"

ORDER = {"two": 1, "three": 2, "four": 3, "six": 4}
POLICY_LABEL = {"aware": "Joint + ladder", "naive_fid": "Default + ladder",
                "edge": "Static edge + ladder", "naive": "Default, no ladder"}


def ladder_table() -> pd.DataFrame:
    frame = pd.read_csv(LADDER)
    frame["ladder"] = pd.Categorical(frame["ladder"], list(ORDER), ordered=True)
    grouped = frame.groupby(["compute_multiplier", "ladder", "policy"], observed=True).agg(
        success=("success_ratio", "mean"),
        fidelity=("overhead_mean_fidelity", "mean"),
        low=("overhead_low_fidelity_rate", "mean"),
        p95=("task_delay_p95_s", "mean"),
        energy=("energy_per_success_wh", "mean")).reset_index()
    grouped["ladder"] = grouped["ladder"].astype(str)
    grouped["policy"] = grouped["policy"].map(POLICY_LABEL).fillna(grouped["policy"])
    display = grouped.rename(columns={
        "compute_multiplier": "Overhead", "ladder": "Rungs", "policy": "Policy",
        "success": "Success", "fidelity": "Mean $f$", "low": "Low-$f$ rate",
        "p95": "p95 (s)", "energy": "Wh/success"})
    display["Overhead"] = display["Overhead"].map(lambda v: f"{v:g}$\\times$")
    for column in ("Success", "Mean $f$", "Low-$f$ rate", "p95 (s)", "Wh/success"):
        display[column] = display[column].map(lambda v: f"{v:.3f}")
    return display, grouped


def contention_table() -> pd.DataFrame:
    frame = pd.read_csv(CONTENTION)
    frame["policy"] = frame["policy"].map(POLICY_LABEL).fillna(frame["policy"])
    grouped = frame.groupby(["compute_multiplier", "policy"]).agg(
        success=("success_ratio", "mean"), p95=("task_delay_p95_s", "mean"),
        cpu=("Average CPU usage (Edge) (%)", "mean"),
        tasks=("Number of generated tasks", "mean")).reset_index()
    display = grouped.rename(columns={
        "compute_multiplier": "Overhead", "policy": "Policy", "success": "Success",
        "p95": "p95 (s)", "cpu": "Edge CPU (%)", "tasks": "Tasks"})
    display["Overhead"] = display["Overhead"].map(lambda v: f"{v:g}$\\times$")
    display["Tasks"] = display["Tasks"].map(lambda v: f"{v:.0f}")
    for column in ("Success", "p95 (s)", "Edge CPU (%)"):
        display[column] = display[column].map(lambda v: f"{v:.3f}")
    return display, grouped


def contention_figure(grouped: pd.DataFrame) -> None:
    plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 9, "figure.dpi": 160})
    fig, ax = plt.subplots(figsize=(5.2, 3.2))
    for policy, block in grouped.groupby("policy"):
        ax.plot(block["compute_multiplier"], block["success"], marker="o", label=policy)
    ax.set_xlabel("overhead multiplier")
    ax.set_ylabel("success ratio (contended fleet)")
    ax.set_title("Agentic requests sharing the edge with a background service")
    ax.grid(alpha=0.15)
    ax.legend(frameon=False, fontsize=7)
    fig.tight_layout()
    for suffix in ("pdf", "png"):
        fig.savefig(RESULTS / "figures" / f"f_contention.{suffix}")
    plt.close(fig)


def ladder_figure(grouped: pd.DataFrame) -> None:
    plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 9, "figure.dpi": 160})
    fig, axes = plt.subplots(1, 2, figsize=(10, 3.4), sharey=True)
    for ax, (policy, block) in zip(axes, grouped.groupby("policy")):
        for overhead, arm in block.groupby("compute_multiplier"):
            ax.plot([ORDER[r] for r in arm["ladder"]], arm["success"], marker="o",
                    label=f"{overhead:g}x")
        ax.set_title(policy, fontsize=9)
        ax.set_xlabel("rung count (2, 3, 4, 6)")
        ax.set_xticks([1, 2, 3, 4])
        ax.grid(alpha=0.15)
    axes[0].set_ylabel("success ratio")
    axes[0].legend(frameon=False, fontsize=7, title="overhead")
    fig.suptitle("Fidelity-ladder granularity")
    fig.tight_layout()
    for suffix in ("pdf", "png"):
        fig.savefig(RESULTS / "figures" / f"f_ladder.{suffix}")
    plt.close(fig)


def main() -> int:
    ladder, ladder_grouped = ladder_table()
    tables.write(ladder, RESULTS / "tables" / "fidelity_ladder.tex",
                 "Fidelity-ladder granularity: success, realised mean fidelity, "
                 "low-fidelity rate, p95 delay and energy by rung count, policy "
                 "and overhead.",
                 "tab:p2-ladder", wide=True, size=r"\footnotesize",
                 notes="Every ladder keeps the full-effort rung and the 0.1 floor. "
                       "``two'' is a single fallback rung above the floor.")
    contention, contention_grouped = contention_table()
    tables.write(contention, RESULTS / "tables" / "contention.tex",
                 "Resource contention: a non-agentic background service shares the "
                 "fleet with the agentic classes. Success, p95 delay, edge CPU "
                 "utilisation and the generated task count.",
                 "tab:p2-contention", wide=True, size=r"\footnotesize",
                 notes="The background class is not scaled by the agentic overhead "
                       "model; it only occupies edge capacity.")
    ladder_figure(ladder_grouped)
    contention_figure(contention_grouped)
    print(ladder.to_string(index=False))
    print(contention.to_string(index=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
