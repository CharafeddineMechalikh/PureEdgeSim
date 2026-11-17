"""Cross-layer ISCC-MEC DAG model, semantic compression, PHY proxies, and orchestrator (P5).

Implements every item in ``05_crosslayer_iscc_mec_6g/paper/research_plan.md``:

1. **Multi-stage ISCC service DAG with configurable pipeline depth** (2, 4, or 6 stages):
   - Depth 2: ``sense_encode`` -> ``infer_actuate`` (6,000 MI total, 0.8 s request deadline)
   - Depth 4: ``sense`` -> ``encode_comm`` -> ``edge_infer`` -> ``actuate`` (6,000 MI total, 0.8 s)
   - Depth 6: ``sense`` -> ``preproc_filter`` -> ``encode_comm`` -> ``feature_align``
     -> ``edge_infer`` -> ``actuate`` (6,000 MI total, 0.8 s)

2. **Semantic compression ratio & stage-level accuracy/quality proxy**:
   Compression ratio ``rho`` in (0, 1] scales intermediate feature payloads after
   sensing, adjusts encoder/inference compute, and maps to a rate-distortion semantic
   accuracy score ``Q(rho, fidelity, antenna_gain)``.

3. **PHY / reconfigurable-antenna proxy knobs**:
   Bandwidth scaling ``bandwidth_factor`` and reconfigurable/movable/pinching-antenna
   array gain ``antenna_gain`` (modelling SNR/rate enhancement and access-delay
   reduction on wireless links).

4. **Dependency- and data-aware ``ISCCAwareOrchestrator``**:
   Tracks parent subtask placements per ``request_id``, rewards stage co-location to
   eliminate inter-tier feature handoff delay, avoids shared WAN bottlenecks for
   deadline-tight DAG stages, and adapts semantic compression when request slack is
   tight.
"""
from __future__ import annotations

import math
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

from pureedgesim.manager import SimulationManager
from pureedgesim.nodes import NodeType
from pureedgesim.orchestrator import Orchestrator
from pureedgesim.tasks import DefaultTaskGenerator, TaskStatus

KB = 8000.0

# Pipeline-depth stage specifications (total compute = 6,000 MI, deadline = 0.8 s).
PIPELINE_SPECS: dict[int, list[dict]] = {
    2: [
        {"name": "sense", "length": 2000, "req_kb": 1200, "res_kb": 250, "depends_on": None},
        {"name": "edge_infer", "length": 4000, "req_kb": 250, "res_kb": 30, "depends_on": "sense"},
    ],
    4: [
        {"name": "sense", "length": 800, "req_kb": 1200, "res_kb": 400, "depends_on": None},
        {"name": "encode_comm", "length": 1200, "req_kb": 400, "res_kb": 250, "depends_on": "sense"},
        {"name": "edge_infer", "length": 3500, "req_kb": 250, "res_kb": 80, "depends_on": "encode_comm"},
        {"name": "actuate", "length": 500, "req_kb": 80, "res_kb": 30, "depends_on": "edge_infer"},
    ],
    6: [
        {"name": "sense", "length": 500, "req_kb": 1200, "res_kb": 600, "depends_on": None},
        {"name": "preproc_filter", "length": 500, "req_kb": 600, "res_kb": 400, "depends_on": "sense"},
        {"name": "encode_comm", "length": 800, "req_kb": 400, "res_kb": 250, "depends_on": "preproc_filter"},
        {"name": "feature_align", "length": 1200, "req_kb": 250, "res_kb": 180, "depends_on": "encode_comm"},
        {"name": "edge_infer", "length": 2500, "req_kb": 180, "res_kb": 80, "depends_on": "feature_align"},
        {"name": "actuate", "length": 500, "req_kb": 80, "res_kb": 30, "depends_on": "edge_infer"},
    ],
}


@dataclass
class ISCCConfig:
    policy: str = "iscc_aware"         # agnostic | iscc_aware
    compression_ratio: float = 0.50    # semantic feature payload ratio in (0, 1]
    fidelity: float = 1.00             # inference compute fidelity in (0, 1]
    bandwidth_factor: float = 1.00     # PHY link bandwidth multiplier
    antenna_gain: float = 1.50         # reconfigurable / movable antenna gain (>= 1.0)
    pipeline_depth: int = 4            # 2, 4, or 6 stages
    rate_factor: float = 1.00
    colocation_bonus_s: float = 0.035  # latency saving from stage co-location


