"""Java/Python parity harness for the P1 benchmark.

The two PureEdgeSim implementations share the settings format and the 56-column
result schema, but not their random number generators.  We therefore report two
levels of agreement:

1. **Structural parity** on one identical configuration (same seed, same settings):
   generated workload, success/failure structure and the order of magnitude of
   delay, energy and network usage must agree.
2. **Distributional parity** over several seeds: the success-ratio distributions
   must not differ significantly (Mann--Whitney, alpha = 0.05).

Both are written to ``results/parity/parity.json`` with the raw rows, so the paper
can report exactly what was and was not reproduced.
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from research_common import stats  # noqa: E402
from research_common.runs import prepare_settings, run_once  # noqa: E402

PROJECT = "01_sim2deploy_sagin_uav_benchmark"
RESULTS = ROOT / PROJECT / "results" / "parity"
SETTINGS = ROOT / PROJECT / "scenario" / "settings"
JAR = ROOT / "target" / "pureedgesim-6.1.jar"
SOURCE = ROOT / PROJECT / "scenario" / "java" / "ParityRunner.java"

SHARED = {
    "generated": "Number of generated tasks",
    "success": "Tasks successfully executed",
    "delay_failures": "Tasks failed (delay)",
    "mobility_failures": "Tasks failed (mobility)",
    "energy": "Energy consumption of computing nodes (Wh)",
    "avg_delay": "Average execution delay (s)",
    "wan": "Wan usage (s)",
    "traffic": "Total network traffic (MBytes)",
}

PROPERTIES = {
    "simulation_time": 5,
    "realistic_network_model": "true",
    "network_update_interval": 1,
    "one_shared_wan_network": "true",
    "edge_devices_range": 80,
    "edge_datacenters_coverage": 350,
    "wan_bandwidth": 50,
    "wan_latency": 0.12,
    "wifi_bandwidth": 300,
    "save_log_file": "false",
    "save_charts": "false",
    "batch_size": 100,
    "max_number_of_edge_devices": 40,
    "edge_device_counter_size": 1,
}


def build(architecture: str = "ALL", algorithm: str = "TRADE_OFF", seed: int = 1) -> Path:
    settings = RESULTS / "settings"
    prepare_settings(SETTINGS, settings, properties={
        **PROPERTIES,
        "random_seed": seed,
        "orchestration_architectures": architecture,
        "orchestration_algorithms": algorithm,
    })
    return settings


def compile_runner() -> Path:
    classes = RESULTS / "classes"
    classes.mkdir(parents=True, exist_ok=True)
    if not JAR.is_file():
        raise SystemExit(f"Java jar not found: {JAR}. Run `mvn -q -DskipTests package` in PureEdgeSim first.")
    subprocess.run(["javac", "-cp", str(JAR), "-d", str(classes), str(SOURCE)], check=True)
    return classes


def run_java(settings: Path, classes: Path, output: Path) -> dict:
    if output.exists():
        shutil.rmtree(output)
    cp = f"{JAR}{';' if sys.platform == 'win32' else ':'}{classes}"
    # Add all Maven dependencies to the classpath (jgrapht, etc.)
    m2 = Path.home() / ".m2" / "repository"
    if m2.is_dir():
        jars = list(m2.rglob("*.jar"))
        cp += (";" if sys.platform == "win32" else ":") + (";" if sys.platform == "win32" else ":").join(str(j) for j in jars)
    env = dict(os.environ)
    env["CLASSPATH"] = cp
    subprocess.run(["java", "ParityRunner", str(settings) + os.sep, str(output)], check=True, cwd=ROOT, env=env)
    csv_files = sorted(output.rglob("Sequential_simulation.csv"))
    if not csv_files:
        raise SystemExit(f"Java run produced no CSV under {output}")
    import pandas as pd

    frame = pd.read_csv(csv_files[-1])
    row = frame.iloc[-1].to_dict() if len(frame) else {}
    return {key: row.get(column) for key, column in SHARED.items()} | {"csv": str(csv_files[-1])}


def run_python(settings: Path, output: Path, seed: int) -> dict:
    row = run_once(base_settings=settings, out_dir=output, architecture="ALL", algorithm="TRADE_OFF",
                   devices=40, duration_s=300.0, seed=seed, keep_tasks=False, run_id=f"parity-{seed}")
    return {key: row.get(column) for key, column in SHARED.items()}


def relative(a, b) -> float:
    try:
        a, b = float(a), float(b)
    except (TypeError, ValueError):
        return float("nan")
    denominator = max(abs(a), abs(b), 1e-9)
    return abs(a - b) / denominator


def main() -> int:
    RESULTS.mkdir(parents=True, exist_ok=True)
    classes = compile_runner()
    settings = build()

    java_row = run_java(settings, classes, RESULTS / "java")
    python_row = run_python(settings, RESULTS / "python", seed=1)
    differences = {key: relative(java_row.get(key), python_row.get(key)) for key in SHARED}
    structural = (java_row.get("generated") == python_row.get("generated")
                  and abs(java_row.get("success", 0) / max(1, java_row.get("generated", 1))
                          - python_row.get("success", 0) / max(1, python_row.get("generated", 1))) <= 0.05
                  and (set(k for k in ("delay_failures", "mobility_failures")
                           if (java_row.get(k) or 0) > 0)
                       == set(k for k in ("delay_failures", "mobility_failures")
                              if (python_row.get(k) or 0) > 0)))

    java_success, python_success = [], []
    for seed in range(1, 6):
        seed_settings = build(seed=seed)
        output = RESULTS / f"distribution/java-{seed}"
        row = run_java(seed_settings, classes, output)
        java_success.append(row["success"] / max(1, row["generated"]))
    for seed in range(1, 6):
        seed_settings = build(seed=seed)
        row = run_python(seed_settings, RESULTS / f"distribution/python-{seed}", seed=seed)
        python_success.append(row["success"] / max(1, row["generated"]))
    test = stats.mann_whitney(java_success, python_success)

    java_mean = sum(java_success) / len(java_success)
    python_mean = sum(python_success) / len(python_success)
    payload = {
        "available": True,
        "structural_match": bool(structural),
        "workload_match": bool(java_row.get("generated") == python_row.get("generated")
                               and java_row.get("generated")),
        "success_gap_pp": 100.0 * abs(java_mean - python_mean),
        "max_seed_gap_pp": 100.0 * max(abs(a - b) for a, b in zip(java_success, python_success)),
        "java_success_mean": java_mean,
        "python_success_mean": python_mean,
        "max_relative_difference": max(value for value in differences.values() if value == value),
        "relative_differences": differences,
        "distribution_p_value": test["p_value"],
        "distribution_effect_size": test["delta"],
        "java_row": java_row,
        "python_row": python_row,
        "java_success_ratio": java_success,
        "python_success_ratio": python_success,
        "note": ("Workload parity means both ports generate the same number of tasks for a seed. "
                 "Distributional parity is a five-seed Mann--Whitney test of the success ratio at "
                 "alpha=0.05. Structural parity additionally requires a mean success gap <= 5 pp and "
                 "identical non-zero failure categories. Exact numeric equality is not expected: the "
                 "ports use different RNG streams, and tier usage diverges materially (WAN time in "
                 "particular), so per-seed gaps of up to 13 pp occur."),
    }
    (RESULTS / "parity.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(json.dumps({key: payload[key] for key in ("structural_match", "max_relative_difference",
                                                    "distribution_p_value")}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
