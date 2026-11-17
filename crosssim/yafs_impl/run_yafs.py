"""YAFS 3.1 driver for the cross-simulator replication of the P1 temporal-resolution finding.

Usage
-----
    python crosssim/yafs_impl/run_yafs.py --grid --seeds 5 --out crosssim/results/yafs
    python crosssim/yafs_impl/run_yafs.py --arch ALL --tick 1.0 --deadline-factor 1 --seed 1

The vendored YAFS sources are NOT modified (`git -C crosssim/vendor/YAFS status --porcelain`
is empty): only public YAFS API plus SimPy are used.  The periodic control loop is
registered on `sim.env`, YAFS's own SimPy environment.

Scenario source of truth: crosssim/scenario.json.  Metric definitions: crosssim/definitions.md.
Mapping notes and fidelity caveats: crosssim/PORTING_NOTES.md.

Model summary
-------------
* time unit = second;  MI = million instructions; a node's IPT attribute carries
  PureEdgeSim's *per-core* MIPS (DefaultComputingNode.java:300 -> `task.length / mipsPerCore`),
  and one YAFS DES module instance is deployed per core, because YAFS models a deployed
  module as a single server with the node's IPT (core.py:443) and has no core count.
* one Application with a shared "svc" module (three message types) so that all workload
  classes contend for the same core pool, as they do in PureEdgeSim.
* chain: gen-Cx(device) --M_REQ--> svc(replica) --M_RES--> done(sink on the device).
* orchestration: mist-first-deadline-aware; an offloaded request is admitted at the next
  control-loop tick (period = delta), a locally executed one starts at release.
"""
from __future__ import annotations

import argparse
import copy
import csv
import json
import math
import random
from pathlib import Path

import networkx as nx

from yafs.core import Sim
from yafs.topology import Topology
from yafs.application import Application, Message, fractional_selectivity
from yafs.placement import Placement
from yafs.selection import Selection

HERE = Path(__file__).resolve().parent
CROSSSIM = HERE.parent
SCENARIO_PATH = CROSSSIM / "scenario.json"

CLOUD_ID, EDGE_BASE, DEV_BASE = 0, 1, 100
KBITS = 8000.0        # PureEdgeSim writes sizes in Kbytes, converted to bits (ApplicationFileParser.java:114-121)
APP_NAME = "appP1"
SVC, DONE = "svc", "done"


# ---------------------------------------------------------------- topology
def build_topology(scn):
    net = scn["network"]
    n_edge = scn["edge_tier"]["n_datacenters"]
    entity, link = [], []

    entity.append({"id": CLOUD_ID, "model": "cloud", "IPT": scn["cloud_tier"]["mips"],
                   "CORES": scn["cloud_tier"]["cores"], "RAM": scn["cloud_tier"]["ram_MB"],
                   "COST": 0, "WATT": 0.0})
    for k in range(n_edge):
        entity.append({"id": EDGE_BASE + k, "model": "edge", "IPT": scn["edge_tier"]["mips"],
                       "CORES": scn["edge_tier"]["cores"], "RAM": scn["edge_tier"]["ram_MB"],
                       "COST": 0, "WATT": 0.0})
        link.append({"s": CLOUD_ID, "d": EDGE_BASE + k,
                     "BW": net["wan_bandwidth_Mbps"], "PR": net["wan_latency_s"]})
    for a in range(n_edge):
        for b in range(a + 1, n_edge):
            link.append({"s": EDGE_BASE + a, "d": EDGE_BASE + b,
                         "BW": net["man_bandwidth_Mbps"], "PR": net["man_latency_s"]})

    types = []
    for t in scn["fleet"]["device_types"]:
        types += [t] * int(round(scn["fleet"]["n_devices"] * t["percentage"] / 100.0))
    while len(types) < scn["fleet"]["n_devices"]:
        types.append(types[len(types) % len(scn["fleet"]["device_types"])])
    types = types[: scn["fleet"]["n_devices"]]

    devices = {}
    for i, t in enumerate(types):
        nid = DEV_BASE + i
        entity.append({"id": nid, "model": "dev-" + t["name"], "IPT": max(t["mips"], 1),
                       "CORES": t["cores"], "RAM": t["ram_MB"], "COST": 0, "WATT": 0.0})
        # 500 m area with 350 m MEC coverage -> every node hears two sites
        for e in (i % n_edge, (i + 1) % n_edge):
            link.append({"s": nid, "d": EDGE_BASE + e,
                         "BW": net["wifi_bandwidth_Mbps"], "PR": net["wifi_latency_s"]})
        devices[nid] = t
    topo = Topology()
    topo.load({"entity": entity, "link": link})
    return topo, devices


