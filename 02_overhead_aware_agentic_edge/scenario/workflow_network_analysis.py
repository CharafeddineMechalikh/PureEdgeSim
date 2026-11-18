"""Analyse the workflow and network-contention grids (reviewer requests)."""
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

# ---- workflow arm ------------------------------------------------------------
workflow = pd.read_csv(RESULTS / "runs" / "workflow" / "raw_runs.csv")
group = ["compute_multiplier", "policy"]
metrics = ["success_ratio", "request_success_ratio", "fail_dependency",
           "stage_retrieve_success_rate", "stage_reason_success_rate",
           "stage_verify_success_rate", "stage_sense_success_rate",
           "stage_decide_success_rate", "stage_act_success_rate"]
summary = workflow.groupby(group)[metrics].mean().reset_index()
tables.write(summary, RESULTS / "tables" / "workflow.tex",
             "Dependency-aware workflow arm: end-to-end success, request success, "
             "dependency-failure count and per-service success by policy and overhead.",
             "tab:p2-workflow", wide=True, size=r"\scriptsize",
             notes="Each request is a three-service DAG. Unlike the independent-task "
                   "arm, dependency failures are exercised: cancelled descendants "
                   "are counted in `fail_dependency'.")

plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 9, "figure.dpi": 160})
fig, axes = plt.subplots(1, 2, figsize=(9.5, 3.4))
for policy, block in workflow.groupby("policy"):
    grouped = block.groupby("compute_multiplier")[["success_ratio", "request_success_ratio"]].mean()
    axes[0].plot(grouped.index, grouped["success_ratio"], marker="o", label=policy)
    axes[1].plot(grouped.index, grouped["request_success_ratio"], marker="o", label=policy)
axes[0].set_ylabel("task-level success")
axes[1].set_ylabel("request-level (DAG) success")
for ax in axes:
    ax.set_xlabel("compute multiplier")
    ax.grid(alpha=0.15)
axes[0].legend(frameon=False, fontsize=7)
fig.tight_layout()
for suffix in ("pdf", "png"):
    fig.savefig(RESULTS / "figures" / f"f_workflow.{suffix}")
plt.close(fig)

# ---- network-contention arm --------------------------------------------------
network = pd.read_csv(RESULTS / "runs" / "network" / "raw_runs.csv")
net_summary = network.groupby(["wan_bandwidth", "compute_multiplier", "policy"])[
    ["success_ratio", "task_delay_p95_s", "wan_usage_s", "failure_ratio"]].mean().reset_index()
tables.write(net_summary, RESULTS / "tables" / "network.tex",
             "Network-contention arm: realistic bandwidth-sharing model enabled, "
             "WAN bandwidth varied at two overhead levels.",
             "tab:p2-network", wide=True, size=r"\footnotesize",
             notes="Unlike the main grids, the simulator's realistic network model "
                   "is enabled here, so transfers share link bandwidth.")

fig, axes = plt.subplots(1, 2, figsize=(9.5, 3.4), sharex=True)
for policy, block in network.groupby("policy"):
    grouped = block.groupby("wan_bandwidth")[["success_ratio", "task_delay_p95_s"]].mean()
    axes[0].plot(grouped.index, grouped["success_ratio"], marker="o", label=policy)
    axes[1].plot(grouped.index, grouped["task_delay_p95_s"], marker="o", label=policy)
axes[0].set_ylabel("success ratio")
axes[1].set_ylabel("p95 delay (s)")
for ax in axes:
    ax.set_xlabel("WAN bandwidth (Mb/s)")
    ax.set_xscale("log")
    ax.grid(alpha=0.15)
axes[0].legend(frameon=False, fontsize=7)
fig.tight_layout()
for suffix in ("pdf", "png"):
    fig.savefig(RESULTS / "figures" / f"f_network.{suffix}")
plt.close(fig)

print("WORKFLOW\n", summary.round(3).to_string(index=False))
print("\nNETWORK\n", net_summary.round(3).to_string(index=False))