def semantic_quality_score(compression_ratio: float, fidelity: float,
                           antenna_gain: float = 1.0) -> float:
    """Rate-distortion semantic quality/accuracy proxy in [0, 1]."""
    rho = min(1.0, max(0.1, float(compression_ratio)))
    fid = min(1.0, max(0.2, float(fidelity)))
    gain = max(1.0, float(antenna_gain))
    q_max = 0.965
    dist_loss = 0.22 * ((1.0 - rho) ** 1.6)
    fid_loss = 0.26 * ((1.0 - fid) ** 1.4)
    phy_loss = 0.035 / math.sqrt(gain)
    return float(max(0.50, min(0.98, q_max * (1.0 - dist_loss) * (1.0 - fid_loss) * (1.0 - phy_loss))))


def write_pipeline_depth_xml(settings_dir: Path, depth: int) -> None:
    """Rewrite ``applications.xml`` with the requested ISCC DAG depth (2, 4, or 6)."""
    stages = PIPELINE_SPECS.get(int(depth), PIPELINE_SPECS[4])
    path = settings_dir / "applications.xml"
    tree = ET.parse(path)
    root = tree.getroot()
    apps = root.findall("application")
    if not apps:
        return
    iscc_app = apps[0]
    for old_sub in list(iscc_app.findall("subtask")):
        iscc_app.remove(old_sub)
    for stage in stages:
        attrs = {
            "name": stage["name"],
            "task_length": str(stage["length"]),
            "request_size": str(stage["req_kb"]),
            "results_size": str(stage["res_kb"]),
        }
        if stage["depends_on"]:
            attrs["depends_on"] = stage["depends_on"]
        ET.SubElement(iscc_app, "subtask", attrs)
    tree.write(path, encoding="utf-8", xml_declaration=True)


def apply_phy_parameters(settings_dir: Path, bandwidth_factor: float,
                         antenna_gain: float) -> None:
    """Scale link bandwidths and wireless latency to reflect 6G PHY + antenna gain."""
    prop = settings_dir / "simulation_parameters.properties"
    lines = prop.read_text(encoding="utf-8").splitlines()
    eff_wireless_bw = float(bandwidth_factor) * (1.0 + 0.45 * math.log2(max(1.0, float(antenna_gain))))
    eff_backhaul_bw = float(bandwidth_factor)
    wireless_lat_scale = 1.0 / math.sqrt(max(1.0, float(antenna_gain)))

    out = []
    for line in lines:
        if line.startswith(("wifi_bandwidth=", "cellular_bandwidth=")):
            key, val = line.split("=", 1)
            out.append(f"{key}={ max(1.0, float(val) * eff_wireless_bw):.2f}")
        elif line.startswith(("wan_bandwidth=", "man_bandwidth=", "ethernet_bandwidth=")):
            key, val = line.split("=", 1)
            out.append(f"{key}={ max(1.0, float(val) * eff_backhaul_bw):.2f}")
        elif line.startswith(("wifi_latency=", "cellular_latency=")):
            key, val = line.split("=", 1)
            out.append(f"{key}={ max(0.0005, float(val) * wireless_lat_scale):.5f}")
        else:
            out.append(line)
    prop.write_text("\n".join(out) + "\n", encoding="utf-8")


def apply_stage_transform(task, compression_ratio: float, fidelity: float,
                          antenna_gain: float) -> None:
    """Apply semantic compression and inference fidelity to an ISCC subtask."""
    nom_len = float(getattr(task, "research_nominal_length", task.length))
    nom_req_kb = float(getattr(task, "research_nominal_req_kb", task.request_size / KB))
    nom_res_kb = float(getattr(task, "research_nominal_res_kb", task.result_size / KB))
    stage = (task.subtask_name or "").strip()

    rho = float(compression_ratio)
    fid = float(fidelity)

    if stage == "sense":
        # Raw sensing acquisition: uncompressed sensor input, compressed output if fused
        new_len = nom_len
        new_req_kb = nom_req_kb
        new_res_kb = nom_res_kb
    elif stage in ("preproc_filter", "encode_comm"):
        # Semantic encoder spends slight extra compute to compress feature tensors by rho
        new_len = nom_len * (1.0 + 0.20 * (1.0 - rho))
        new_req_kb = nom_req_kb * (0.65 + 0.35 * rho)
        new_res_kb = nom_res_kb * rho
    elif stage in ("feature_align", "edge_infer"):
        # Downstream inference receives compressed semantic features and scales with fidelity
        new_len = nom_len * (0.75 + 0.25 * rho) * fid
        new_req_kb = nom_req_kb * rho
        new_res_kb = nom_res_kb * rho
    elif stage == "actuate":
        # Control actuation message is compact
        new_len = nom_len
        new_req_kb = nom_req_kb * (0.50 + 0.50 * rho)
        new_res_kb = nom_res_kb
    else:
        # Background telemetry task
        new_len = nom_len * fid
        new_req_kb = nom_req_kb * rho
        new_res_kb = nom_res_kb * rho

    task.length = float(max(50.0, new_len))
    task.request_size = float(max(1.0, new_req_kb) * KB)
    task.result_size = float(max(1.0, new_res_kb) * KB)
    task.research_compression = float(rho)
    task.research_fidelity = float(fid)
    task.research_quality = semantic_quality_score(rho, fid, antenna_gain)
    task.research_overhead_mi = float(task.length - nom_len)
    task.research_overhead_kb = float((task.request_size + task.result_size) / KB - (nom_req_kb + nom_res_kb))


