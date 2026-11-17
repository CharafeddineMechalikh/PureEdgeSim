"""Generate the simulator-independent arrival trace for P1-CBS v1.0.

The trace is produced *outside* every simulator and consumed by every adapter.
Matching a numerical seed across simulators is NOT treated as equivalent to
matching inputs; this file is the authority for arrivals, task attributes and
application dependencies.

Usage
-----
    python crosssim/common_trace.py --seed 1 --out crosssim/traces/trace_s1.csv
    python crosssim/common_trace.py --all --out crosssim/traces
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd

HERE = Path(__file__).resolve().parent
GRAPH = json.loads((HERE / "application_graph.json").read_text(encoding="utf-8"))
SCENARIO = json.loads((HERE / "scenario.json").read_text(encoding="utf-8"))

CLASS_OF = {workflow["class"]: workflow for workflow in GRAPH["workflows"]}
DEVICE_TYPES = SCENARIO["fleet"]["device_types"]


def _device_types(n: int, rng: np.random.Generator) -> list[dict]:
    counts = [round(n * item["percentage"] / 100.0) for item in DEVICE_TYPES]
    counts[-1] = n - sum(counts[:-1])
    devices = []
    for item, count in zip(DEVICE_TYPES, counts):
        for _ in range(count):
            devices.append({
                "type": item["name"], "cores": item["cores"], "mips": item["mips"],
                "mobile": item["mobility"], "speed_ms": item["speed_ms"],
            })
    return devices


def build(seed: int, duration_s: float = 300.0, deadline_factor: float = 1.0) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    devices = _device_types(int(SCENARIO["fleet"]["n_devices"]), rng)
    # PureEdgeSim assigns each device to exactly one application class by
    # usage_percentage: 14 / 14 / 12 devices for C1 / C2 / C3.
    shares = [SCENARIO["workload_classes"][i]["device_share"] for i in range(3)]
    n_devices = len(devices)
    class_counts = [round(n_devices * share) for share in shares]
    class_counts[-1] = n_devices - sum(class_counts[:-1])
    assignment = (["C1"] * class_counts[0] + ["C2"] * class_counts[1]
                  + ["C3"] * class_counts[2])
    assigned_class = {index: name for index, name in enumerate(assignment)}
    rows = []
    for workflow in GRAPH["workflows"]:
        class_id = workflow["class"]
        for device_index, device in enumerate(devices):
            if assigned_class[device_index] != class_id:
                continue
            rate = workflow["rate_per_min_per_device"] / 60.0
            expected = rate * duration_s
            count = int(rng.poisson(expected))
            arrivals = np.sort(rng.uniform(0.0, duration_s, size=count))
            for sequence, arrival in enumerate(arrivals):
                workflow_id = f"{class_id}-{device_index}-{sequence:05d}"
                for service_id in workflow["path"]:
                    service = next(s for s in GRAPH["services"] if s["id"] == service_id)
                    rows.append({
                        "seed": seed,
                        "arrival_s": float(arrival),
                        "class": class_id,
                        "workflow_id": workflow_id,
                        "service": service_id,
                        "service_name": service["name"],
                        "mi": float(service["mi"][class_id]),
                        "input_kb": float(service["input_kb"][class_id]),
                        "output_kb": float(service["output_kb"][class_id]),
                        "deadline_s": float(workflow["deadline_s"] * deadline_factor),
                        "workflow_deadline_s": float(workflow["deadline_s"] * deadline_factor),
                        "device_id": device_index,
                        "device_type": device["type"],
                        "predecessors": ";".join(
                            edge["from"] for edge in GRAPH["edges"]
                            if edge["to"] == service_id and class_id in edge["classes"]),
                        "allowed_tiers": ";".join(service["allowed_tiers"]),
                    })
    frame = pd.DataFrame(rows)
    # Assertions: the application graph's service sums must equal the calibrated
    # class lengths, otherwise the dependency-aware arm would not be comparable.
    for workflow in GRAPH["workflows"]:
        class_id = workflow["class"]
        total = frame[frame["class"] == class_id].groupby("workflow_id")["mi"].sum()
        if len(total) and not np.allclose(total.to_numpy(), workflow["total_mi"]):
            raise AssertionError(f"{class_id}: service MI sum != {workflow['total_mi']}")
    return frame.sort_values(["arrival_s", "workflow_id", "service"]).reset_index(drop=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--seeds", type=int, default=5,
                        help="number of seeds when --all is given")
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--duration-s", type=float, default=300.0)
    parser.add_argument("--deadline-factor", type=float, default=1.0)
    parser.add_argument("--out", default=str(HERE / "traces"))
    args = parser.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    seeds = range(1, args.seeds + 1) if args.all else [args.seed]
    for seed in seeds:
        frame = build(seed, args.duration_s, args.deadline_factor)
        path = out / f"trace_s{seed}.csv"
        frame.to_csv(path, index=False)
        print(f"{path}: {len(frame)} service rows, "
              f"{frame['workflow_id'].nunique()} workflows, "
              f"{int(frame['mi'].sum())} total MI")


if __name__ == "__main__":
    main()
