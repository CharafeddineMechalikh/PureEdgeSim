"""Feasibility scenario for: Overhead-Aware Agentic Edge Intelligence.

Compares a baseline workload vs an "agentic" workload that includes LLM/RAG-like
reasoning overhead (larger request/container sizes and task lengths) under the
same edge infrastructure. Demonstrates how orchestration/placement outcomes and
resource pressure change when agentic control-plane/data-plane overhead is made
explicit in a PureEdgeSim (Python) feasibility study.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "_deps_pureedgesim_python" / "src"))

from pureedgesim import COLUMNS, Simulation  # noqa: E402
from pureedgesim.config import load_applications  # noqa: E402


def run_case(label: str, apps_file: str) -> dict:
    settings = Path(__file__).resolve().parent / "settings"
    out = Path(__file__).resolve().parent / "output" / label
    out.mkdir(parents=True, exist_ok=True)
    sim = Simulation(str(settings), str(out))
    sim.load()
    # swap applications by loading the alternate applications file
    sim.files.applications = load_applications(settings / apps_file)
    p = sim.parameters
    p.simulation_duration = 5 * 60  # seconds
    p.min_number_of_devices = p.max_number_of_devices = 30
    p.devices_increment = 1
    p.orchestration_architectures = ["EDGE_AND_CLOUD", "ALL"]
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
        "failed_delay": "Tasks failed (delay)",
        "avg_delay": "Average execution delay (s)",
        "cloud_tasks": "Tasks successfully executed (Cloud)",
        "edge_tasks": "Tasks successfully executed (Edge)",
        "mist_tasks": "Tasks successfully executed (Mist)",
        "energy_wh": "Energy consumption of computing nodes (Wh)",
        "network_s": "Network usage (s)",
        "wan_s": "Wan usage (s)",
    }
    summary = []
    for row in rows:
        item = {"case": label}
        for k, v in cols.items():
            item[k] = row[v]
        summary.append(item)
        print(f"[{label}] {item['architecture']:<15} gen={item['generated']} ok={item['success']} "
              f"not_exec={item['not_executed']} delay_fail={item['failed_delay']} avg_delay={item['avg_delay']} "
              f"net={item['network_s']} wan={item['wan_s']} energy={item['energy_wh']}")
    return summary


def main():
    all_rows = []
    all_rows += run_case("baseline", "applications_baseline.xml")
    all_rows += run_case("agentic_overhead", "applications_agentic.xml")
    out = Path(__file__).resolve().parent / "output"
    (out / "feasibility_summary.json").write_text(
        json.dumps({"scenario": "overhead_aware_agentic_edge", "rows": all_rows}, indent=2),
        encoding="utf-8",
    )
    print(f"\nSummary written to {out / 'feasibility_summary.json'}")


if __name__ == "__main__":
    main()
