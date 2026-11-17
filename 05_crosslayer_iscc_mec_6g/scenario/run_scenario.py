"""Feasibility scenario for: Cross-Layer ISCC-MEC Co-Design for 6G Edge Networks.

Models an ISCC-like pipeline as a service graph (sensing -> communication/encoding ->
edge compute/inference) using PureEdgeSim (Python) subtasks/dependencies, and
evaluates execution under different architectures.
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
    p.simulation_duration = 5 * 60  # seconds
    p.min_number_of_devices = p.max_number_of_devices = 30
    p.devices_increment = 1
    p.orchestration_architectures = ["MIST_AND_EDGE", "EDGE_AND_CLOUD", "ALL"]
    p.orchestration_algorithms = ["TRADE_OFF"]
    p.random_seed = 42
    p.save_log_file = True
    p.save_charts = False
    p.display_real_time_charts = False
    p.auto_close_real_time_charts = True
    rows = sim.launch()
    cols = {
        "architecture": "Orchestration architecture",
        "generated": "Number of generated tasks",
        "success": "Tasks successfully executed",
        "not_executed": "Task not executed (No resources available or long waiting time)",
        "failed_dep": "Tasks not executed because a dependency failed",
        "failed_delay": "Tasks failed (delay)",
        "avg_delay": "Average execution delay (s)",
        "gen_req": "Requests generated",
        "succ_req": "Successful requests",
        "avg_req_delay": "Average request makespan (s)",
        "energy_wh": "Energy consumption of computing nodes (Wh)",
    }
    summary = []
    for row in rows:
        item = {k: row[v] for k, v in cols.items()}
        summary.append(item)
        print(f"{item['architecture']:<15} tasks ok={item['success']}/{item['generated']} "
              f"not_exec={item['not_executed']} delay_fail={item['failed_delay']} dep_fail={item['failed_dep']} "
              f"req ok={item['succ_req']}/{item['gen_req']} e2e={item['avg_req_delay']}")
    (out / "feasibility_summary.json").write_text(
        json.dumps({"scenario": "crosslayer_iscc_mec_6g", "rows": summary}, indent=2),
        encoding="utf-8",
    )
    print(f"\nSummary written to {out / 'feasibility_summary.json'}")


if __name__ == "__main__":
    main()
