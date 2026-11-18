"""Agentic-overhead model, workload generator and overhead-aware orchestrator (P2).

The model treats an agentic request as a pipeline whose *effective* demand differs
from the nominal one:

    w~ = w * c_mul * (1 - cache_compute_saving) + c_msgs * c_mi
    d~ = d * p_mul * (1 - cache_payload_saving) + c_msgs * c_kb

where ``c_mul`` is the reasoning compute multiplier (chain-of-thought, tool calls,
verification), ``p_mul`` the payload multiplier (prompt, retrieved context,
scratchpad), and ``c_msgs`` the control messages exchanged per decision.

The overhead-aware orchestrator chooses, for each task, a *fidelity level* (a
smaller model / shorter reasoning budget, which scales compute) and a placement
that minimises an explicit delay-plus-energy cost subject to the task deadline:

    J(v, f) = delay(v, f) + lambda * energy(v, f) + beta * I[cold container] + kappa(v, f)

``beta`` is the container-locality penalty (the node does not hold the application
container) and ``kappa`` the *cache-context* locality penalty, which is only
computed when ``cache_aware`` is true and the task generator recorded a cache hit
for that task; the two are separate mechanisms and are ablated separately.

Every term of ``J``, the feasibility of the deadline constraint and the fidelity
level that was chosen are logged per decision, so the magnitude of each component
and the share of decisions in which it changes the argmin can be reported without
re-deriving them from the prose.

Instrumentation notes
---------------------
* No new randomness is drawn anywhere in this module: the estimators only read
  simulator state, so adding or removing instrumentation cannot move a seed.
* Every switch added for the revision (``cache_aware``, the ``gamma`` fidelity-cost
  curve, the per-term logging) defaults to the behaviour of the grids that were
  already published, so re-running an old job reproduces the old numbers.
"""
from __future__ import annotations

from dataclasses import dataclass, field

from pureedgesim.nodes import NodeType
from pureedgesim.orchestrator import Orchestrator
from pureedgesim.tasks import DefaultTaskGenerator

KB = 8000.0
BITS_PER_KB = KB


@dataclass
class AgenticConfig:
    compute_multiplier: float = 1.0
    payload_multiplier: float = 1.0
    control_messages: int = 3
    control_kb: float = 0.5
    control_mi: float = 150.0
    cache_hit_ratio: float = 0.0
    cache_payload_saving: float = 0.7
    cache_compute_saving: float = 0.25
    fidelity_enabled: bool = False
    fidelity_levels: tuple[float, ...] = (1.0, 0.5, 0.25, 0.1)
    # Fidelity cost curve, as a fraction of the fidelity-free effective work:
    #     C(f) = fidelity_cost_min + (fidelity_cost_max - fidelity_cost_min) * f**gamma
    # The defaults reproduce the linear model C(f) = f of the published grids, so a
    # job that does not set these keys is bit-identical to the old one.
    fidelity_cost_gamma: float = 1.0
    fidelity_cost_min: float = 0.0
    fidelity_cost_max: float = 1.0
    energy_weight: float = 0.02
    deadline_margin: float = 0.15
    # Cost, in seconds, of pulling an application container that the node does
    # not already hold.  This is beta in Eq. (5): it approximates the avoided
    # transfer, not an arbitrary preference.
    container_penalty_s: float = 0.02
    # Cache awareness: whether the orchestrator is allowed to use the locality
    # information recorded by the task generator (``research_cache_hit``) when it
    # ranks candidate nodes.  False = the information is ignored, which is *not*
    # the same thing as pricing the container penalty at zero.
    cache_aware: bool = True
    # Per-decision log; filled in by the orchestrator and written to
    # ``decisions.csv`` by the harness.  Kept out of the serialised metadata.
    decision_log: list = field(default_factory=list)

    @property
    def serialisable(self) -> dict:
        return {key: value for key, value in self.__dict__.items() if key != "decision_log"}


