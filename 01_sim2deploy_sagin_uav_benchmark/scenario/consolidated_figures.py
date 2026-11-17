"""Consolidated multi-panel figures for the P1 manuscript.

Every panel is computed from the generated result CSVs; no values are
transcribed by hand. Output: results/figures/consolidated/f4..f10.pdf+png.
"""
from __future__ import annotations

import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

HERE = Path(__file__).resolve().parent
P1 = HERE.parent
ROOT = P1.parent
RESULTS = P1 / "results"
TABLES = RESULTS / "tables"
OUT = RESULTS / "figures" / "consolidated"
OUT.mkdir(parents=True, exist_ok=True)
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

COLORS = {"ALL": "#2b6cb0", "EDGE_AND_CLOUD": "#c05621", "EDGE_ONLY": "#2f855a"}
LABELS = {"ALL": "all resources", "EDGE_AND_CLOUD": "edge+cloud", "EDGE_ONLY": "edge only"}

plt.rcParams.update({
    "font.family": "DejaVu Sans", "font.size": 8,
    "axes.titlesize": 9, "axes.labelsize": 8,
    "legend.fontsize": 7, "xtick.labelsize": 7, "ytick.labelsize": 7,
    "figure.dpi": 160, "savefig.bbox": "tight",
})


def save(fig: plt.Figure, name: str) -> None:
    fig.savefig(OUT / f"{name}.pdf")
    fig.savefig(OUT / f"{name}.png")
    plt.close(fig)
    print(f"[fig] {name}")


def grid(name: str) -> pd.DataFrame:
    return pd.read_csv(RESULTS / "runs" / name / "raw_runs.csv")


# ---------------------------------------------------------------- F4
def f4() -> None:
    scale = pd.read_csv(TABLES / "scale_aggregate.csv")
    classes = pd.read_csv(TABLES / "class_breakdown.csv")
    fig, axes = plt.subplots(1, 3, figsize=(11, 3.2))
    for metric, ax, title in (("success_ratio", axes[0], "Success ratio"),
                              ("task_delay_p95_s", axes[1], "p95 delay (s)")):
        subset = scale[scale["metric"] == metric]
        for arch, block in subset.groupby("architecture"):
            g = block.groupby("devices")["mean"].agg(["mean", "std", "count"]).reset_index()
            ax.errorbar(g["devices"], g["mean"], yerr=1.96 * g["std"] / np.sqrt(g["count"]),
                        marker="o", color=COLORS.get(arch, "#333"),
                        label=LABELS.get(arch, arch), capsize=2)
        ax.set_xlabel("devices")
        ax.set_title(title)
        ax.grid(alpha=0.15)
    axes[0].legend(frameon=False)
    # class-level success
    ax = axes[2]
    x = np.arange(len(classes))
    for index, arch in enumerate(["ALL", "EDGE_AND_CLOUD", "EDGE_ONLY"]):
        ax.bar(x + (index - 1) * 0.26, classes[arch], 0.26,
               color=COLORS[arch], label=LABELS[arch])
    ax.set_xticks(x)
    ax.set_xticklabels(classes["application"], rotation=15, ha="right")
    ax.set_ylabel("success ratio")
    ax.set_title("Success by class")
    ax.legend(frameon=False)
    ax.grid(alpha=0.15, axis="y")
    fig.tight_layout()
    save(fig, "f4")


# ---------------------------------------------------------------- F5
def f5() -> None:
    clock = grid("clock")
    deadline = grid("deadline")
    mix = grid("mix")
    fig, axes = plt.subplots(1, 3, figsize=(11, 3.2))
    for arch in ("ALL", "EDGE_AND_CLOUD", "EDGE_ONLY"):
        block = clock[clock["architecture"] == arch].groupby("update_interval")[
            "success_ratio"].mean()
        axes[0].plot(block.index, block.values, marker="o", color=COLORS[arch],
                     label=LABELS[arch])
    axes[0].set_xscale("log")
    axes[0].set_xticks([0.05, 0.2, 1.0])
    axes[0].set_xticklabels(["50 ms", "200 ms", "1 s"])
    axes[0].set_xlabel("control interval")
    axes[0].set_ylabel("success ratio")
    axes[0].set_title("(a) temporal resolution")
    axes[0].legend(frameon=False)
    axes[0].grid(alpha=0.15)

    for arch in ("ALL", "EDGE_AND_CLOUD", "EDGE_ONLY"):
        block = deadline[deadline["architecture"] == arch].groupby("deadline_factor")[
            "success_ratio"].mean()
        axes[1].plot(block.index, block.values, marker="o", color=COLORS[arch])
    axes[1].set_xlabel("deadline multiplier")
    axes[1].set_title("(b) deadline scaling")
    axes[1].grid(alpha=0.15)

    for arch in ("ALL", "EDGE_AND_CLOUD", "EDGE_ONLY"):
        block = mix[mix["architecture"] == arch].groupby("mix")["success_ratio"].mean()
        axes[2].plot(block.index, block.values, marker="o", color=COLORS[arch])
    axes[2].set_xlabel("workload mix")
    axes[2].set_title("(c) workload composition")
    axes[2].grid(alpha=0.15)
    fig.tight_layout()
    save(fig, "f5")


