"""YAFS 3.1 adapter for the P1 common benchmark (P1-CBS v1.0).

Unlike ``yafs_impl/run_yafs.py`` (which uses YAFS's own workload generator),
this adapter consumes the simulator-independent trace produced by
``crosssim/common_trace.py``. That is a Phase-2 requirement: matching numerical
seeds across simulators is not treated as matching experimental inputs.

The independent-task arm aggregates each workflow's services into one task with
the calibrated class length; the workflow arm (not yet wired) would release each
service as a separate YAFS message along the application DAG.

Usage
-----
    python crosssim/adapters/yafs/run_common.py --grid --seeds 5 \
        --out crosssim/results/common_yafs
"""
from __future__ import annotations

import argparse
import copy
import csv
import json
import math
import random
import sys
from pathlib import Path

import networkx as nx
import pandas as pd

CROSSSIM = Path(__file__).resolve().parents[2]          # crosssim/
HERE = CROSSSIM / "adapters" / "yafs"
sys.path.insert(0, str(CROSSSIM / "vendor" / "YAFS" / "src"))
sys.path.insert(0, str(CROSSSIM / "yafs_impl"))

from yafs.core import Sim                             # noqa: E402
from yafs.application import Application, Message, fractional_selectivity  # noqa: E402
from yafs.placement import Placement                  # noqa: E402
from yafs.selection import Selection                  # noqa: E402
from run_yafs import (CLOUD_ID, EDGE_BASE, DEV_BASE, KBITS, APP_NAME, SVC, DONE,  # noqa: E402
                      build_topology, PreBooted, ReturnToOrigin)

SCENARIO = json.loads((CROSSSIM / "scenario.json").read_text(encoding="utf-8"))
TRACES = CROSSSIM / "traces"


def aggregate_trace(seed: int) -> pd.DataFrame:
    """Collapse the per-service trace into one job per workflow."""
    path = TRACES / f"trace_s{seed}.csv"
    frame = pd.read_csv(path)
    first = frame.groupby("workflow_id").first().drop(columns=["mi"])
    total = frame.groupby("workflow_id")["mi"].sum().rename("mi")
    job = first.join(total)
    job["deadline_s"] = job["workflow_deadline_s"]
    return job.reset_index()


class PreBootedCommon(Placement):
    def __init__(self):
        super().__init__("prebooted_common", activation_dist=None)

    def initial_allocation(self, sim, app_name):
        return None