# ---------------------------------------------------------------- application
def build_application(scn, deadline_factor):
    a = Application(name=APP_NAME)
    mods = [{"gen-" + c["id"]: {"Type": Application.TYPE_SOURCE}} for c in scn["workload_classes"]]
    mods.append({SVC: {"RAM": 1000, "Type": Application.TYPE_MODULE}})
    mods.append({DONE: {"Type": Application.TYPE_SINK}})
    a.set_modules(mods)

    meta, req = {}, {}
    for c in scn["workload_classes"]:
        cid = c["id"]
        # YAFS divides Message.bytes by (BW_Mbps * 1e6), i.e. the field is consumed as BITS (core.py:292-296)
        m_req = Message("REQ_" + cid, "gen-" + cid, SVC,
                        instructions=c["task_length_MI"], bytes=c["request_Kbytes"] * KBITS)
        m_res = Message("RES_" + cid, SVC, DONE,
                        instructions=0, bytes=c["result_Kbytes"] * KBITS)
        a.add_source_messages(m_req)
        a.add_service_module(SVC, m_req, m_res, fractional_selectivity, threshold=1.0)
        req[cid] = m_req
        meta[cid] = {"deadline": c["deadline_s"] * deadline_factor,
                     "period": 60.0 / c["rate_tasks_per_min_per_device"],
                     "mi": float(c["task_length_MI"]),
                     "container_MB": float(c["container_Mbytes"]),
                     "bits_up": c["request_Kbytes"] * KBITS,
                     "bits_down": c["result_Kbytes"] * KBITS}
    return a, meta, req


class PreBooted(Placement):
    """One-shot placement: containers are installed by the driver before the run."""

    def __init__(self):
        super().__init__("prebooted", activation_dist=None)

    def initial_allocation(self, sim, app_name):
        return None


class ReturnToOrigin(Selection):
    """Routes a result message back to the device that released the task.

    YAFS calls get_path synchronously whenever a module forwards a message, so the
    downlink is a normal YAFS network exchange.  The origin is recovered from the
    message id, which YAFS preserves when forwarding (core.py:636).
    """

    def __init__(self, origin_of, net_cost):
        super().__init__()
        self.origin_of = origin_of
        self.net_cost = net_cost      # (up_bits, msg) -> bookkeeping, unused here

    def get_path(self, sim, app_name, message, topology_src, alloc_DES, alloc_module, traffic, from_des):
        dst_node = self.origin_of.get(message.id)
        if dst_node is None:
            return [], []
        try:
            path = list(nx.shortest_path(sim.topology.G, source=topology_src, target=dst_node))
        except (nx.NetworkXNoPath, nx.NodeNotFound):
            return [], []
        for des in alloc_module[app_name][DONE]:
            if alloc_DES[des] == dst_node:
                return [path], [des]
        return [], []


