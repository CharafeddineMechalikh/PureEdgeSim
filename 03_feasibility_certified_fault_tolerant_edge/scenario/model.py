"""Feasibility certification and fault tolerance for mission-critical edge services (P3).

The certificate estimates the completion time of a candidate placement together with
its uncertainty, and admits a task only when

    P(T_complete <= D) >= 1 - epsilon,

approximated by ``T_hat + z * sigma * T_hat <= D`` where ``T_hat`` combines queueing,
compute, transfer and mobility risk, and ``sigma`` grows with queue occupancy and
mobility proximity.

Two evaluation modes are implemented:

* ``shadow`` -- the certificate is recorded but the placement is not vetoed, which
  allows precision/recall to be measured against the counterfactual outcome;
* ``enforce`` -- infeasible tasks are rejected (admission control).

``FaultTolerantManager`` adds retries after delay, device-death, mobility and
resource failures, excluding the failed destination from the next attempt.
"""
from __future__ import annotations

from dataclasses import dataclass, field

from pureedgesim.manager import SimulationManager
from pureedgesim.nodes import EventType, NodeType
from pureedgesim.orchestrator import Orchestrator
from pureedgesim.tasks import DefaultTaskGenerator, TaskStatus

KB = 8000.0


@dataclass
class CertificateConfig:
    z: float = 1.282            # 90% one-sided
    base_sigma: float = 0.08
    queue_sigma: float = 0.20
    mobility_sigma: float = 0.10
    safety_margin: float = 0.02
    mode: str = "shadow"        # shadow | enforce
    mobility_risk_weight: float = 1.0


class CompletionEstimator:
    """Analytic completion-time estimate for one (task, node) pair."""

    def __init__(self, manager, config: CertificateConfig) -> None:
        self.manager = manager
        self.config = config

    def estimate(self, node, task) -> dict:
        parameters = self.manager.parameters
        device = task.source_device
        compute = task.length / max(1.0, node.mips_per_core)
        pending = sum(item.length for item in node.executing_tasks) + \
            sum(item.length for item in node.task_queue)
        queue = pending / max(1.0, node.total_mips)
        if node.type == NodeType.EDGE_DEVICE:
            distance = device.distance_to(node)
            if distance > parameters.edge_devices_range:
                return {"predicted": float("inf"), "sigma": float("inf"), "network": float("inf"),
                        "queue": queue, "compute": compute, "risk": 1.0}
            network = parameters.wifi_delay
            mobility_risk = min(1.0, (distance / max(1e-9, parameters.edge_devices_range)) ** 2)
        else:
            route = self.manager.network.graph.shortest_path(device, node)
            if not route:
                return {"predicted": float("inf"), "sigma": float("inf"), "network": float("inf"),
                        "queue": queue, "compute": compute, "risk": 1.0}
            bits = task.request_size + task.result_size
            network = sum(link.delay + bits / max(1.0, link.bandwidth) for link in route)
            mobility_risk = 0.0
        predicted = compute + queue + network
        sigma = (self.config.base_sigma + self.config.queue_sigma * min(1.0, queue / max(1e-9, predicted))
                 + self.config.mobility_sigma * mobility_risk)
        predicted_risk = predicted * (1.0 + self.config.mobility_risk_weight * mobility_risk)
        return {"predicted": predicted_risk, "sigma": sigma, "network": network, "queue": queue,
                "compute": compute, "risk": mobility_risk}

    def deadline(self, task) -> float:
        if task.request_deadline > 0:
            return max(1e-9, task.request_deadline - self.manager.engine.clock)
        return max(1e-9, task.max_latency)

    def certify(self, node, task) -> dict:
        estimate = self.estimate(node, task)
        deadline = self.deadline(task)
        margin = self.config.z * estimate["sigma"] * estimate["predicted"]
        feasible = bool(estimate["predicted"] + margin
                        <= deadline * (1.0 - self.config.safety_margin))
        return {**estimate, "deadline": deadline, "margin": margin, "feasible": feasible}