def run_one(arch: str, tick: float, seed: int, deadline_factor: float,
            jobs: pd.DataFrame, out_prefix: str) -> list[dict]:
    random.seed(seed)
    scn = SCENARIO
    topo, dev_types = build_topology(scn)
    net = scn["network"]
    tiers = scn["architectures"][arch]["tiers"]
    allow_mist, allow_cloud = "device" in tiers, "cloud" in tiers

    app = Application(name=APP_NAME)
    classes = [c["id"] for c in scn["workload_classes"]]
    modules = [{"gen-" + cid: {"Type": Application.TYPE_SOURCE}} for cid in classes]
    modules.append({SVC: {"RAM": 1000, "Type": Application.TYPE_MODULE}})
    modules.append({DONE: {"Type": Application.TYPE_SINK}})
    app.set_modules(modules)

    req = {}
    meta = {}
    for cid in classes:
        cls = next(c for c in scn["workload_classes"] if c["id"] == cid)
        m_req = Message("REQ_" + cid, "gen-" + cid, SVC,
                        instructions=1.0,  # per-message instructions are set on the copy below
                        bytes=cls["request_Kbytes"] * KBITS)
        m_res = Message("RES_" + cid, SVC, DONE,
                        instructions=0, bytes=cls["result_Kbytes"] * KBITS)
        app.add_source_messages(m_req)
        app.add_service_module(SVC, m_req, m_res, fractional_selectivity, threshold=1.0)
        req[cid] = m_req
        meta[cid] = {"deadline": cls["deadline_s"] * deadline_factor,
                     "bits_up": cls["request_Kbytes"] * KBITS}

    origin_of, tasks = {}, {}
    sim = Sim(topo, default_results_path=out_prefix)
    sim.deploy_app(app, PreBootedCommon(), ReturnToOrigin(origin_of, None))

    replicas: dict[int, list] = {}

    def deploy_replicas(node, n):
        des = sim.deploy_module(APP_NAME, SVC, app.services[SVC], [node] * int(n))
        replicas[node] = list(des)

    for k in range(scn["edge_tier"]["n_datacenters"]):
        deploy_replicas(EDGE_BASE + k, scn["edge_tier"]["cores"])
    if allow_cloud:
        deploy_replicas(CLOUD_ID, scn["cloud_tier"]["cores"])
    local_replicas = {}
    for nid, t in dev_types.items():
        if allow_mist and t["cores"] > 0:
            deploy_replicas(nid, t["cores"])
            local_replicas[nid] = t["cores"]
    for nid in dev_types:
        sim.deploy_sink(APP_NAME, nid, DONE)

    infra_nodes = [EDGE_BASE + k for k in range(scn["edge_tier"]["n_datacenters"])]
    if allow_cloud:
        infra_nodes.append(CLOUD_ID)

    free_at: dict[int, float] = {}

    def svc_t(mi, node):
        return mi / float(topo.G.nodes[node]["IPT"])

    def hop(node, dev):
        if node == dev:
            return 0.0
        return (net["wan_latency_s"] + net["wifi_latency_s"] if node == CLOUD_ID
                else net["wifi_latency_s"])

    def transmit(bits, node):
        bw = net["wan_bandwidth_Mbps"] if node == CLOUD_ID else net["wifi_bandwidth_Mbps"]
        return bits / (bw * 1e6)

    def pick(cid, dev, now, mi, candidates):
        best, best_cost = None, float("inf")
        for node, des in candidates:
            start = max(now, free_at.get(des, 0.0))
            cost = (start - now) + svc_t(mi, node) + hop(node, dev) + transmit(
                meta[cid]["bits_up"], node)
            if cost < best_cost:
                best, best_cost = (node, des, start), cost
        return best

    def send(tid, node, des, now):
        task = tasks[tid]
        cid = task["class"]
        msg = copy.copy(req[cid])
        msg.inst = task["mi"]          # YAFS charges message.inst / IPT (core.py:443)
        msg.timestamp = task["release"]
        msg.id = tid
        msg.original_DES_src = -1
        msg.app_name = APP_NAME
        msg.dst_int = -1
        msg.idDES = des
        msg.path = list(nx.shortest_path(topo.G, source=task["device"], target=node))
        origin_of[tid] = task["device"]
        free_at[des] = max(now, free_at.get(des, 0.0)) + svc_t(task["mi"], node)
        sim.network_ctrl_pipe.put(msg)
        task["dispatch"] = now
        task["dest_node"] = node
        task["tier"] = ("device" if node == task["device"]
                        else "cloud" if node == CLOUD_ID else "edge")

    def next_tick(now):
        return tick * (math.floor(now / tick + 1e-9) + 1) if tick > 0 else now

    def offload_at(tid, when):
        if when > sim.env.now:
            yield sim.env.timeout(when - sim.env.now)
        task = tasks[tid]
        cand = [(n, d) for n in infra_nodes for d in replicas[n]]
        if not cand:
            return
        node, des, _ = pick(task["class"], task["device"], sim.env.now, task["mi"], cand)
        task["decision"] = sim.env.now
        send(tid, node, des, sim.env.now)

    # The common trace already fixes arrivals; release them as timed processes.
    for _, row in jobs.iterrows():
        device = DEV_BASE + int(row["device_id"])
        if device not in dev_types:
            continue
        tid = row["workflow_id"]
        task = {"id": tid, "class": row["class"], "device": device,
                "source_device_id": int(row["device_id"]),
                "release": float(row["arrival_s"]), "decision": None,
                "dispatch": None, "mi": float(row["mi"]),
                "deadline": float(row["deadline_s"]) * deadline_factor,
                "workflow_id": tid}
        if float(row["arrival_s"]) >= scn["simulation_time_s"]:
            continue
        tasks[tid] = task

        def release(t=tid):
            task_t = tasks[t]
            now = task_t["release"]
            if now > sim.env.now:
                yield sim.env.timeout(now - sim.env.now)
            cand_mist = ([(task_t["device"], d) for d in replicas.get(task_t["device"], [])]
                         if task_t["device"] in local_replicas else [])
            if cand_mist:
                node, des, start = pick(task_t["class"], task_t["device"], now,
                                        task_t["mi"], cand_mist)
                if (start - now) + svc_t(task_t["mi"], node) <= task_t["deadline"]:
                    task_t["decision"] = now
                    send(t, node, des, now)
                    return
            if tick > 0:
                sim.env.process(offload_at(t, next_tick(now)))
            else:
                sim.env.process(offload_at(t, now))

        sim.env.process(release())

    sim.run(scn["simulation_time_s"])
    sim.metrics.close()
    return collect(out_prefix, tasks, arch, tick, deadline_factor, seed)


