"""Collect EdgeCloudSim common-scenario runs into the common results schema.

Reads the deep SUCCESS/FAIL logs and the orchestrator decision CSV written by
the adapter, joins them on task id, and emits one row per task with the fields
expected by crosssim/aggregate_common.py.
"""
from __future__ import annotations

import csv
import sys
from pathlib import Path

import pandas as pd

HERE = Path(__file__).resolve().parent
CROSSSIM = HERE.parents[1]
sys.path.insert(0, str(CROSSSIM.parent))

RESULTS = CROSSSIM / "results" / "common_edgecloudsim"
OUT = CROSSSIM / "results" / "common_edgecloudsim" / "jobs.csv"

FIELDS = ["task_id", "device_id", "dc_id", "host_id", "vm_id", "vm_type",
          "task_type", "mi", "input_kb", "output_kb", "start_s", "end_s"]


def parse_log(path: Path, success: bool) -> pd.DataFrame:
    rows = []
    with path.open(encoding="utf-8", errors="ignore") as handle:
        for line in handle:
            if line.startswith("#") or not line.strip():
                continue
            fields = line.strip().replace(",", ".").split(";")
            if len(fields) < 12:
                continue
            values = [float(x) for x in fields[:12]]
            row = dict(zip(FIELDS, values)) | {"success": int(success)}
            row["task_id"] = int(row["task_id"])
            rows.append(row)
    return pd.DataFrame(rows)


def main() -> None:
    frames = []
    for folder in sorted(RESULTS.glob("*-seed*")):
        name = folder.name
        arch = name.rsplit("-seed", 1)[0]
        seed = int(name.rsplit("-seed", 1)[1])
        success = folder / f"SIMRESULT_CBS_{arch}_SUCCESS.log"
        failure = folder / f"SIMRESULT_CBS_{arch}_FAIL.log"
        if not success.exists():
            continue
        tasks = pd.concat([parse_log(success, True), parse_log(failure, False)],
                          ignore_index=True)
        decisions_path = folder / "decisions.csv"
        if decisions_path.exists():
            decisions = pd.read_csv(decisions_path)
            decisions["task_id"] = pd.to_numeric(decisions["task_id"], errors="coerce")
            decisions = decisions.dropna(subset=["task_id"])
            decisions["task_id"] = decisions["task_id"].astype(int)
            decisions = decisions.rename(columns={
                "task_id": "task_id", "decision_s": "decision_s", "tier": "tier"})
            tasks = tasks.merge(decisions[["task_id", "decision_s", "release_s", "tier"]],
                                on="task_id", how="left")
        else:
            tasks["decision_s"], tasks["tier"], tasks["release_s"] = float("nan"), "unknown", float("nan")
        tasks["platform"] = "EdgeCloudSim"
        tasks["scenario"] = "P1-CBS-v1.0"
        tasks["arm"] = "independent"
        tasks["architecture"] = arch
        tasks["seed"] = seed
        tasks["tick_s"] = float("nan")   # no global periodic loop (capability finding)
        tasks["deadline_factor"] = 1.0
        tasks["workflow_id"] = tasks["task_id"].astype(int).astype(str)
        tasks["class"] = tasks["task_type"].map({0: "C1", 1: "C2", 2: "C3"})
        tasks["device"] = tasks["device_id"].astype(int)
        # Deadline from the common trace, keyed by seed and class.
        trace = pd.read_csv(CROSSSIM / "traces" / f"trace_s{seed}.csv")
        deadlines = (trace.groupby("class")["workflow_deadline_s"].first().to_dict())
        tasks["deadline_s"] = tasks["class"].map(deadlines)
        tasks["release_s"] = tasks["release_s"].fillna(tasks["start_s"])
        tasks["exec_start_s"] = tasks["start_s"]
        tasks["complete_s"] = tasks["end_s"]
        tasks["orchestration_latency_s"] = tasks["exec_start_s"] - tasks["release_s"]
        tasks["completion_delay_s"] = tasks["complete_s"] - tasks["release_s"]
        # Recompute the deadline metric against the common deadline.
        tasks["success"] = (tasks["completion_delay_s"] <= tasks["deadline_s"]).astype(int)
        frames.append(tasks)
    if not frames:
        print("[ecs] no runs found")
        return
    out = pd.concat(frames, ignore_index=True)
    columns = ["platform", "scenario", "arm", "architecture", "tick_s", "deadline_factor",
               "seed", "workflow_id", "class", "device", "deadline_s", "tier", "release_s",
               "decision_s", "exec_start_s", "complete_s", "orchestration_latency_s",
               "completion_delay_s", "success"]
    out[columns].to_csv(OUT, index=False)
    print(f"[ecs] wrote {len(out)} tasks to {OUT}")
    print(out.groupby(["architecture", "class"])["success"].mean().round(3).to_string())


if __name__ == "__main__":
    main()