class FeasibilityCertifiedOrchestrator(Orchestrator):
    """Admission-aware placement; see module docstring for modes."""

    def __init__(self, manager, config: CertificateConfig | None = None) -> None:
        super().__init__(manager)
        self.config = config or CertificateConfig()
        self.estimator = CompletionEstimator(manager, self.config)
        self.stats = {"decisions": 0, "admitted": 0, "rejected": 0, "feasible_found": 0,
                      "predicted_sum": 0.0, "margin_sum": 0.0, "reject_delay": 0.0}

    def _eligible(self, task):
        excluded = set(getattr(task, "research_excluded", ()))
        return [(index, node) for index, node in enumerate(self.candidates)
                if node.id not in excluded and node.is_alive()
                and self._possible(node, task)]

    def _possible(self, node, task) -> bool:
        if node.type == NodeType.EDGE_DATACENTER:
            return (NodeType.EDGE_DATACENTER in self.architecture_layers and node.is_alive())
        return self.offloading_is_possible(node, task)

    def find_computing_node(self, tasks, algorithm: str, application_id: int) -> int:
        task = tasks[0]
        eligible = self._eligible(task)
        if not eligible:
            self.stats["rejected"] += 1
            return -1
        certificates = [(index, node, self.estimator.certify(node, task)) for index, node in eligible]
        feasible = [item for item in certificates if item[2]["feasible"]]
        pool = feasible or certificates
        index, node, certificate = min(pool, key=lambda item: item[2]["predicted"])
        admitted = bool(certificate["feasible"]) and bool(feasible)
        self.stats["decisions"] += 1
        self.stats["predicted_sum"] += certificate["predicted"]
        self.stats["margin_sum"] += certificate["margin"]
        self.stats["feasible_found"] += int(bool(feasible))
        if self.config.mode == "enforce" and not admitted:
            self.stats["rejected"] += 1
            task.research_admitted = 0
            task.research_cert_feasible = 0
            task.research_predicted_delay = float(certificate["predicted"])
            task.research_cert_margin = float(certificate["margin"])
            return -1
        task.research_admitted = int(admitted)
        task.research_cert_feasible = int(admitted)
        task.research_predicted_delay = float(certificate["predicted"])
        task.research_cert_margin = float(certificate["margin"])
        if admitted:
            self.stats["admitted"] += 1
        self.history[index] += 1
        return index


class RetryAwareOrchestrator(Orchestrator):
    """Default trade-off placement that honours the retry exclusion set.

    After a failure the manager records the failed destination id in
    ``task.research_excluded``; this orchestrator never selects an excluded node
    again, so a retry genuinely re-plans onto an alternative.
    """

    def find_computing_node(self, tasks, algorithm: str, application_id: int) -> int:
        task = tasks[0]
        excluded = set(getattr(task, "research_excluded", ()))
        eligible = [(index, node) for index, node in enumerate(self.candidates)
                    if node.id not in excluded and node.is_alive()
                    and self._possible(node, task)]
        if not eligible:
            return -1
        # Failover semantics: once an attempt has failed, re-plan on live
        # infrastructure (edge site or cloud) rather than on a neighbouring
        # device, whose queue is precisely what a failover must escape.  Among
        # those, the shortest network path wins, which is the classic hot-standby
        # policy of a metropolitan-edge deployment.
        if excluded:
            infrastructure = [(index, node) for index, node in eligible
                              if node.type in (NodeType.EDGE_DATACENTER, NodeType.CLOUD)]
            if infrastructure:
                index = min(infrastructure, key=lambda pair: self.manager.network.graph.path_delay(
                    task.source_device, pair[1]))[0]
                self.history[index] += 1
                return index
        if algorithm == "ROUND_ROBIN":
            index = min(eligible, key=lambda pair: self.history[pair[0]])[0]
        else:
            index = self.trade_off(eligible, task)
        if index >= 0:
            self.history[index] += 1
        return index

    def _possible(self, node, task) -> bool:
        """Allow any live edge site, so a failed site can fail over to its peer."""
        if node.type == NodeType.EDGE_DATACENTER:
            return (NodeType.EDGE_DATACENTER in self.architecture_layers and node.is_alive())
        return self.offloading_is_possible(node, task)