def collect(prefix, tasks, arch, tick, df, seed):
    events = {}
    with open(prefix + ".csv", newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if not row["id"]:
                continue
            tid = row["id"]
            rec = events.setdefault(tid, {})
            if row["module"] == SVC:
                rec["exec_start"] = float(row["time_in"])
                rec["service"] = float(row["service"])
            elif row["module"] == DONE:
                rec["complete"] = float(row["time_reception"])
    rows = []
    for tid, task in sorted(tasks.items()):
        ev = events.get(tid, {})
        complete, exec_start = ev.get("complete"), ev.get("exec_start")
        delay = None if complete is None else complete - task["release"]
        rows.append({
            "platform": "YAFS-3.1", "run_id": f"common_{arch}_tick{tick:g}_df{df:g}_s{seed}",
            "scenario": "P1-CBS-v1.0", "arm": "independent", "architecture": arch,
            "tick_s": tick, "deadline_factor": df, "seed": seed, "workflow_id": tid,
            "class": task["class"], "device": task.get("source_device_id"), "deadline_s": task["deadline"],
            "tier": task.get("tier", "unplaced"),
            "release_s": round(task["release"], 6),
            "decision_s": None if task["decision"] is None else round(task["decision"], 6),
            "dispatch_s": None if task["dispatch"] is None else round(task["dispatch"], 6),
            "exec_start_s": None if exec_start is None else round(exec_start, 6),
            "complete_s": None if complete is None else round(complete, 6),
            "orchestration_latency_s": None if exec_start is None else round(
                exec_start - task["release"], 6),
            "completion_delay_s": None if delay is None else round(delay, 6),
            "success": 0 if delay is None else int(delay <= task["deadline"]),
        })
    return rows


FIELDS = ("platform run_id scenario arm architecture tick_s deadline_factor seed workflow_id "
          "class device deadline_s tier release_s decision_s dispatch_s exec_start_s complete_s "
          "orchestration_latency_s completion_delay_s success").split()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--grid", action="store_true")
    ap.add_argument("--arch", default=None)
    ap.add_argument("--tick", type=float, default=None)
    ap.add_argument("--deadline-factor", type=float, default=1.0)
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--seeds", type=int, default=5)
    ap.add_argument("--out", default=str(HERE / "results" / "common_yafs"))
    args = ap.parse_args()
    outdir = Path(args.out)
    outdir.mkdir(parents=True, exist_ok=True)

    if args.grid:
        combos = [(a, SCENARIO["temporal_settings"][t]["tick_s"], s)
                  for a in SCENARIO["architecture_order"]
                  for t in SCENARIO["temporal_order"]
                  for s in range(1, args.seeds + 1)]
    else:
        combos = [(args.arch, args.tick, args.seed)]

    trace_file = outdir / f"jobs_df{str(args.deadline_factor).replace('.', 'p')}.csv"
    job_cache = {}
    with open(trace_file, "a", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=FIELDS)
        if trace_file.stat().st_size == 0:
            writer.writeheader()
        for arch, tick, seed in combos:
            if seed not in job_cache:
                job_cache[seed] = aggregate_trace(seed)
            prefix = str(outdir / f"ev_common_{arch}_tick{tick:g}_df{args.deadline_factor:g}_s{seed}")
            rows = run_one(arch, tick, seed, args.deadline_factor, job_cache[seed], prefix)
            writer.writerows(rows)
            fh.flush()
            print(f"[common-yafs] {arch} tick={tick:g} seed={seed}: {len(rows)} jobs, "
                  f"success={sum(r['success'] for r in rows)/max(1,len(rows)):.4f}", flush=True)


if __name__ == "__main__":
    main()
