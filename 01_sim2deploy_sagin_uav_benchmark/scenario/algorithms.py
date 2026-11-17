"""Orchestration algorithms for the P1 benchmark.

The benchmark previously exercised only the simulator's two built-in rules
(``TRADE_OFF`` and ``ROUND_ROBIN``).  Disentangling algorithmic from
architectural and simulator effects requires algorithms that differ in their
*decision mechanism*, not merely in their constants, and it requires the cost of
deciding to be measured alongside the cost of executing.

Every algorithm here:

* observes the same state the simulator exposes to an orchestrator (candidate
  list, per-node pending work, task length, task deadline, device distance);
* is stamped with the simulated instant at which its decision was taken and the
  wall-clock time the decision consumed;
* is deterministic given the run seed.

Algorithms
----------
``trade_off``      simulator default (weighted score, state-blind)
``round_robin``    simulator default (stateless rotation)
``random``         uniform over eligible candidates
``minmin``         classic min-min: smallest own execution time
``leastloaded``    smallest pending work (queue-aware greedy)
``mct``            deadline-aware: smallest predicted completion time
``qos``            tabular Q-learning over tiers, reward = deadline met

``minmin``, ``leastloaded`` and ``mct`` are the standard heuristics of the task
scheduling literature; ``qos`` stands in for the learned orchestrators that the
2026 edge literature now routinely proposes.  ``random`` is included as the
floor against which the others should be judged.
"""
from __future__ import annotations

import time

from pureedgesim.nodes import NodeType
from pureedgesim.orchestrator import Orchestrator

TIER_OF = {NodeType.CLOUD: 0, NodeType.EDGE_DATACENTER: 1, NodeType.EDGE_DEVICE: 2}
# Fixed action space: tier identity, so a Q-table learned under one
# architecture stays comparable under another that exposes fewer tiers.
ALL_TIERS = (0, 1, 2)


class InstrumentedOrchestrator(Orchestrator):
    """Stamps decision time and decision cost on every task it places."""

    name = "instrumented"

    def __init__(self, manager) -> None:
        super().__init__(manager)
        self.stats = {"decisions": 0, "cost_sum": 0.0, "cost_max": 0.0}

    # -- helpers ---------------------------------------------------------
    def _eligible(self, task):
        return [(index, node) for index, node in enumerate(self.candidates)
                if self.offloading_is_possible(node, task)]

    def _pending_mi(self, node) -> float:
        return (sum(item.length for item in node.executing_tasks)
                + sum(item.length for item in node.task_queue))

    def _predict(self, node, task) -> float:
        """Predicted completion time of ``task`` on ``node`` from observable state."""
        compute = task.length / max(1.0, node.mips_per_core)
        queue = self._pending_mi(node) / max(1.0, node.total_mips)
        if node.type == NodeType.EDGE_DEVICE:
            distance = task.source_device.distance_to(node)
            if distance > self.manager.parameters.edge_devices_range:
                return float("inf")
            network = self.manager.parameters.wifi_delay
        else:
            route = self.manager.network.graph.shortest_path(task.source_device, node)
            if not route:
                return float("inf")
            bits = task.request_size + task.result_size
            network = sum(link.delay + bits / max(1.0, link.bandwidth) for link in route)
        return compute + queue + network

    def _stamp(self, task, elapsed: float) -> None:
        task.research_decision_s = float(self.decision_clock)
        task.research_dispatch_s = float(self.manager.engine.clock)
        task.research_decision_cost_ms = float(elapsed) * 1000.0
        self.stats["decisions"] += 1
        self.stats["cost_sum"] += task.research_decision_cost_ms
        self.stats["cost_max"] = max(self.stats["cost_max"],
                                     task.research_decision_cost_ms)

    def _commit(self, index: int, task, elapsed: float) -> int:
        if index >= 0:
            self.history[index] += 1
        self._stamp(task, elapsed)
        return index

    def select(self, eligible, task) -> int:  # pragma: no cover - overridden
        raise NotImplementedError

    def find_computing_node(self, tasks, algorithm: str, application_id: int) -> int:
        task = tasks[0]
        self.decision_clock = float(self.manager.engine.clock)
        started = time.perf_counter()
        eligible = self._eligible(task)
        if not eligible:
            self._stamp(task, time.perf_counter() - started)
            return -1
        index = self.select(eligible, task)
        return self._commit(index, task, time.perf_counter() - started)


class RoundRobin(InstrumentedOrchestrator):
    name = "round_robin"

    def select(self, eligible, task) -> int:
        return min(eligible, key=lambda pair: self.history[pair[0]])[0]


class TradeOff(InstrumentedOrchestrator):
    name = "trade_off"

    def select(self, eligible, task) -> int:
        return self.trade_off(eligible, task)


class Random(InstrumentedOrchestrator):
    """Uniform over the eligible candidates: the performance floor."""

    name = "random"

    def select(self, eligible, task) -> int:
        return eligible[self.manager.random.randrange(len(eligible))][0]


