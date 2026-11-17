"""Feasibility scenario for: Secure-by-Design Federated and Agentic MEC.

Compares a non-secure baseline against a "secure" profile where post-quantum/crypto
and secure-coordination overhead are modeled as additional compute and payload sizes.
Implemented with latest PureEdgeSim (Python).
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "_deps_pureedgesim_python" / "src"))

from pureedgesim import COLUMNS, Simulation  # noqa: E402
from pureedgesim.config import load_applications  # noqa: E402


def run_case(label: str, apps_file: str):
    settings = Path(__file__).resolve().parent / "settings"
    out = Path(__file__).resolve().parent / "output" / label
    out.mkdir(parents=True, exist_ok=True)
    sim = Simulation(str(settings), str(out))
    sim.load()
    sim.files.applications = load_applications(settings / apps_file)
    p = sim.parameters
    p.simulation_duration = 5 * 60  # seconds
    p.min_number_of_devices = p.max_number_of_devices = 35
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
        "network_s": "Network usage (s)",
        "wan": "Wan usage (s)",
        "energy_wh": "Energy consumption of computing nodes (Wh)",
    }
    out_rows = []
    for row in rows:
        item = {"case": label}
        for k, v in cols.items():
            item[k] = row[v]
        out_rows.append(item)
        print(f"[{label}] {item['architecture']:<15} ok={item['success']}/{item['generated']} "
              f"not_exec={item['not_executed']} delay_fail={item['failed_delay']} delay={item['avg_delay']} "
              f"net={item['network_s']} wan={item['wan']} energy={item['energy_wh']}")
    return out_rows


def main():
    rows = []
    rows += run_case("baseline", "applications_baseline.xml")
    rows += run_case("secure_overhead", "applications_secure.xml")
    out = Path(__file__).resolve().parent / "output"
    (out / "feasibility_summary.json").write_text(
        json.dumps({"scenario": "secure_federated_agentic_mec", "rows": rows}, indent=2),
        encoding="utf-8",
    )
    print(f"\nSummary written to {out / 'feasibility_summary.json'}")


if __name__ == "__main__":
    main()