class AgenticTaskGenerator(DefaultTaskGenerator):
    """Default workload generation followed by an explicit agentic overhead model."""

    def __init__(self, manager, config: AgenticConfig) -> None:
        super().__init__(manager)
        self.config = config

    def generate(self):
        tasks = super().generate()
        config = self.config
        # A background tenant is generated on every task-producing device, not on
        # the disjoint device subset the per-application usage split assigns: the
        # point of the contention grid is that both workloads share the same edge
        # nodes.  The default generator gives the last application the leftover
        # devices, which is zero when the agentic classes already claim 100 %.
        background = [app for app in self.manager.applications
                      if app.name.startswith("Background")]
        if background:
            devices = [node for node in self.manager.edge_devices
                       if node.generates_tasks and node.is_alive()]
            identifier = max((task.id for task in tasks), default=0) + 1
            for device in devices:
                identifier = self._generate_for_device(device, background[0], tasks, identifier)
            tasks.sort(key=lambda task: task.time)
        hits = 0
        for task in tasks:
            # A contention workload shares the same fleet but is not an agentic
            # request: it keeps its nominal size so that the agentic classes face a
            # genuinely occupied edge tier rather than a larger version of
            # themselves.
            app_name = self.manager.applications[task.application_id].name
            if app_name.startswith("Background"):
                task.research_overhead_mi = 0.0
                task.research_overhead_kb = 0.0
                task.research_cache_hit = 0
                task.research_cache_saved_kb = 0.0
                task.research_fidelity = 1.0
                task.research_feasible = 1
                task.research_fallback = 0
                task.research_f1_feasible = 1
                task.research_security_level = 0
                continue
            hit = self.random.random() < config.cache_hit_ratio
            hits += int(hit)
            payload = config.payload_multiplier * (1.0 - config.cache_payload_saving if hit else 1.0)
            compute = config.compute_multiplier * (1.0 - config.cache_compute_saving if hit else 1.0)
            nominal_length = task.length
            nominal_request = task.request_size / KB
            task.length = nominal_length * compute + config.control_messages * config.control_mi
            task.request_size = (nominal_request * payload + config.control_messages * config.control_kb) * KB
            task.container_size *= max(1.0, config.payload_multiplier * 0.8)
            task.research_overhead_mi = float(task.length - nominal_length)
            task.research_overhead_kb = float(task.request_size / KB - nominal_request)
            task.research_cache_hit = int(hit)
            # Context bytes that the hit removed from the request; the cache-aware
            # orchestrator prices their re-transfer when it moves the task to a node
            # that does not hold them.
            task.research_cache_saved_kb = float(nominal_request * config.cache_payload_saving) if hit else 0.0
            task.research_fidelity = 1.0
            task.research_feasible = 1
            task.research_fallback = 0
            task.research_f1_feasible = 1
            task.research_security_level = 0
        # Realised cache statistics, read back by OverheadObserver.  These are
        # bookkeeping only: they do not feed any decision.
        self.manager.research_cache_tasks = len(tasks)
        self.manager.research_cache_hits = hits
        return tasks