# ---------------------------------------------------------------- F6
def f6() -> None:
    dec = pd.read_csv(TABLES / "latency_decomposition.csv")
    curve = pd.read_csv(TABLES / "temporal_fidelity.csv")
    fig, axes = plt.subplots(1, 2, figsize=(9, 3.4))
    # component plot at 1 s ALL for three algorithms
    subset = dec[(dec["Architecture"] == "ALL") & (dec["Interval (s)"] == 1.0)
                 & (dec["Algorithm"].isin(["mct", "leastloaded", "minmin"]))]
    agg = subset.groupby("Algorithm")[
        ["Release to decision (s)", "Decision to dispatch (s)",
         "Dispatch to exec (s)", "Decision latency (s)"]].mean()
    bottom = np.zeros(len(agg))
    for column, color in zip(["Decision latency (s)", "Dispatch to exec (s)"],
                             ["#cbd5e0", "#2b6cb0"]):
        axes[0].bar(agg.index, agg[column], bottom=bottom, color=color, label=column)
        bottom += agg[column].values
    axes[0].set_ylabel("seconds")
    axes[0].set_title("(a) latency decomposition at 1 s")
    axes[0].legend(frameon=False, fontsize=6)
    axes[0].grid(alpha=0.15, axis="y")

    for cls, block in curve.groupby("Class"):
        axes[1].plot(range(len(block)), block["mean_success"], marker="o", label=cls)
        axes[1].fill_between(range(len(block)), block["min_success"],
                             block["max_success"], alpha=0.15)
    axes[1].set_ylim(-0.02, 1.02)
    axes[1].set_xlabel("latency/deadline bin (ordered)")
    axes[1].set_ylabel("success ratio")
    axes[1].set_title("(b) measured calibration")
    axes[1].legend(frameon=False, fontsize=6)
    axes[1].grid(alpha=0.15)
    fig.tight_layout()
    save(fig, "f6")


# ---------------------------------------------------------------- F7
def f7() -> None:
    ranking = pd.read_csv(TABLES / "algorithm_ranking.csv")
    hetero = pd.read_csv(TABLES / "heterogeneity.csv")
    affinity = pd.read_csv(TABLES / "hetero_affinity.csv")
    fig, axes = plt.subplots(1, 3, figsize=(12, 3.4))
    pivot = ranking[ranking["Architecture"] == "ALL"].pivot(
        index="Algorithm", columns="Interval (s)", values="Success")
    im = axes[0].imshow(pivot.values, cmap="RdYlGn", vmin=0, vmax=1, aspect="auto")
    axes[0].set_xticks(range(len(pivot.columns)))
    axes[0].set_xticklabels([f"{v:g} s" for v in pivot.columns])
    axes[0].set_yticks(range(len(pivot.index)))
    axes[0].set_yticklabels(pivot.index)
    axes[0].set_title("(a) algorithm success heatmap")
    fig.colorbar(im, ax=axes[0], fraction=0.046)

    wide = hetero.pivot(index="Profile", columns="Interval (s)", values="Effect (pp)")
    wide.plot(kind="bar", ax=axes[1], color=["#c05621", "#2b6cb0"])
    axes[1].axhline(0, color="black", linewidth=0.6)
    axes[1].set_ylabel("effect (pp)")
    axes[1].set_title("(b) infrastructure interaction")
    axes[1].legend(frameon=False, fontsize=6)

    af = affinity.copy()
    for column, color in (("Mist share", "#2f855a"), ("Edge share", "#2b6cb0"),
                          ("Cloud share", "#c05621")):
        axes[2].bar(range(len(af)), af[column], bottom=0, color=color, label=column,
                    width=0.6, alpha=0.85 if column == "Edge share" else 0.6)
    axes[2].set_xticks(range(len(af)))
    axes[2].set_xticklabels([f"{p[:10]}-{i:g}" for p, i in
                             zip(af["Profile"], af["Interval (s)"])], rotation=45, ha="right")
    axes[2].set_title("(c) placement distribution")
    axes[2].legend(frameon=False, fontsize=6)
    fig.tight_layout()
    save(fig, "f7")