# ---------------------------------------------------------------- one run
def run_one(scn, arch, tick, deadline_factor, seed, out_prefix):
    random.seed(seed)
    topo, dev_types = build_topology(scn)
    app, meta, req = build_application(scn, deadline_factor)
    net = scn["network"]

    tiers = scn["architectures"][arch]["tiers"]
    allow_mist = "device" in tiers
    allow_cloud = "cloud" in tiers

    origin_of, tasks = {}, {}
    sim = Sim(topo, default_results_path=out_prefix)
    sim.deploy_app(app, PreBooted(), ReturnToOrigin(origin_of, None))

    # ---- one DES replica per CPU core, on every node of an enabled tier ---------------
    replicas = {}     # node -> [des, ...]

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

    # ---- orchestrator's own view (a virtual D/D/1 pool per node) ----------------------
    free_at = {}                 # des -> simulation time at which this core is free
    up_tx = {}                   # (node) -> worst-case uplink transmission time, used in the estimate

    def svc_t(mi, node):
        return mi / float(topo.G.nodes[node]["IPT"])

    def hop(node, dev):
        if node == dev:
            return 0.0
        prop = net["wifi_latency_s"] if node != CLOUD_ID else net["wan_latency_s"] + net["wifi_latency_s"]
        return prop

    def transmit(bits, node, dev):
        bw = net["wifi_bandwidth_Mbps"] if node != CLOUD_ID else net["wan_bandwidth_Mbps"]
        return bits / (bw * 1e6)

    def pick(cid, dev, now, candidates):
        """Least expected completion time among `candidates` (node, des)."""
        mi = meta[cid]["mi"]
        best, best_cost = None, float("inf")
        for node, des in candidates:
            start = max(now, free_at.get(des, 0.0))
            cost = (start - now) + svc_t(mi, node) + hop(node, dev) \
                + transmit(meta[cid]["bits_up"], node, dev)
            if cost < best_cost:
                best, best_cost = (node, des, start), cost
        return best

    def send(cid, node, des, tid, now):
        msg = copy.copy(req[cid])
        msg.timestamp = tasks[tid]["release"]
        msg.id = tid
        msg.original_DES_src = -1
        msg.app_name = APP_NAME
        msg.dst_int = -1
        msg.idDES = des
        msg.path = list(nx.shortest_path(topo.G, source=tasks[tid]["device"], target=node))
        origin_of[tid] = tasks[tid]["device"]
        free_at[des] = max(now, free_at.get(des, 0.0)) + svc_t(meta[cid]["mi"], node)
        sim.network_ctrl_pipe.put(msg)
        tasks[tid]["dispatch"] = now
        tasks[tid]["dest_node"] = node
        tasks[tid]["tier"] = ("device" if node == tasks[tid]["device"]
                              else ("cloud" if node == CLOUD_ID else "edge"))

    counter = [0]

    def next_tick(now):
        """First control-loop instant strictly after `now`.  With PureEdgeSim's
        integer-second release grid and delta in {1, 0.2, 0.05} every release lands on a
        tick boundary, so the realised wait is one whole period -- the same aliasing the
        reference implementation shows (P1 measured 1.01 s at delta = 1 s)."""
        return tick * (math.floor(now / tick + 1e-9) + 1)

    def offload_at(tid, when):
        if when > sim.env.now:
            yield sim.env.timeout(when - sim.env.now)
        cid = tasks[tid]["class"]
        dev = tasks[tid]["device"]
        cand = [(n, d) for n in infra_nodes for d in replicas[n]]
        if not cand:
            return
        node, des, _start = pick(cid, dev, sim.env.now, cand)
        tasks[tid]["decision"] = sim.env.now
        send(cid, node, des, tid, sim.env.now)

    def device_source(nid, cid):
        period = meta[cid]["period"]
        while True:
            yield sim.env.timeout(period)
            if sim.env.now > scn["simulation_time_s"]:
                return
            counter[0] += 1
            tid = counter[0]
            tasks[tid] = {"id": tid, "class": cid, "device": nid, "release": sim.env.now,
                          "decision": None, "dispatch": None}
            mi = meta[cid]["mi"]
            deadline = meta[cid]["deadline"]
            # mist-first, deadline-aware: local if the local pool still meets the deadline
            if nid in local_replicas:
                cand = [(nid, d) for d in replicas[nid]]
                node, des, start = pick(cid, nid, sim.env.now, cand)
                if (start - sim.env.now) + svc_t(mi, nid) <= deadline:
                    tasks[tid]["decision"] = sim.env.now
                    send(cid, nid, des, tid, sim.env.now)
                    continue
            if tick > 0:
                sim.env.process(offload_at(tid, next_tick(sim.env.now)))
            else:
                sim.env.process(offload_at(tid, sim.env.now))

    share = [c["device_share"] for c in scn["workload_classes"]]
    n = scn["fleet"]["n_devices"]
    bounds = [0]
    for s in share:
        bounds.append(min(n, bounds[-1] + int(round(n * s))))
    bounds[-1] = n
    for k, cid in enumerate([c["id"] for c in scn["workload_classes"]]):
        for idx in range(bounds[k], bounds[k + 1]):
            sim.env.process(device_source(DEV_BASE + idx, cid))

    sim.run(scn["simulation_time_s"])
    sim.metrics.close()
    return collect(out_prefix, tasks, meta, arch, tick, deadline_factor, seed)