class MinMin(InstrumentedOrchestrator):
    """Classic min-min: smallest standalone execution time, queue-blind."""

    name = "minmin"

    def select(self, eligible, task) -> int:
        return min(eligible,
                   key=lambda pair: task.length / max(1.0, pair[1].mips_per_core))[0]


class LeastLoaded(InstrumentedOrchestrator):
    """Resource-aware greedy: least pending work per unit of capacity."""

    name = "leastloaded"

    def select(self, eligible, task) -> int:
        return min(eligible,
                   key=lambda pair: self._pending_mi(pair[1])
                   / max(1.0, pair[1].total_mips))[0]


class EarliestCompletion(InstrumentedOrchestrator):
    """Deadline-aware: smallest predicted completion time (minimises laxity)."""

    name = "mct"

    def select(self, eligible, task) -> int:
        return min(eligible, key=lambda pair: self._predict(pair[1], task))[0]


class QLearner(InstrumentedOrchestrator):
    """Tabular Q-learning over tiers.

    State
        ``(application_id, load bucket)`` with the load bucket derived from the
        fraction of eligible tiers whose predicted completion exceeds the task
        deadline.  This uses only observable state.
    Action
        index into the *sorted eligible tier list*, so the action space is the
        tier actually available in the current architecture.
    Reward
        ``+1`` when the placed task is returned inside its deadline, ``-1``
        otherwise, applied to the last state-action pair of that task.

    Exploration uses the run's random stream, so training is reproducible.
    """

    name = "qos"

    def __init__(self, manager, alpha: float = 0.2, epsilon: float = 0.15,
                 gamma: float = 0.0) -> None:
        super().__init__(manager)
        self.alpha = float(alpha)
        self.epsilon = float(epsilon)
        self.gamma = float(gamma)
        self.table: dict[tuple[int, int, int], list[float]] = {}
        self.pending: dict[int, tuple[tuple[int, int], int]] = {}
        self.stats.update({"exploit": 0, "explore": 0, "updates": 0})

    # -- state / action --------------------------------------------------
    def _load_bucket(self, eligible, task) -> int:
        deadline = max(1e-9, task.max_latency)
        late = sum(1 for _, node in eligible if self._predict(node, task) > deadline)
        return int(round(len(eligible) * late / max(1, len(eligible))))

    def _value(self, state, size: int) -> list[float]:
        values = self.table.get(state)
        if values is None:
            values = [0.0] * size
            self.table[state] = values
        while len(values) < size:
            values.append(0.0)
        return values

    def select(self, eligible, task) -> int:
        # One representative node per tier: the one with the smallest predicted
        # completion.  The action is the tier itself (cloud / edge / mist), so
        # the Q-table is comparable across architectures that expose different
        # tier sets.
        choices: dict[int, tuple[int, object]] = {}
        for index, node in eligible:
            tier = TIER_OF.get(node.type, 9)
            best = choices.get(tier)
            if best is None or self._predict(node, task) < self._predict(best[1], task):
                choices[tier] = (index, node)
        tiers = sorted(choices)
        state = (int(task.application_id), self._load_bucket(eligible, task))
        values = self._value(state, len(ALL_TIERS))
        if self.manager.random.random() < self.epsilon:
            tier = self.manager.random.choice(tiers)
            self.stats["explore"] += 1
        else:
            tier = max(tiers, key=lambda t: values[t])
            self.stats["exploit"] += 1
        self.pending[int(task.id)] = (state, tier)
        return choices[tier][0]

    def results_returned(self, task) -> None:
        record = self.pending.pop(int(task.id), None)
        if record is None:
            return
        state, tier = record
        values = self._value(state, len(ALL_TIERS))
        reward = 1.0 if task.status.name == "SUCCESS" else -1.0
        values[tier] += self.alpha * (reward + self.gamma * max(values) - values[tier])
        self.stats["updates"] += 1

    def find_computing_node(self, tasks, algorithm: str, application_id: int) -> int:
        task = tasks[0]
        self.decision_clock = float(self.manager.engine.clock)
        started = time.perf_counter()
        eligible = self._eligible(task)
        if not eligible:
            self._stamp(task, time.perf_counter() - started)
            return -1
        index = self.select(eligible, task)
        return self._commit(index, task, time.perf_counter() - started)


ALGORITHMS = {
    "trade_off": TradeOff,
    "round_robin": RoundRobin,
    "random": Random,
    "minmin": MinMin,
    "leastloaded": LeastLoaded,
    "mct": EarliestCompletion,
    "qos": QLearner,
}
FLOORS = ("random",)
DOCUMENTATION = {
    "trade_off": ("weighted score over assigned tasks and per-core speed",
                  "task length, node speed, placement count", "no"),
    "round_robin": ("rotation over candidates", "candidate list only", "no"),
    "random": ("uniform choice", "candidate list only", "no"),
    "minmin": ("smallest own execution time", "task length, node speed", "no"),
    "leastloaded": ("smallest pending work ratio", "queue contents, node capacity", "no"),
    "mct": ("smallest predicted completion time",
            "queue contents, route delay, link bandwidth, task size", "no"),
    "qos": ("tabular Q-learning over tiers",
            "application id, eligible-tier load, own reward history", "yes"),
}