# ---------------------------------------------------------------- F8
def f8() -> None:
    failure = pd.read_csv(TABLES / "failure_causes.csv")
    energy = pd.read_csv(TABLES / "energy_accounting.csv")
    sens = grid("sensitivity")
    fig, axes = plt.subplots(1, 3, figsize=(12, 3.3))
    causes = ["Deadline", "Mobility", "Device death", "Resources", "Dependency",
              "No destination", "Unresolved"]
    bottom = np.zeros(len(failure))
    for cause in causes:
        axes[0].bar(failure["Architecture"], failure[cause], bottom=bottom,
                    color=f"C{causes.index(cause)}", label=cause, width=0.6)
        bottom += failure[cause].values
    axes[0].set_ylabel("share of generated tasks")
    axes[0].set_title("(a) failure composition")
    axes[0].legend(frameon=False, fontsize=5, ncol=2)

    axes[1].bar(energy["Architecture"], energy["Edge"], color="#2b6cb0", label="edge")
    axes[1].bar(energy["Architecture"], energy["Mist"], bottom=energy["Edge"],
                color="#2f855a", label="mist")
    axes[1].set_ylabel("energy (Wh)")
    axes[1].set_title("(b) energy by resource")
    axes[1].legend(frameon=False, fontsize=6)

    for wan, block in sens.groupby("wan_latency"):
        axes[2].plot(block["wifi_bandwidth"], block["task_delay_p95_s"], marker="o",
                     label=f"WAN {wan:g} s")
    axes[2].set_xlabel("Wi-Fi bandwidth (Mbps)")
    axes[2].set_ylabel("p95 delay (s)")
    axes[2].set_title("(c) network sensitivity")
    axes[2].legend(frameon=False, fontsize=6)
    fig.tight_layout()
    save(fig, "f8")


# ---------------------------------------------------------------- F9
def f9() -> None:
    trace = pd.read_csv(TABLES / "trace_aggregate.csv")
    mobility = pd.read_csv(TABLES / "mobility_aggregate.csv")
    classes = pd.read_csv(TABLES / "trace_classes.csv")
    fig, axes = plt.subplots(1, 3, figsize=(12, 3.3))
    subset = trace[trace["metric"] == "success_ratio"]
    for arch, block in subset.groupby("architecture"):
        axes[0].plot(block["workload"], block["mean"], marker="o", color=COLORS[arch],
                     label=LABELS[arch])
    axes[0].tick_params(axis="x", rotation=20)
    axes[0].set_ylabel("success ratio")
    axes[0].set_title("(a) workload realism, four arms")
    axes[0].legend(frameon=False, fontsize=6)
    axes[0].grid(alpha=0.15)

    mob = mobility[mobility["metric"] == "success_ratio"]
    for arch, block in mob.groupby("architecture"):
        axes[1].plot(block["mobility"], block["mean"], marker="o", color=COLORS[arch],
                     label=LABELS[arch])
    axes[1].tick_params(axis="x", rotation=20)
    axes[1].set_title("(b) mobility sensitivity")
    axes[1].grid(alpha=0.15)

    x = np.arange(len(classes))
    for index, arch in enumerate(["ALL", "EDGE_AND_CLOUD", "EDGE_ONLY"]):
        axes[2].bar(x + (index - 1) * 0.26, classes[arch], 0.26,
                    color=COLORS[arch], label=LABELS[arch])
    axes[2].set_xticks(x)
    axes[2].set_xticklabels(classes["application"], rotation=15, ha="right")
    axes[2].set_title("(c) trace classes")
    fig.tight_layout()
    save(fig, "f9")


# ---------------------------------------------------------------- F10
def f10() -> None:
    common = pd.read_csv(ROOT / "crosssim" / "results" / "common_summary.csv")
    fig, ax = plt.subplots(figsize=(7.5, 2.8))
    pivot = common.pivot_table(index=["platform", "architecture"], columns="tick",
                               values="success")
    columns = [c for c in ["1 s", "200 ms", "50 ms", "event-driven"] if c in pivot]
    image = ax.imshow(pivot[columns].values, cmap="RdYlGn", vmin=0, vmax=1, aspect="auto")
    ax.set_xticks(range(len(columns)))
    ax.set_xticklabels(columns)
    ax.set_yticks(range(len(pivot.index)))
    ax.set_yticklabels([f"{p} / {a}" for p, a in pivot.index], fontsize=6)
    for row in range(pivot.shape[0]):
        for col in range(len(columns)):
            value = pivot[columns].iloc[row, col]
            if pd.notna(value):
                ax.text(col, row, f"{value:.3f}", ha="center", va="center", fontsize=6)
    ax.set_title("Common-scenario success by platform, configuration and temporal setting")
    fig.colorbar(image, ax=ax, fraction=0.03)
    ax.set_xlabel("blank = adapter not executed; no value is zero")
    fig.tight_layout()
    save(fig, "f10")


def main() -> None:
    for function in (f4, f5, f6, f7, f8, f9, f10):
        try:
            function()
        except Exception as error:
            print(f"[fig] {function.__name__} FAILED: {type(error).__name__}: {error}")


if __name__ == "__main__":
    main()