class OverheadAwareOrchestrator(Orchestrator):
    """Deadline- and energy-aware placement with fidelity and cache awareness."""

    def __init__(self, manager, config: AgenticConfig | None = None) -> None:
        super().__init__(manager)
        self.config = config or AgenticConfig()
        self.stats = {"decisions": 0, "cache_aware_picks": 0, "fidelity_sum": 0.0,
                      "fidelity_low": 0, "deadline_met_estimate": 0, "overhead_mi": 0.0,
                      "overhead_kb": 0.0,
                      # Objective-component magnitudes (item 3.5).
                      "delay_term_sum": 0.0, "energy_term_sum": 0.0, "container_term_sum": 0.0,
                      "locality_term_sum": 0.0, "flip_energy": 0, "flip_container": 0,
                      "flip_locality": 0, "flip_objective": 0, "score_spread_sum": 0.0,
                      # Feasibility / fallback accounting (item 3.6).
                      "feasible": 0, "fallback": 0, "f1_feasible": 0, "constrained_choice": 0,
                      "fid_hist_feasible": {}, "fid_hist_fallback": {}, "tier_picks": {},
                      # Cache mechanism observables.
                      "locality_engaged": 0, "warm_known": 0, "no_destination": 0}
        # Locality belief: the node that last served this (device, application)
        # successfully.  Populated from ``results_returned``.
        self._warm: dict[tuple[int, int], int] = {}

    # -- estimates -------------------------------------------------------------------
    def _network_delay(self, node, task) -> float:
        device = task.source_device
        graph = self.manager.network.graph
        if node.type == NodeType.EDGE_DEVICE:
            if device.distance_to(node) > self.manager.parameters.edge_devices_range:
                return float("inf")
            return self.manager.parameters.wifi_delay
        route = graph.shortest_path(device, node)
        if not route:
            return float("inf")
        return sum(link.delay for link in route)

    def _queue_delay(self, node, task) -> float:
        if node.total_mips <= 0:
            return float("inf")
        pending = sum(item.length for item in node.executing_tasks) + \
            sum(item.length for item in node.task_queue)
        return pending / node.total_mips

    def _energy(self, node, task, delay: float) -> float:
        model = node.energy_model
        dynamic = max(0.0, model.max_consumption - model.idle_consumption) / 3600.0 * min(
            delay, task.length / max(1.0, node.total_mips))
        static = model.idle_consumption / 3600.0 * delay
        return dynamic + static

    def _predict(self, node, task, length: float | None = None) -> dict:
        length = task.length if length is None else length
        compute = length / max(1.0, node.mips_per_core)
        queue = self._queue_delay(node, task)
        network = self._network_delay(node, task)
        delay = compute + queue + network
        return {"delay": delay, "compute": compute, "queue": queue, "network": network,
                "energy": self._energy(node, task, delay)}

    def _container_bonus(self, node, task) -> float:
        """beta: the container-locality penalty of Eq. (5)."""
        return (0.0 if task.application_id in node.installed_containers
                else self.config.container_penalty_s)

    def _route_bandwidth_bps(self, source, target) -> float:
        """Bottleneck bandwidth (bit/s) of the route; PureEdgeSim stores links in bit/s."""
        route = self.manager.network.graph.shortest_path(source, target)
        if not route:
            return 0.0
        return min(float(link.bandwidth) for link in route)

    def _cache_locality_penalty(self, node, task) -> float:
        """kappa: the price of moving a cache-hit task away from its context.

        The generator already reduced the request payload of a cache-hit task on
        the assumption that the retrieved context is where the task runs.  A
        cache-aware orchestrator knows that the assumption only holds on the node
        that served the previous request of the same application from the same
        device; elsewhere the context has to cross the network again, and the
        estimator charges the transfer time of those bytes over the bottleneck
        link.  ``cache_aware=False`` disables the inference, not the price: the
        arm then treats every candidate as equally (un)informed, which is a
        constant and therefore cannot re-rank anything.
        """
        config = self.config
        if not config.cache_aware or not getattr(task, "research_cache_hit", 0):
            return 0.0
        key = (task.source_device.id, task.application_id)
        warm = self._warm.get(key)
        if warm is None:
            return 0.0
        self.stats["warm_known"] += 1
        if warm == node.id:
            return 0.0
        bits = getattr(task, "research_cache_saved_kb", 0.0) * BITS_PER_KB
        if bits <= 0.0:
            return 0.0
        warm_node = next((item for item in self.candidates if item.id == warm), None)
        bandwidth = self._route_bandwidth_bps(warm_node or task.source_device, node) if warm_node else 0.0
        if bandwidth <= 0.0:
            # No route (or no known warm node): fall back to the device access link.
            bandwidth = float(self.manager.parameters.wifi_bandwidth)
        self.stats["locality_engaged"] += 1
        return bits / bandwidth

    def _fidelity_cost(self, level: float) -> float:
        """C(f), the compute fraction that fidelity level ``level`` executes."""
        config = self.config
        low, high = config.fidelity_cost_min, config.fidelity_cost_max
        return low + (high - low) * level ** config.fidelity_cost_gamma

    def _deadline_target(self, task) -> float:
        return task.max_latency * (1.0 - self.config.deadline_margin)

    def _apply_fidelity(self, task) -> float:
        config = self.config
        base = task.length
        target = self._deadline_target(task)
        candidates = self._eligible(task)
        if not config.fidelity_enabled:
            # The single (node, f=1) option: feasible if the best node meets the
            # deadline at full fidelity, and there is no ladder to fall back on.
            estimates = [self._predict(node, task, base) for node in candidates]
            feasible = bool(estimates) and min(item["delay"] for item in estimates) <= target
            task.research_fidelity = 1.0
            task.research_feasible = int(feasible)
            task.research_f1_feasible = int(feasible)
            task.research_fallback = 0
            return base
        best = None
        for level in config.fidelity_levels:
            candidate = base * self._fidelity_cost(level)
            estimates = [self._predict(node, task, candidate) for node in candidates]
            if level == 1.0:
                task.research_f1_feasible = int(bool(estimates) and
                                                min(item["delay"] for item in estimates) <= target)
            if estimates and min(item["delay"] for item in estimates) <= target:
                best = (level, candidate)
                break
        fallback = best is None
        level, length = best or (config.fidelity_levels[-1],
                                 base * self._fidelity_cost(config.fidelity_levels[-1]))
        task.length = length
        task.research_fidelity = float(level)
        task.research_feasible = int(not fallback)
        task.research_fallback = int(fallback)
        return length

    def _eligible(self, task):
        return [node for node in self.candidates if self.offloading_is_possible(node, task)]

    # -- API --------------------------------------------------------------------------
    @staticmethod
    def _first_min(rows, key) -> int:
        """Candidate index of the first minimum, matching ``Orchestrator.trade_off``."""
        best, selected = None, -1
        for row in rows:
            value = key(row)
            if best is None or value < best:
                best, selected = value, row["index"]
        return selected

    def find_computing_node(self, tasks, algorithm: str, application_id: int) -> int:
        task = tasks[0]
        length = self._apply_fidelity(task)
        eligible = [(index, node) for index, node in enumerate(self.candidates)
                    if self.offloading_is_possible(node, task)]
        if not eligible:
            self.stats["no_destination"] += 1
            return -1
        rows = []
        for index, node in eligible:
            estimate = self._predict(node, task, length)
            energy_term = self.config.energy_weight * estimate["energy"]
            container_term = self._container_bonus(node, task)
            locality_term = self._cache_locality_penalty(node, task)
            rows.append({"index": index, "node": node, "estimate": estimate,
                         "delay": estimate["delay"], "energy_term": energy_term,
                         "container_term": container_term, "locality_term": locality_term,
                         "score": estimate["delay"] + energy_term + container_term + locality_term})
        rows.sort(key=lambda row: row["score"])
        chosen = rows[0]
        index, node, estimate = chosen["index"], chosen["node"], chosen["estimate"]
        # Counterfactual re-rankings: drop one component and see whether the
        # argmin moves.  This is what makes the component magnitudes auditable.
        flip_energy = self._first_min(rows, lambda row: row["score"] - row["energy_term"]) != index
        flip_container = self._first_min(rows,
                                         lambda row: row["score"] - row["container_term"]) != index
        flip_locality = self._first_min(rows,
                                        lambda row: row["score"] - row["locality_term"]) != index
        flip_objective = self._first_min(rows, lambda row: row["delay"]) != index
        task.research_predicted_delay = float(estimate["delay"])
        task.research_fidelity = float(task.research_fidelity)
        self.history[index] += 1
        self._log_decision(task, chosen, rows, flip_energy, flip_container, flip_locality,
                           flip_objective)
        return index

    def _log_decision(self, task, chosen, rows, flip_energy, flip_container, flip_locality,
                      flip_objective) -> None:
        index = chosen["index"]
        node = chosen["node"]
        target = self._deadline_target(task)
        feasible_any = any(row["estimate"]["delay"] <= target for row in rows)
        self.stats["decisions"] += 1
        self.stats["fidelity_sum"] += float(task.research_fidelity)
        self.stats["fidelity_low"] += int(task.research_fidelity < 1.0)
        self.stats["deadline_met_estimate"] += int(chosen["estimate"]["delay"] <= task.max_latency)
        self.stats["overhead_mi"] += getattr(task, "research_overhead_mi", 0.0)
        self.stats["overhead_kb"] += getattr(task, "research_overhead_kb", 0.0)
        if task.application_id in node.installed_containers:
            self.stats["cache_aware_picks"] += 1
        self.stats["delay_term_sum"] += chosen["delay"]
        self.stats["energy_term_sum"] += chosen["energy_term"]
        self.stats["container_term_sum"] += chosen["container_term"]
        self.stats["locality_term_sum"] += chosen["locality_term"]
        self.stats["flip_energy"] += int(flip_energy)
        self.stats["flip_container"] += int(flip_container)
        self.stats["flip_locality"] += int(flip_locality)
        self.stats["flip_objective"] += int(flip_objective)
        scores = [row["score"] for row in rows]
        self.stats["score_spread_sum"] += max(scores) - min(scores)
        self.stats["feasible"] += int(bool(getattr(task, "research_feasible", 1)))
        self.stats["fallback"] += int(bool(getattr(task, "research_fallback", 0)))
        self.stats["f1_feasible"] += int(bool(getattr(task, "research_f1_feasible", 1)))
        self.stats["constrained_choice"] += int(len(rows) > 1 and not feasible_any)
        mode = "fid_hist_fallback" if getattr(task, "research_fallback", 0) else "fid_hist_feasible"
        bucket = self.stats[mode]
        level = float(task.research_fidelity)
        bucket[level] = bucket.get(level, 0) + 1
        tier = node.type.value if hasattr(node.type, "value") else str(node.type)
        self.stats["tier_picks"][tier] = self.stats["tier_picks"].get(tier, 0) + 1
        log = self.config.decision_log
        if log is not None and len(log) < 4000:
            log.append({
                "t_s": round(float(self.manager.engine.clock), 4) if self.manager.engine else 0.0,
                "arrival_s": float(task.time),
                "application_id": int(task.application_id),
                "device_id": int(task.source_device.id),
                "cache_hit": int(getattr(task, "research_cache_hit", 0)),
                "deadline_s": float(task.max_latency),
                "eligible": len(rows),
                "fidelity": float(task.research_fidelity),
                "feasible": int(getattr(task, "research_feasible", 1)),
                "fallback": int(getattr(task, "research_fallback", 0)),
                "f1_feasible": int(getattr(task, "research_f1_feasible", 1)),
                "tier": tier,
                "node_id": int(node.id),
                "delay_s": round(float(chosen["delay"]), 6),
                "energy_term_s": round(float(chosen["energy_term"]), 9),
                "container_term_s": round(float(chosen["container_term"]), 6),
                "locality_term_s": round(float(chosen["locality_term"]), 6),
                "locality_max_s": round(max((row["locality_term"] for row in rows), default=0.0), 6),
                "score_s": round(float(chosen["score"]), 6),
                "runner_up_gap_s": (round(float(sorted((row["score"] for row in rows))[1] -
                                           sorted((row["score"] for row in rows))[0]), 6)
                                    if len(rows) > 1 else float("nan")),
                "flip_energy": int(flip_energy),
                "flip_container": int(flip_container),
                "flip_locality": int(flip_locality),
                "flip_objective": int(flip_objective),
            })

    def results_returned(self, task) -> None:
        """Track which node last served this device/application successfully."""
        if task.destination is None or task.failed:
            return
        self._warm[(task.source_device.id, task.application_id)] = task.destination.id