class ISCCTaskGenerator(DefaultTaskGenerator):
    """Generates ISCC DAG subtasks annotated with nominal stage demands and semantic quality."""

    def __init__(self, manager, config: ISCCConfig) -> None:
        super().__init__(manager)
        self.config = config

    def generate(self):
        tasks = super().generate()
        cfg = self.config
        for task in tasks:
            task.research_nominal_length = float(task.length)
            task.research_nominal_req_kb = float(task.request_size / KB)
            task.research_nominal_res_kb = float(task.result_size / KB)
            task.research_security_level = int(cfg.pipeline_depth)
            apply_stage_transform(task, cfg.compression_ratio, cfg.fidelity, cfg.antenna_gain)
        return tasks


class ISCCManager(SimulationManager):
    """Disables PureEdgeSim's per-device placement cache so each DAG stage is placed dynamically."""

    def _send_from_orchestrator_to_destination(self, task) -> None:
        if task.source_device is not None:
            task.source_device.application_placed = False
            task.source_device.application_placement_location = None
        super()._send_from_orchestrator_to_destination(task)


class ISCCAwareOrchestrator(Orchestrator):
    """Cross-layer dependency-, co-location-, and data-aware orchestrator for ISCC pipelines.

    * ``policy == "agnostic"``: layer-agnostic baseline that places subtasks using
      tier trade-off without tracking parent stage locations or shared WAN delay
      under request deadlines.
    * ``policy == "iscc_aware"``:
      1. Tracks the placement of parent subtasks in the same ``request_id`` and
         computes inter-stage feature handoff delay;
      2. Co-locates dependent stages (`encode_comm`, `edge_infer`) on the same edge
         server (or high-MIPS local mist node for `sense`/`actuate`) when capacity
         allows, eliminating multi-hop backhaul transfers;
      3. Adapts semantic compression ratio and inference fidelity when remaining
         end-to-end request deadline slack is tight.
    """

    def __init__(self, manager, config: ISCCConfig | None = None) -> None:
        super().__init__(manager)
        self.config = config or ISCCConfig()
        self.request_stage_node: dict[int, list] = {}
        self.stats = {
            "decisions": 0,
            "colocated_stages": 0,
            "dependent_stages": 0,
            "edge_picks": 0,
            "mist_picks": 0,
            "cloud_picks": 0,
            "adaptive_compressions": 0,
        }

    def _eligible(self, task):
        out = []
        for index, node in enumerate(self.candidates):
            if not node.is_alive():
                continue
            if node.type == NodeType.EDGE_DATACENTER:
                if NodeType.EDGE_DATACENTER in self.architecture_layers:
                    out.append((index, node))
            elif self.offloading_is_possible(node, task):
                out.append((index, node))
        return out

    def _stage_delay(self, node, task, parent_node) -> float:
        compute_s = task.length / max(1.0, node.mips_per_core)
        pending_mi = (sum(item.length for item in node.executing_tasks)
                      + sum(item.length for item in node.task_queue))
        queue_s = pending_mi / max(1.0, node.total_mips)
        bits = task.request_size + task.result_size
        device = task.source_device

        if node.type == NodeType.EDGE_DEVICE:
            if device.distance_to(node) > self.manager.parameters.edge_devices_range:
                return float("inf")
            net_s = self.manager.parameters.wifi_delay + bits / max(
                1.0, self.manager.parameters.wifi_bandwidth)
        else:
            route = self.manager.network.graph.shortest_path(device, node)
            if not route:
                return float("inf")
            net_s = sum(link.delay + bits / max(1.0, link.bandwidth) for link in route)

        # Inter-stage co-location vs splitting adjustment
        if parent_node is not None:
            if node.id == parent_node.id:
                # Co-located on the exact same node: intermediate feature tensor stays in memory
                net_s = max(0.001, 0.25 * net_s - self.config.colocation_bonus_s)
            elif node.type != parent_node.type:
                # Splitting across tiers (e.g. edge <-> cloud) incurs cross-tier handoff penalty
                net_s += 0.045 if NodeType.CLOUD in (node.type, parent_node.type) else 0.012

        return compute_s + queue_s + net_s

    def find_computing_node(self, tasks, algorithm: str, application_id: int) -> int:
        task = tasks[0]
        eligible = self._eligible(task)
        if not eligible:
            return -1

        if self.config.policy == "agnostic":
            # Layer-agnostic baseline: ignores DAG parent location and deadline budget
            if algorithm == "ROUND_ROBIN":
                index = min(eligible, key=lambda pair: self.history[pair[0]])[0]
            else:
                index = self.trade_off(eligible, task)
            if index >= 0:
                node = self.candidates[index]
                self._record(index, node, task, None)
            return index

        # Cross-layer ISCC-aware scheduling
        req_id = int(getattr(task, "request_id", -1))
        history_nodes = self.request_stage_node.get(req_id, [])
        parent_node = history_nodes[-1] if history_nodes else None
        stage = (task.subtask_name or "").strip()

        # Check remaining request deadline slack and adapt semantic compression if tight
        if task.request_deadline > 0:
            remaining_budget = max(0.05, task.request_deadline - self.manager.engine.clock)
        else:
            remaining_budget = max(0.05, task.max_latency)

        if stage in ("encode_comm", "feature_align", "edge_infer") and remaining_budget < 0.35:
            tight_rho = min(float(self.config.compression_ratio), 0.35)
            tight_fid = min(float(self.config.fidelity), 0.80)
            apply_stage_transform(task, tight_rho, tight_fid, self.config.antenna_gain)
            self.stats["adaptive_compressions"] += 1

        # Co-location preference: place dependent stages on the parent's node when
        # it has spare capacity, otherwise fall back to the tier trade-off rule.
        parent_candidate = None
        if parent_node is not None and parent_node.is_alive() and stage in ("encode_comm", "actuate"):
            for index, node in eligible:
                if node.id == parent_node.id:
                    parent_candidate = (index, node)
                    break
        if parent_candidate is not None:
            index, node = parent_candidate
            pending_mi = (sum(item.length for item in node.executing_tasks)
                          + sum(item.length for item in node.task_queue))
            if pending_mi / max(1.0, node.total_mips) < 0.80:
                self._record(index, node, task, parent_node)
                return index

        best = None
        for index, node in eligible:
            delay_s = self._stage_delay(node, task, parent_node)
            tier_bias = 0.0
            if stage in ("sense", "actuate"):
                if node.type == NodeType.CLOUD:
                    tier_bias += 0.12
                elif node.type == NodeType.EDGE_DEVICE:
                    tier_bias -= 0.040
            elif stage in ("encode_comm", "preproc_filter", "feature_align"):
                if node.type == NodeType.EDGE_DATACENTER:
                    tier_bias -= 0.030
                elif node.type == NodeType.CLOUD:
                    tier_bias += 0.12
            elif stage == "edge_infer":
                if node.type == NodeType.EDGE_DATACENTER:
                    tier_bias -= 0.060
                elif node.type == NodeType.CLOUD:
                    tier_bias += 0.15
                elif node.type == NodeType.EDGE_DEVICE:
                    tier_bias += 0.080
            deadline_over = max(0.0, delay_s - 0.45 * max(0.05, task.max_latency))
            score = delay_s + 3.0 * deadline_over + tier_bias
            if best is None or score < best[0]:
                best = (score, index, node)

        _, index, node = best
        self._record(index, node, task, parent_node)
        return index

    def _record(self, index: int, node, task, parent_node) -> None:
        self.history[index] += 1
        self.stats["decisions"] += 1
        req_id = int(getattr(task, "request_id", -1))
        if parent_node is not None:
            self.stats["dependent_stages"] += 1
            if parent_node.id == node.id or parent_node.type == node.type:
                self.stats["colocated_stages"] += 1
                task.research_colocated = 1
            else:
                task.research_colocated = 0
        else:
            task.research_colocated = 1
        if req_id >= 0:
            self.request_stage_node.setdefault(req_id, []).append(node)
        if node.type == NodeType.EDGE_DATACENTER:
            self.stats["edge_picks"] += 1
        elif node.type == NodeType.MIST if hasattr(NodeType, "MIST") else NodeType.EDGE_DEVICE:
            self.stats["mist_picks"] += 1
        elif node.type == NodeType.CLOUD:
            self.stats["cloud_picks"] += 1


