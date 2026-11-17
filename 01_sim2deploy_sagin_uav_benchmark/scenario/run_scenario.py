"""Feasibility scenario for: From Simulation to Deployment - A Reproducible Benchmark
and Testbed for UAV- and SAGIN-Integrated Edge Computing.

Models a simplified SAGIN/UAV edge setting in PureEdgeSim (Python):
- mobile UAV/IoT edge devices generate sensing/mission tasks (mobility enabled),
- ground edge data centers act as MEC aggregation points,
- a remote cloud tier proxies satellite/backhaul compute.

The script runs a small, reproducible sweep over orchestration architectures and
algorithms and reports task success, latency, network/energy use to assess
simulation-to-deployment feasibility of the benchmark design.
"""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "_deps_pureedgesim_python" / "src"))

from pureedgesim import COLUMNS, Simulation  # noqa: E402


def main() -> dict:
    settings = Path(__file__).resolve().parent / "settings"
    out = Path(__file__).resolve().parent / "output"
    sim = Simulation(str(settings), str(out))
    sim.load()
    p = sim.parameters
    p.simulation_duration = 5 * 60  # seconds (5 minutes), keep runtime short for feasibility
    p.min_number_of_devices = p.max_number_of_devices = 40
    p.devices_increment = 1
    p.orchestration_architectures = ["EDGE_ONLY", "EDGE_AND_CLOUD", "ALL"]
    p.orchestration_algorithms = ["TRADE_OFF", "ROUND_ROBIN"]
    p.random_seed = 42
    p.save_log_file = True
    p.save_charts = False
    p.display_real_time_charts = False
    p.auto_close_real_time_charts = True
    rows = sim.launch()
    summary = []
    cols = {
        "architecture": "Orchestration architecture",
        "algorithm": "Orchestration algorithm",
        "devices": "Edge devices count",
        "generated": "Number of generated tasks",
        "success": "Tasks successfully executed",
        "not_executed": "Task not executed (No resources available or long waiting time)",
        "failed_delay": "Tasks failed (delay)",
        "failed_mobility": "Tasks failed (mobility)",
        "failed_dead_device": "Tasks failed (device dead)",
        "avg_delay": "Average execution delay (s)",
        "cloud_tasks": "Tasks successfully executed (Cloud)",
        "edge_tasks": "Tasks successfully executed (Edge)",
        "mist_tasks": "Tasks successfully executed (Mist)",
        "energy_wh": "Energy consumption of computing nodes (Wh)",
        "wan_s": "Wan usage (s)",
    }
    for row in rows:
        item = {k: row[v] for k, v in cols.items()}
        summary.append(item)
        print(f"{item['architecture']:<15} {item['algorithm']:<12} gen={item['generated']} "
              f"ok={item['success']} not_exec={item['not_executed']} delay_fail={item['failed_delay']} "
              f"mob={item['failed_mobility']} dead={item['failed_dead_device']} "
              f"avg_delay={item['avg_delay']}s cloud={item['cloud_tasks']} edge={item['edge_tasks']} "
              f"mist={item['mist_tasks']} energy={item['energy_wh']}Wh wan={item['wan_s']}s")
    report = {"scenario": "sim2deploy_sagin_uav_benchmark", "rows": summary}
    (out / "feasibility_summary.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(f"\nSummary written to {out / 'feasibility_summary.json'}")
    return report


if __name__ == "__main__":
    main()