class FidelityOnlyOrchestrator(OverheadAwareOrchestrator):
    """The cheapest strong baseline: default placement plus the ladder.

    It applies the same deadline-driven fidelity reduction as the aware policy
    and then defers to the simulator's default trade-off rule.  Comparing it
    with ``naive`` isolates the ladder; comparing it with ``aware`` isolates
    everything else.
    """

    def find_computing_node(self, tasks, algorithm: str, application_id: int) -> int:
        task = tasks[0]
        self._apply_fidelity(task)
        eligible = [(index, node) for index, node in enumerate(self.candidates)
                    if self.offloading_is_possible(node, task)]
        if not eligible:
            self.stats["no_destination"] += 1
            return -1
        if algorithm == "ROUND_ROBIN":
            index = min(eligible, key=lambda pair: self.history[pair[0]])[0]
        else:
            index = self.trade_off(eligible, task)
        if index >= 0:
            self.history[index] += 1
        self.stats["decisions"] += 1
        self.stats["fidelity_sum"] += float(task.research_fidelity)
        self.stats["fidelity_low"] += int(task.research_fidelity < 1.0)
        self.stats["feasible"] += int(getattr(task, "research_feasible", 1))
        self.stats["fallback"] += int(getattr(task, "research_fallback", 0))
        self.stats["f1_feasible"] += int(getattr(task, "research_f1_feasible", 1))
        bucket = self.stats["fid_hist_fallback" if getattr(task, "research_fallback", 0)
                             else "fid_hist_feasible"]
        level = float(task.research_fidelity)
        bucket[level] = bucket.get(level, 0) + 1
        node = self.candidates[index] if 0 <= index < len(self.candidates) else None
        if node is not None:
            tier = node.type.value if hasattr(node.type, "value") else str(node.type)
            self.stats["tier_picks"][tier] = self.stats["tier_picks"].get(tier, 0) + 1
            log = self.config.decision_log
            if log is not None and len(log) < 4000:
                log.append({"t_s": round(float(self.manager.engine.clock), 4),
                            "arrival_s": float(task.time),
                            "application_id": int(task.application_id),
                            "device_id": int(task.source_device.id),
                            "cache_hit": int(getattr(task, "research_cache_hit", 0)),
                            "deadline_s": float(task.max_latency),
                            "eligible": len(eligible), "fidelity": float(task.research_fidelity),
                            "feasible": int(getattr(task, "research_feasible", 1)),
                            "fallback": int(getattr(task, "research_fallback", 0)),
                            "f1_feasible": int(getattr(task, "research_f1_feasible", 1)),
                            "tier": tier, "node_id": int(node.id)})
        return index