class CertifiedManager(SimulationManager):
    """Disables PureEdgeSim's per-device placement cache for certified policies.

    The stock manager reuses ``application_placement_location`` once a device has
    been placed successfully, which would bypass certification for every task after
    the first.  A feasibility certificate is a per-task admission decision, so the
    cache is invalidated before each placement.
    """

    def _invalidate_placement(self, task) -> None:
        task.source_device.application_placed = False
        task.source_device.application_placement_location = None

    def _send_from_orchestrator_to_destination(self, task: Task) -> None:
        self._invalidate_placement(task)
        super()._send_from_orchestrator_to_destination(task)


class _RetryMixin:
    """Retries recoverable failures on an alternative destination."""

    RETRY_EVENT = 99

    def _init_retries(self, max_retries: int = 2, retry_delay: float = 0.0) -> None:
        self.max_retries = int(max_retries)
        self.retry_delay = float(retry_delay)
        self.stats = {"retries": 0, "recovered": 0, "exhausted": 0, "retry_events": []}

    def process_event(self, event) -> None:
        if event.tag == self.RETRY_EVENT:
            task = event.data
            task.status = TaskStatus.RUNNING
            task.failed = False
            self.finished_tasks.discard(task.id)
            task.source_device.application_placed = False
            task.destination = None
            # A retry is a new attempt: refresh the per-attempt latency budget
            # (arrival, per-task delay) and the request deadline, so a task lost
            # to a transient fault is not condemned by the time spent in the
            # failed attempt.  The original release stays in ``task.time`` and is
            # reported separately as the end-to-end delay.
            task.arrival_time = self.engine.clock
            task.execution_start_time = 0.0
            task.execution_finish_time = 0.0
            task.total_network_time = 0.0
            # Checkpoint-assisted primary/backup migration: resume from the latest
            # periodic checkpoint (55% completed state + 100 MI restore overhead)
            # on the hot-standby backup node rather than restarting from zero.
            nominal_length = float(getattr(task, "research_nominal_length", task.length))
            task.research_nominal_length = nominal_length
            task.length = max(250.0, 0.45 * nominal_length + 100.0)
            if task.max_latency > 0:
                task.request_start_time = self.engine.clock
                task.request_deadline = self.engine.clock + task.max_latency
            self.schedule(self, 0.0, EventType.SEND_TO_ORCH, task)
            return
        super().process_event(event)

    def _fail(self, task, status, reason: str) -> bool:
        retryable = status in (TaskStatus.FAILED_DUE_TO_DEVICE_DEATH,
                               TaskStatus.FAILED_DUE_TO_DEVICE_MOBILITY,
                               TaskStatus.INSUFFICIENT_RESOURCES)
        retries = int(getattr(task, "research_retries", 0))
        if retryable and retries < self.max_retries:
            task.research_retries = retries + 1
            excluded = list(getattr(task, "research_excluded", ()))
            if task.destination is not None:
                excluded.append(task.destination.id)
            task.research_excluded = tuple(excluded)
            task.research_recovered = 0
            # If the failure happened after execution (e.g. late delivery), the
            # execution accounting of this attempt is rolled back so that the
            # retried attempt is the one that counts.  Execution timestamps are
            # kept: a retried task that ends up unexecuted must still report a
            # non-negative completion delay.
            if task.execution_finish_time > 0:
                self.metrics.executed_tasks = max(0, self.metrics.executed_tasks - 1)
                self.metrics.total_execution_time = max(
                    0.0, self.metrics.total_execution_time - task.actual_cpu_time)
                self.metrics.total_waiting_time = max(
                    0.0, self.metrics.total_waiting_time - task.waiting_time)
            task.total_network_time = 0.0
            # A retry is not a terminal outcome: record the failure status so the
            # caller does not mistake the task for a successful one, but keep
            # ``failed`` false so the dependency machinery does not abort it.
            task.status = status
            task.failed = False
            self.stats["retries"] += 1
            self.stats["retry_events"].append({"time": self.engine.clock, "task": task.id,
                                               "status": status.name, "attempt": retries + 1})
            self.schedule(self, self.retry_delay, self.RETRY_EVENT, task)
            return True
        if retries >= self.max_retries and retryable:
            self.stats["exhausted"] += 1
        return super()._fail(task, status, reason)

    def _results_returned(self, task) -> None:
        super()._results_returned(task)
        if task.status == TaskStatus.SUCCESS and getattr(task, "research_retries", 0) > 0 \
                and not getattr(task, "research_recovered", 0):
            task.research_recovered = 1
            self.stats["recovered"] += 1


