"""Feasibility scenario for: Feasibility-Certified and Fault-Tolerant Edge Orchestration
for Mission-Critical Services under Mobility and Non-Stationary Workloads.

Runs a mission-critical workload with tight deadlines under mobile edge devices and
compares orchestration architectures. Reports success/failure and delay metrics as
proxies for feasibility certification and fault impact in PureEdgeSim (Python).
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "_deps_pureedgesim_python" / "src"))

from pureedgesim import COLUMNS, Simulation  # noqa: E402


def main():
    settings = Path(__file__).resolve().parent / "settings"
    out = Path(__file__).resolve().parent / "output"
    sim = Simulation(str(settings), str(out))
    sim.load()
    p = sim.parameters
    p.simulation_duration = 6 * 60  # seconds
    p.min_number_of_devices = p.max_number_of_devices = 50
    p.devices_increment = 1
    p.orchestration_architectures = ["EDGE_ONLY", "EDGE_AND_CLOUD", "ALL"]
    p.orchestration_algorithms = ["TRADE_OFF", "ROUND_ROBIN"]
    p.random_seed = 7
    p.save_log_file = True
    p.save_charts = False
    p.display_real_time_charts = False
    p.auto_close_real_time_charts = True
    rows = sim.launch()
    cols = {
        "architecture": "Orchestration architecture",
        "algorithm": "Orchestration algorithm",
        "generated": "Number of generated tasks",
        "success": "Tasks successfully executed",
        "not_executed": "Task not executed (No resources available or long waiting time)",
        "failed_delay": "Tasks failed (delay)",
        "failed_mob": "Tasks failed (mobility)",
        "failed_dead": "Tasks failed (device dead)",
        "failed_dep": "Tasks not executed because a dependency failed",
        "avg_delay": "Average execution delay (s)",
        "energy_wh": "Energy consumption of computing nodes (Wh)",
    }
    summary = []
    for row in rows:
        item = {k: row[v] for k, v in cols.items()}
        summary.append(item)
        print(f"{item['architecture']:<15} {item['algorithm']:<12} ok={item['success']}/{item['generated']} "
              f"not_exec={item['not_executed']} delay_fail={item['failed_delay']} mob_fail={item['failed_mob']} "
              f"dead_fail={item['failed_dead']} dep_fail={item['failed_dep']} avg={item['avg_delay']}")
    (out / "feasibility_summary.json").write_text(
        json.dumps({"scenario": "feasibility_certified_fault_tolerant_edge", "rows": summary}, indent=2),
        encoding="utf-8",
    )
    print(f"\nSummary written to {out / 'feasibility_summary.json'}")


if __name__ == "__main__":
    main()