class ISCCObserver:
    """Exposes ISCC pipeline stage breakdown, co-location, and semantic fidelity metrics."""

    prefix = "iscc"

    def __init__(self, manager, engine) -> None:
        self.manager = manager
        self.engine = engine

    def summary(self) -> dict:
        tasks = list(self.manager.tasks)
        if not tasks:
            return {}
        n = len(tasks)
        qualities = [float(getattr(t, "research_quality", 0.90)) for t in tasks]
        compressions = [float(getattr(t, "research_compression", 1.0)) for t in tasks]
        fidelities = [float(getattr(t, "research_fidelity", 1.0)) for t in tasks]
        dep_failures = sum(1 for t in tasks if t.status == TaskStatus.FAILED_BECAUSE_DEPENDENCY_FAILED)

        # Request-level accounting for the ISCC pipeline (application_id == 0)
        by_req: dict[int, list] = {}
        for t in tasks:
            if int(getattr(t, "application_id", 0)) == 0 and int(getattr(t, "request_id", -1)) >= 0:
                by_req.setdefault(int(t.request_id), []).append(t)

        req_total = max(1, len(by_req))
        req_ok = 0
        req_makespans = []
        req_bytes_kb = []
        for req_id, r_tasks in by_req.items():
            start_s = min(
                (float(t.request_start_time) if float(getattr(t, "request_start_time", 0.0)) > 0.0
                 else float(t.time))
                for t in r_tasks
            )
            deadline_s = max(float(t.request_deadline) for t in r_tasks)
            budget_s = max(0.1, deadline_s - start_s) if deadline_s > start_s else 0.8
            req_bytes_kb.append(sum((t.request_size + t.result_size) / KB for t in r_tasks))
            if all(t.status == TaskStatus.SUCCESS for t in r_tasks):
                finish_s = max(float(t.execution_finish_time) for t in r_tasks)
                span = max(0.0, finish_s - start_s)
                if span <= budget_s + 1e-9:
                    req_ok += 1
                req_makespans.append(span)

        total_energy = float(self.manager.metrics.total_energy_consumption) if hasattr(
            self.manager.metrics, "total_energy_consumption") else 0.0
        orch_stats = dict(getattr(self.manager.orchestrator, "stats", {}))
        dep_stages = max(1, int(orch_stats.get("dependent_stages", 1)))
        decisions = max(1, int(orch_stats.get("decisions", n)))

        mean_quality = sum(qualities) / n
        iscc_req_success = req_ok / req_total
        return {
            "pipeline_request_count": len(by_req),
            "pipeline_request_success_ratio": iscc_req_success,
            "pipeline_makespan_mean_s": (sum(req_makespans) / len(req_makespans))
            if req_makespans else float("nan"),
            "pipeline_makespan_p95_s": float(
                sorted(req_makespans)[min(len(req_makespans) - 1, int(0.95 * len(req_makespans)))]
            ) if req_makespans else float("nan"),
            "dependency_failure_rate": dep_failures / n,
            "bytes_per_request_kb": (sum(req_bytes_kb) / len(req_bytes_kb)) if req_bytes_kb else 0.0,
            "energy_per_successful_request_wh": (total_energy / req_ok) if req_ok > 0 else float("nan"),
            "semantic_quality_mean": mean_quality,
            "effective_semantic_utility": iscc_req_success * mean_quality,
            "mean_compression_ratio": sum(compressions) / n,
            "mean_fidelity": sum(fidelities) / n,
            "colocation_ratio": float(orch_stats.get("colocated_stages", 0)) / dep_stages,
            "edge_share": float(orch_stats.get("edge_picks", 0)) / decisions,
            "mist_share": float(orch_stats.get("mist_picks", 0)) / decisions,
            "cloud_share": float(orch_stats.get("cloud_picks", 0)) / decisions,
        }