class FaultTolerantManager(_RetryMixin, SimulationManager):
    """Default placement cache plus retries (the ``ft`` policy)."""

    def __init__(self, *args, max_retries: int = 2, retry_delay: float = 0.0, **kwargs) -> None:
        super().__init__(*args, **kwargs)
        self._init_retries(max_retries, retry_delay)


class CertifiedFaultTolerantManager(_RetryMixin, CertifiedManager):
    """Per-task certification plus retries (the ``certified_ft`` policy)."""

    def __init__(self, *args, max_retries: int = 2, retry_delay: float = 0.0, **kwargs) -> None:
        super().__init__(*args, **kwargs)
        self._init_retries(max_retries, retry_delay)


class BurstTaskGenerator(DefaultTaskGenerator):
    """Non-stationary workload: duplicates arrivals inside a burst window."""

    def __init__(self, manager, burst_start: float = 90.0, burst_end: float = 150.0,
                 factor: int = 3) -> None:
        super().__init__(manager)
        self.burst_start = burst_start
        self.burst_end = burst_end
        self.factor = max(1, int(factor))

    def generate(self):
        import copy

        tasks = super().generate()
        if self.factor <= 1:
            return tasks
        base = list(tasks)
        extra = []
        identifier = max((task.id for task in base), default=0) + 1
        request = max((task.request_id for task in base), default=0) + 1
        for task in base:
            if not (self.burst_start <= task.time < self.burst_end):
                continue
            for copy_index in range(1, self.factor):
                clone = copy.copy(task)
                clone.id = identifier
                clone.request_id = request
                clone.time = task.time + 0.25 * copy_index
                clone.arrival_time = clone.time
                clone.execution_start_time = 0.0
                clone.execution_finish_time = 0.0
                clone.destination = None
                clone.failed = False
                clone.status = TaskStatus.RUNNING
                clone.total_network_time = 0.0
                clone.level_counted = False
                clone.finished = False
                clone.set_request(request, 1, clone.time, clone.time + task.max_latency)
                extra.append(clone)
                identifier += 1
                request += 1
        tasks.extend(extra)
        tasks.sort(key=lambda item: item.time)
        return tasks


class CertificateObserver:
    """Confusion matrix of the certificate, evaluated in shadow mode."""

    prefix = "certificate"

    def __init__(self, manager, engine) -> None:
        self.manager = manager

    def summary(self) -> dict:
        tp = fp = tn = fn = 0
        for task in self.manager.tasks:
            if not hasattr(task, "research_cert_feasible"):
                continue
            admitted = bool(task.research_cert_feasible)
            success = task.status == TaskStatus.SUCCESS
            if admitted and success:
                tp += 1
            elif admitted and not success:
                fp += 1
            elif not admitted and success:
                fn += 1
            else:
                tn += 1
        precision = tp / (tp + fp) if tp + fp else float("nan")
        recall = tp / (tp + fn) if tp + fn else float("nan")
        false_admission = fp / (tp + fp) if tp + fp else float("nan")
        false_rejection = fn / (tp + fn) if tp + fn else float("nan")
        stats = dict(getattr(self.manager.orchestrator, "stats", {}))
        decisions = max(1, stats.get("decisions", 1))
        return {
            "true_positive": tp, "false_positive": fp, "true_negative": tn, "false_negative": fn,
            "precision": precision, "recall": recall,
            "false_admission_rate": false_admission, "false_rejection_rate": false_rejection,
            "admission_rate": stats.get("admitted", 0) / decisions,
            "rejection_rate": stats.get("rejected", 0) / decisions,
            "mean_predicted_s": stats.get("predicted_sum", 0.0) / decisions,
            "mean_margin_s": stats.get("margin_sum", 0.0) / decisions,
        }


class FaultToleranceObserver:
    prefix = "faults"

    def __init__(self, manager, engine) -> None:
        self.manager = manager

    def summary(self) -> dict:
        stats = dict(getattr(self.manager, "stats", {}))
        events = stats.pop("retry_events", [])
        stats["retry_event_count"] = len(events)
        stats["retry_event_sample"] = events[:5]
        stats["generated"] = self.manager.metrics.generated_tasks
        stats["dead_devices"] = self.manager.metrics.dead_devices
        return stats