# ---------------------------------------------------------------- results
def collect(prefix, tasks, meta, arch, tick, deadline_factor, seed):
    events = {}
    with open(prefix + ".csv", newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if not row["id"]:
                continue
            tid = int(float(row["id"]))
            rec = events.setdefault(tid, {})
            if row["module"] == SVC:
                rec["exec_start"] = float(row["time_in"])
                rec["svc"] = float(row["service"])
                rec["dest"] = int(float(row["TOPO.dst"]))
            elif row["module"] == DONE:
                rec["complete"] = float(row["time_reception"])

    rows = []
    for tid, tk in sorted(tasks.items()):
        ev = events.get(tid, {})
        complete, exec_start = ev.get("complete"), ev.get("exec_start")
        d = meta[tk["class"]]["deadline"]
        delay = None if complete is None else complete - tk["release"]
        rows.append({
            "platform": "YAFS-3.1",
            "run_id": "%s_tick%g_df%g_seed%d" % (arch, tick, deadline_factor, seed),
            "architecture": arch, "tick_s": tick, "deadline_factor": deadline_factor, "seed": seed,
            "task_id": tid, "class": tk["class"], "device": tk["device"], "deadline_s": d,
            "tier": tk.get("tier", "unplaced"),
            "release_s": round(tk["release"], 6),
            "decision_s": None if tk["decision"] is None else round(tk["decision"], 6),
            "dispatch_s": None if tk["dispatch"] is None else round(tk["dispatch"], 6),
            "exec_start_s": None if exec_start is None else round(exec_start, 6),
            "complete_s": None if complete is None else round(complete, 6),
            "service_s": None if "svc" not in ev else round(ev["svc"], 6),
            "release_to_decision_s": None if tk["decision"] is None else round(tk["decision"] - tk["release"], 6),
            "decision_to_dispatch_s": None if (tk["decision"] is None or tk["dispatch"] is None) else round(tk["dispatch"] - tk["decision"], 6),
            "dispatch_to_exec_s": None if (exec_start is None or tk["dispatch"] is None) else round(exec_start - tk["dispatch"], 6),
            "orchestration_latency_s": None if exec_start is None else round(exec_start - tk["release"], 6),
            "completion_delay_s": None if delay is None else round(delay, 6),
            "success": 0 if delay is None else int(delay <= d),
        })
    return rows


FIELDS = ("platform run_id architecture tick_s deadline_factor seed task_id class device deadline_s tier "
          "release_s decision_s dispatch_s exec_start_s complete_s service_s release_to_decision_s "
          "decision_to_dispatch_s dispatch_to_exec_s orchestration_latency_s completion_delay_s "
          "success").split()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--arch", default=None)
    ap.add_argument("--tick", type=float, default=None)
    ap.add_argument("--deadline-factor", type=float, default=1.0)
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--seeds", type=int, default=1, help="run seeds 1..N (grid mode)")
    ap.add_argument("--grid", action="store_true")
    ap.add_argument("--out", default=str(CROSSSIM / "results" / "yafs"))
    args = ap.parse_args()

    scn = json.loads(SCENARIO_PATH.read_text())
    outdir = Path(args.out)
    outdir.mkdir(parents=True, exist_ok=True)
    trace_path = outdir / ("tasks_df%s.csv" % str(args.deadline_factor).replace(".", "p"))

    if args.grid:
        combos = [(a, scn["temporal_settings"][t]["tick_s"], args.deadline_factor, s)
                  for a in scn["architecture_order"]
                  for t in scn["temporal_order"]
                  for s in range(1, args.seeds + 1)]
    else:
        combos = [(args.arch, args.tick, args.deadline_factor, args.seed)]

    new_file = not trace_path.exists()
    with open(trace_path, "a", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=FIELDS)
        if new_file:
            w.writeheader()
        for arch, tick, df, seed in combos:
            prefix = str(outdir / ("ev_%s_tick%g_df%g_seed%d" % (arch, tick, df, seed)))
            rows = run_one(scn, arch, tick, df, seed, prefix)
            w.writerows(rows)
            fh.flush()
            ok = sum(r["success"] for r in rows) / max(len(rows), 1)
            print("done %-15s tick=%-5g df=%-4g seed=%d  n=%d  success=%.4f" % (arch, tick, df, seed, len(rows), ok), flush=True)


if __name__ == "__main__":
    main()