class EdgeOnlyOrchestrator(OverheadAwareOrchestrator):
    """The strong static baseline: edge sites only, same overhead model.

    It shares the fidelity ladder and the delay/energy scoring with the aware
    orchestrator, so the only difference is that cloud and mist peers are never
    eligible.  This is the policy an operator would deploy without an
    overhead-aware placement rule, and it is the baseline the paper must beat
    under stress.
    """

    def _eligible(self, task):
        return [node for node in self.candidates
                if node.type == NodeType.EDGE_DATACENTER and node.is_alive()
                and self.offloading_is_possible(node, task)]


class OverheadObserver:
    """Expose orchestrator-side statistics to the harness."""

    prefix = "overhead"

    def __init__(self, manager, engine) -> None:
        self.manager = manager

    def summary(self) -> dict:
        orchestrator = self.manager.orchestrator
        stats = dict(getattr(orchestrator, "stats", {}))
        tasks = self.manager.metrics
        decisions = stats.get("decisions")
        if decisions:
            stats["mean_fidelity"] = stats.pop("fidelity_sum") / decisions
            stats["low_fidelity_rate"] = stats.pop("fidelity_low") / decisions
            stats["estimated_deadline_rate"] = stats.pop("deadline_met_estimate") / decisions
            # Mean per-component contribution to J at the chosen node (item 3.5).
            stats["term_delay_s"] = stats.pop("delay_term_sum") / decisions
            stats["term_energy_s"] = stats.pop("energy_term_sum") / decisions
            stats["term_container_s"] = stats.pop("container_term_sum") / decisions
            stats["term_locality_s"] = stats.pop("locality_term_sum") / decisions
            stats["term_score_spread_s"] = stats.pop("score_spread_sum") / decisions
            for key in ("flip_energy", "flip_container", "flip_locality", "flip_objective",
                        "feasible", "fallback", "f1_feasible", "constrained_choice",
                        "locality_engaged", "warm_known", "no_destination"):
                if key in stats:
                    stats[key] = stats[key] / decisions
            stats["energy_share_of_score"] = (stats["term_energy_s"] /
                                              max(1e-12, stats["term_delay_s"] +
                                                  stats["term_energy_s"] +
                                                  stats["term_container_s"] +
                                                  stats["term_locality_s"]))
        stats["generated"] = tasks.generated_tasks
        stats["success"] = tasks.succeeded_tasks
        generated = getattr(self.manager, "research_cache_tasks", 0)
        if generated:
            stats["cache_hit_tasks"] = int(generated)
            stats["cache_hit_rate"] = float(getattr(self.manager, "research_cache_hits", 0)) / generated
        return stats
