"""Deterministic single-run harness around PureEdgeSim Python.

Every experiment run materialises its own settings folder (so provenance is exact),
loads the latest PureEdgeSim from the cloned checkout, drives one scenario manually,
and returns the standard 56-column row plus per-task/per-request records and derived
statistics.

The harness also provides fault-injection entities (device kill, link drop, battery
drain) and an observer protocol used by the five projects to attach instrumentation
that the aggregate PureEdgeSim row does not expose (stage breakdown, certificate
outcomes, overhead accounting, ...).
"""
from __future__ import annotations

import json
import math
import shutil
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable, Iterable

import numpy as np

from .paths import ensure_pureedgesim

SETTINGS_FILES = (
    "simulation_parameters.properties",
    "edge_devices.xml",
    "edge_datacenters.xml",
    "cloud.xml",
    "applications.xml",
)

COL_GENERATED = "Number of generated tasks"
COL_SUCCESS = "Tasks successfully executed"
COL_NOT_EXECUTED = "Task not executed (No resources available or long waiting time)"
COL_DELAY = "Tasks failed (delay)"
COL_DEATH = "Tasks failed (device dead)"
COL_MOBILITY = "Tasks failed (mobility)"
COL_DEPENDENCY = "Tasks not executed because a dependency failed"
COL_ENERGY = "Energy consumption of computing nodes (Wh)"
COL_WAN_USAGE = "Wan usage (s)"
COL_NETWORK_USAGE = "Network usage (s)"
COL_AVG_DELAY = "Average execution delay (s)"
COL_TRAFFIC = "Total network traffic (MBytes)"
COL_REQUESTS = "Requests generated"
COL_REQ_SUCCESS = "Successful requests"
COL_REQ_MAKESPAN = "Average request makespan (s)"


# --------------------------------------------------------------------------------------
# Settings materialisation
# --------------------------------------------------------------------------------------
def update_properties(path: Path, updates: dict[str, Any]) -> None:
    """Replace or append properties, preserving unrelated lines and comments."""
    lines = path.read_text(encoding="utf-8").splitlines()
    remaining = {str(k): str(v) for k, v in updates.items()}
    out: list[str] = []
    for line in lines:
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and "=" in stripped:
            key = stripped.split("=", 1)[0].strip()
            if key in remaining:
                out.append(f"{key}={remaining.pop(key)}")
                continue
        out.append(line)
    if remaining:
        out.append("")
        out.append("# --- experiment overrides ---")
        for key, value in remaining.items():
            out.append(f"{key}={value}")
    path.write_text("\n".join(out) + "\n", encoding="utf-8")


def prepare_settings(base_settings: Path | str, destination: Path | str,
                     properties: dict[str, Any] | None = None,
                     applications_file: Path | str | None = None,
                     mutate: Callable[[Path], None] | None = None) -> Path:
    """Copy the five settings files and apply deterministic overrides."""
    base = Path(base_settings)
    dest = Path(destination)
    dest.mkdir(parents=True, exist_ok=True)
    for name in SETTINGS_FILES:
        src = base / name
        if not src.is_file():
            raise FileNotFoundError(f"Missing settings file: {src}")
        shutil.copyfile(src, dest / name)
    if applications_file is not None:
        shutil.copyfile(Path(applications_file), dest / "applications.xml")
    if properties:
        update_properties(dest / "simulation_parameters.properties", properties)
    if mutate is not None:
        mutate(dest)
    return dest


def set_device_mobility(settings_dir: Path | str, model: str | None) -> None:
    """Rewrite the mobility model of every ``<device>`` in ``edge_devices.xml``.

    ``None`` / ``"static"`` removes the ``<mobility>`` element, making devices fixed.
    """
    import xml.etree.ElementTree as ElementTree

    path = Path(settings_dir) / "edge_devices.xml"
    tree = ElementTree.parse(path)
    root = tree.getroot()
    for element in root.findall("device"):
        mobility = element.find("mobility")
        if model in (None, "", "static", "none"):
            if mobility is not None:
                element.remove(mobility)
            continue
        if mobility is None:
            mobility = ElementTree.SubElement(element, "mobility")
        mobility.text = "true"
        mobility.set("model", model)
        if model in ("waypoint", "random-waypoint") and not mobility.get("waypoints"):
            if model == "waypoint":
                mobility.set("waypoints", "100,100;400,150;250,400;50,300")
    tree.write(path, encoding="utf-8", xml_declaration=False)


def set_device_battery(settings_dir: Path | str, capacity: float | None = None,
                       initial: float | None = None) -> None:
    """Scale or set battery capacity / initial level for every device."""
    import xml.etree.ElementTree as ElementTree

    path = Path(settings_dir) / "edge_devices.xml"
    tree = ElementTree.parse(path)
    root = tree.getroot()
    for element in root.findall("device"):
        if capacity is not None:
            node = element.find("batteryCapacity")
            if node is None:
                node = ElementTree.SubElement(element, "batteryCapacity")
            node.text = str(capacity)
        if initial is not None:
            node = element.find("initialBatteryLevel")
            if node is None:
                node = ElementTree.SubElement(element, "initialBatteryLevel")
            node.text = str(initial)
    tree.write(path, encoding="utf-8", xml_declaration=False)


def scale_application_rate(settings_dir: Path | str, factor: float,
                           only: Iterable[str] | None = None) -> None:
    """Multiply the ``<rate>`` of applications (optionally only named ones)."""
    import xml.etree.ElementTree as ElementTree

    path = Path(settings_dir) / "applications.xml"
    tree = ElementTree.parse(path)
    root = tree.getroot()
    names = set(only) if only is not None else None
    for element in root.findall("application"):
        name = element.attrib.get("name", "")
        if names is not None and name not in names:
            continue
        node = element.find("rate")
        if node is None:
            continue
        value = float((node.text or "0").strip() or 0)
        node.text = str(round(value * factor, 6))
    tree.write(path, encoding="utf-8", xml_declaration=False)


# --------------------------------------------------------------------------------------
# Fault injection
# --------------------------------------------------------------------------------------
def _select_devices(manager, selector: str, count: int):
    devices = [node for node in manager.edge_devices if node.is_alive()]
    if selector == "battery_first":
        devices.sort(key=lambda n: n.get_remaining_battery_percentage())
    elif selector == "highest_load":
        devices.sort(key=lambda n: len(n.task_queue) + len(n.executing_tasks), reverse=True)
    elif selector == "random":
        manager.random.shuffle(devices)
    else:  # deterministic spread by id
        devices.sort(key=lambda n: n.id)
    return devices[: max(0, int(count))]


def _make_entity():
    """Import lazily so this module stays importable without PureEdgeSim on the path."""
    ensure_pureedgesim()
    from pureedgesim.engine import SimEntity

    return SimEntity


class _InjectionEntity:
    """Shared machinery: arms itself at simulation start and replays a schedule."""

    kind = "injection"

    def __new__(cls, manager, engine, events):  # noqa: D401 - factory
        SimEntity = _make_entity()

        class _Entity(SimEntity):
            def __init__(self) -> None:
                super().__init__(engine)
                self.manager = manager
                self.queue = sorted(events, key=lambda item: item[0])
                self.index = 0
                self.log: list[dict] = []
                self.fired: list[float] = []

            def on_simulation_start(self) -> None:
                if self.queue:
                    self.schedule(self, max(0.0, self.queue[0][0]), 1, None)

            def process_event(self, event) -> None:
                if event.tag != 1:
                    return
                spec = self.queue[self.index]
                self.index += 1
                self.fired.append(float(self.engine.clock))
                self.act(spec)
                if self.index < len(self.queue):
                    delay = max(0.0, self.queue[self.index][0] - self.engine.clock)
                    self.schedule(self, delay, 1, None)

            # -- subclass hook --------------------------------------------------------
            def act(self, spec) -> None:
                raise NotImplementedError

        entity = _Entity()
        entity.kind = cls.kind
        return entity


def _fail_queued_tasks(manager, node) -> int:
    """Fails the tasks queued on a node that has just died.

    Queued tasks have no scheduled execution event, so without this hook they
    would never reach a terminal state.  Failing them through the manager lets a
    fault-tolerant manager re-plan them on an alternative node.
    """
    from pureedgesim.tasks import TaskStatus

    queue = getattr(node, "task_queue", None)
    if not queue:
        return 0
    failed = 0
    for task in list(queue):
        if task.failed:
            continue
        manager._fail(task, TaskStatus.FAILED_DUE_TO_DEVICE_DEATH, "node died with queued task")
        failed += 1
    queue.clear()
    return failed


class KillSchedule(_InjectionEntity):
    """Kills devices at given simulated times.

    ``events`` is a sequence of ``(time_s, selector, count)``; selectors are
    ``battery_first``, ``highest_load``, ``random`` and ``spread``.
    """

    kind = "kill"

    def __new__(cls, manager, engine, events):
        entity = super().__new__(cls, manager, engine, events)

        def act(spec):
            from pureedgesim.nodes import NodeStatus

            time_s, selector, count = spec
            for node in _select_devices(manager, selector, count):
                if node.is_alive():
                    node.status = NodeStatus.DEAD
                    node.death_time = engine.clock
                    manager.metrics.dead_devices += 1
                    _fail_queued_tasks(manager, node)
                    entity.log.append({"time": engine.clock, "id": node.id, "name": node.name,
                                       "selector": selector, "type": "kill"})

        entity.act = act
        return entity


class LinkDropSchedule(_InjectionEntity):
    """Drops the access links of selected devices for a window.

    ``events`` is a sequence of ``(start_s, duration_s, selector, count)``.  The
    device keeps running; only its uplink/downlink disappear from the routing graph,
    which forces the orchestrator to observe connectivity loss.
    """

    kind = "link_drop"

    def __new__(cls, manager, engine, events):
        entity = super().__new__(cls, manager, engine, events)
        pending: dict[int, list] = {}

        def detach(spec):
            start, duration, selector, count = spec
            for node in _select_devices(manager, selector, count):
                if node.up_link is None or node.down_link is None:
                    continue
                graph = manager.network.graph
                for link in (node.up_link, node.down_link):
                    try:
                        graph.adjacency[link.source.id].remove((link.destination.id, link))
                    except (KeyError, ValueError):
                        pass
                    if link in graph.links:
                        graph.links.remove(link)
                graph._cache.clear()
                pending[node.id] = [node, engine.clock + duration]
                entity.log.append({"time": engine.clock, "id": node.id, "name": node.name,
                                   "type": "link_drop", "duration": duration})
            if pending:
                entity.schedule(entity, max(0.0, duration), 2, None)

        def restore(_event) -> None:
            ready = [value for value in pending.values() if value[1] <= engine.clock + 1e-9]
            for node, _ready_at in ready:
                node.up_link = None
                node.down_link = None
                manager.connect_with(node, node.serving_edge_data_center)
                pending.pop(node.id, None)
                entity.log.append({"time": engine.clock, "id": node.id, "name": node.name,
                                   "type": "link_restore"})
            if pending:
                next_time = min(value[1] for value in pending.values())
                entity.schedule(entity, max(0.0, next_time - engine.clock), 2, None)

        def process_event(event) -> None:
            if event.tag == 1:
                spec = entity.queue[entity.index]
                entity.index += 1
                entity.fired.append(float(engine.clock))
                detach(spec)
                if entity.index < len(entity.queue):
                    delay = max(0.0, entity.queue[entity.index][0] - engine.clock)
                    entity.schedule(entity, delay, 1, None)
            elif event.tag == 2:
                restore(event)

        entity.process_event = process_event
        return entity


class OutageSchedule(_InjectionEntity):
    """Takes infrastructure nodes down for a window, then restores them.

    ``events`` is a sequence of ``(start_s, duration_s, target, count)`` where
    ``target`` is ``edge_datacenters`` or ``cloud``.  Unlike :class:`KillSchedule`
    the nodes come back, which models a site outage (maintenance, power, switch
    failure) during which fault-tolerant orchestration can fail over and later
    recover.
    """

    kind = "outage"

    def __new__(cls, manager, engine, events):
        from pureedgesim.nodes import NodeStatus

        entity = super().__new__(cls, manager, engine, events)
        pending: list[dict] = []

        def candidates(target: str) -> list:
            if target == "cloud":
                return list(manager.nodes_generator.cloud_nodes)
            return list(manager.nodes_generator.edge_data_centers)

        def down(spec) -> None:
            start, duration, target, count = spec
            for node in candidates(target)[: max(1, int(count))]:
                if node.is_dead():
                    continue
                node.status = NodeStatus.DEAD
                node.death_time = engine.clock
                _fail_queued_tasks(manager, node)
                pending.append({"node": node, "until": engine.clock + duration, "target": target})
                entity.log.append({"time": engine.clock, "id": node.id, "name": node.name,
                                   "type": "outage_start", "target": target, "duration": duration})
            entity.schedule(entity, max(0.0, duration), 2, None)

        def up(_event) -> None:
            ready = [item for item in pending if item["until"] <= engine.clock + 1e-9]
            for item in ready:
                node = item["node"]
                node.status = NodeStatus.ALIVE
                node.death_time = -1.0
                pending.remove(item)
                entity.log.append({"time": engine.clock, "id": node.id, "name": node.name,
                                   "type": "outage_end", "target": item["target"]})
            if pending:
                next_time = min(item["until"] for item in pending)
                entity.schedule(entity, max(0.0, next_time - engine.clock), 2, None)

        def process_event(event) -> None:
            if event.tag == 1:
                spec = entity.queue[entity.index]
                entity.index += 1
                entity.fired.append(float(engine.clock))
                down(spec)
                if entity.index < len(entity.queue):
                    delay = max(0.0, entity.queue[entity.index][0] - engine.clock)
                    entity.schedule(entity, delay, 1, None)
            elif event.tag == 2:
                up(event)

        entity.process_event = process_event
        return entity


class BatteryDrainSchedule(_InjectionEntity):
    """Accelerates battery depletion of selected devices.

    ``events`` is a sequence of ``(time_s, selector, count, fraction)`` where
    ``fraction`` is the share of the initial capacity removed (0.5 = cut the
    remaining charge in half).
    """

    kind = "battery_drain"

    def __new__(cls, manager, engine, events):
        entity = super().__new__(cls, manager, engine, events)

        def act(spec):
            time_s, selector, count, fraction = spec
            for node in _select_devices(manager, selector, count):
                if not node.is_battery_powered():
                    continue
                energy = node.energy_model
                amount = energy.battery_capacity * float(fraction)
                energy.cpu_energy_consumption += amount
                entity.log.append({"time": engine.clock, "id": node.id, "name": node.name,
                                   "type": "battery_drain", "fraction": fraction,
                                   "remaining": node.get_remaining_battery_percentage()})

        entity.act = act
        return entity


# --------------------------------------------------------------------------------------
# Records and derived metrics
# --------------------------------------------------------------------------------------
def task_records(manager) -> list[dict]:
    """One record per generated task, including failure cause and routing."""
    records = []
    for task in manager.tasks:
        destination = task.destination
        records.append({
            "task_id": task.id,
            "request_id": task.request_id,
            "application_id": task.application_id,
            "task_type": task.type,
            "subtask": task.subtask_name or "",
            "status": task.status.name,
            "success": int(task.status.name == "SUCCESS"),
            "failed": int(bool(task.failed)),
            "length_mi": float(task.length),
            "request_kb": float(task.request_size) / 8000.0,
            "result_kb": float(task.result_size) / 8000.0,
            "container_kb": float(task.container_size) / 8000.0,
            "max_latency_s": float(task.max_latency),
            "arrival_s": float(task.time),
            "exec_start_s": float(task.execution_start_time),
            "exec_finish_s": float(task.execution_finish_time),
            "cpu_s": float(task.actual_cpu_time),
            "wait_s": max(0.0, float(task.waiting_time)),
            "network_s": float(task.total_network_time),
            "total_delay_s": max(0.0, float(task.total_delay)),
            # End-to-end delay measured from the original release, including any
            # time spent in failed attempts.  Defined only for tasks that ended
            # up executed (otherwise there is no honest completion instant).
            "e2e_delay_s": (max(0.0, float(task.execution_finish_time - task.time))
                            if task.execution_finish_time >= task.time else float("nan")),
            "request_deadline_s": float(task.request_deadline),
            "source_id": task.source_device.id,
            "destination_id": destination.id if destination is not None else -1,
            "destination_type": destination.type.value if destination is not None else "NONE",
            "admitted": int(getattr(task, "research_admitted", 1)),
            "security_level": int(getattr(task, "research_security_level", 0)),
            "poisoned": int(getattr(task, "research_poisoned", 0)),
            "fidelity": float(getattr(task, "research_fidelity", 1.0)),
            "retry_count": int(getattr(task, "research_retries", 0)),
            "recovered": int(getattr(task, "research_recovered", 0)),
            "compression_ratio": float(getattr(task, "research_compression", 1.0)),
            "overhead_mi": float(getattr(task, "research_overhead_mi", 0.0)),
            "overhead_kb": float(getattr(task, "research_overhead_kb", 0.0)),
            "cert_margin_s": float(getattr(task, "research_cert_margin", float("nan"))),
            "cert_feasible": int(getattr(task, "research_cert_feasible", 1)),
            "predicted_delay_s": float(getattr(task, "research_predicted_delay", float("nan"))),
            # Orchestration timing, stamped by the instrumented orchestrators.
            "decision_s": float(getattr(task, "research_decision_s", float("nan"))),
            "dispatch_s": float(getattr(task, "research_dispatch_s", float("nan"))),
            "decision_cost_ms": float(getattr(task, "research_decision_cost_ms", float("nan"))),
        })
    return records


def request_records(manager, records: list[dict] | None = None) -> list[dict]:
    """One record per request (service graph instance).

    ``request_id`` is the unit of end-to-end service quality for graph applications;
    a request succeeds only when every subtask succeeds.
    """
    if records is None:
        records = task_records(manager)
    by_request: dict[int, list[dict]] = {}
    for record in records:
        if record["request_id"] >= 0:
            by_request.setdefault(record["request_id"], []).append(record)
    out = []
    for request_id, tasks in by_request.items():
        start = min(task["arrival_s"] for task in tasks)
        deadline = max(task["request_deadline_s"] for task in tasks)
        budget = max(0.0, deadline - start) if deadline > start else max(task["max_latency_s"] for task in tasks)
        success = all(task["success"] for task in tasks)
        makespan = max(task["exec_finish_s"] for task in tasks) - start if success else float("nan")
        out.append({
            "request_id": request_id,
            "start_s": float(start),
            "deadline_s": float(deadline),
            "budget_s": float(budget),
            "task_count": len(tasks),
            "failed": int(not success),
            "success": int(success),
            "makespan_s": float(makespan),
            "within_deadline": int(bool(success) and makespan <= budget + 1e-9),
        })
    out.sort(key=lambda item: item["request_id"])
    return out


def _percentiles(values: np.ndarray, qs=(50, 90, 95, 99)) -> dict[str, float]:
    if values.size == 0:
        return {f"p{q}": float("nan") for q in qs}
    return {f"p{q}": float(np.percentile(values, q)) for q in qs}


def derive_metrics(row: dict, records: list[dict], requests: list[dict] | None = None) -> dict:
    """Add task-level statistics that the aggregate 56-column row does not expose."""
    out: dict[str, Any] = {}
    generated = int(row.get(COL_GENERATED, 0)) or 0
    if records:
        statuses = np.array([r["status"] for r in records])
        success = np.array([r["success"] for r in records])
        delay = np.array([r["total_delay_s"] for r in records])
        cpu = np.array([r["cpu_s"] for r in records])
        wait = np.array([r["wait_s"] for r in records])
        net = np.array([r["network_s"] for r in records])
        bytes_kb = np.array([r["request_kb"] + r["result_kb"] for r in records])
        out.update({
            "task_success_fraction": float(success.mean()) if success.size else float("nan"),
            "task_delay_mean_s": float(delay.mean()),
            "task_delay_p50_s": _percentiles(delay)["p50"],
            "task_delay_p95_s": _percentiles(delay)["p95"],
            "task_delay_p99_s": _percentiles(delay)["p99"],
            "task_cpu_mean_s": float(cpu.mean()),
            "task_wait_mean_s": float(wait.mean()),
            "task_wait_p95_s": float(np.percentile(wait, 95)),
            "task_network_mean_s": float(net.mean()),
            "task_bytes_mean_kb": float(bytes_kb.mean()),
            "task_retries_total": int(sum(r["retry_count"] for r in records)),
            "task_recovered_total": int(sum(r["recovered"] for r in records)),
            "task_admitted_total": int(sum(r["admitted"] for r in records)),
            "task_admission_rate": float(np.mean([r["admitted"] for r in records])),
            "fail_delay": int(np.sum(statuses == "FAILED_DUE_TO_DELAY")),
            "fail_mobility": int(np.sum(statuses == "FAILED_DUE_TO_DEVICE_MOBILITY")),
            "fail_death": int(np.sum(statuses == "FAILED_DUE_TO_DEVICE_DEATH")),
            "fail_dependency": int(np.sum(statuses == "FAILED_BECAUSE_DEPENDENCY_FAILED")),
            "fail_resources": int(np.sum(statuses == "INSUFFICIENT_RESOURCES")),
            "fail_no_destination": int(np.sum(statuses == "NO_OFFLOADING_DESTINATIONS")),
            "task_overhead_mi_mean": float(np.mean([r["overhead_mi"] for r in records])),
            "task_overhead_kb_mean": float(np.mean([r["overhead_kb"] for r in records])),
            "task_poisoned_rate": float(np.mean([r.get("poisoned", 0) for r in records])),
            "task_fidelity_mean": float(np.mean([r.get("fidelity", 1.0) for r in records])),
            "task_compression_mean": float(np.mean([r.get("compression_ratio", 1.0) for r in records])),
        })

        # Orchestration timing.  Present only for instrumented orchestrators;
        # every quantity is defined only over tasks that actually began, so a
        # task killed before dispatch cannot masquerade as an instant decision.
        decision = np.array([r.get("decision_s", float("nan")) for r in records], dtype=float)
        dispatch = np.array([r.get("dispatch_s", float("nan")) for r in records], dtype=float)
        cost = np.array([r.get("decision_cost_ms", float("nan")) for r in records], dtype=float)
        arrival = np.array([r["arrival_s"] for r in records], dtype=float)
        start = np.array([r["exec_start_s"] for r in records], dtype=float)
        finite_cost = cost[np.isfinite(cost)]
        if finite_cost.size:
            out["decision_cost_mean_ms"] = float(finite_cost.mean())
            out["decision_cost_p95_ms"] = float(np.percentile(finite_cost, 95))
            out["decision_cost_total_s"] = float(finite_cost.sum() / 1000.0)
        began = (start > 0) & np.isfinite(decision)
        if np.any(began):
            out["release_to_decision_s"] = float(np.mean(decision[began] - arrival[began]))
            out["decision_to_dispatch_s"] = float(np.mean(dispatch[began] - decision[began]))
            out["dispatch_to_exec_s"] = float(np.mean(start[began] - dispatch[began]))
            out["orchestration_latency_s"] = float(np.mean(start[began] - arrival[began]))
            out["executed_share"] = float(np.mean(start > 0))
        by_stage: dict[str, list[dict]] = {}
        for r in records:
            stage = r.get("subtask") or ""
            if stage:
                by_stage.setdefault(stage, []).append(r)
        for stage, s_records in by_stage.items():
            out[f"stage_{stage}_delay_s"] = float(np.mean([r["total_delay_s"] for r in s_records]))
            out[f"stage_{stage}_cpu_s"] = float(np.mean([r["cpu_s"] for r in s_records]))
            out[f"stage_{stage}_wait_s"] = float(np.mean([r["wait_s"] for r in s_records]))
            out[f"stage_{stage}_network_s"] = float(np.mean([r["network_s"] for r in s_records]))
            out[f"stage_{stage}_success_rate"] = float(np.mean([r["success"] for r in s_records]))
            out[f"stage_{stage}_bytes_kb"] = float(np.mean([r["request_kb"] + r["result_kb"] for r in s_records]))
    successes = int(row.get(COL_SUCCESS, 0))
    energy = float(row.get(COL_ENERGY, 0.0))
    out["success_ratio"] = successes / generated if generated else float("nan")
    out["failure_ratio"] = 1.0 - out["success_ratio"] if generated else float("nan")
    out["energy_per_success_wh"] = energy / successes if successes else float("nan")
    out["success_per_wh"] = successes / energy if energy > 0 else float("nan")
    out["energy_per_task_wh"] = energy / generated if generated else float("nan")
    out["wan_usage_s"] = float(row.get(COL_WAN_USAGE, 0.0))
    out["network_usage_s"] = float(row.get(COL_NETWORK_USAGE, 0.0))
    out["traffic_mb"] = float(row.get(COL_TRAFFIC, 0.0))
    if requests:
        success_requests = sum(r["success"] for r in requests)
        makespans = [r["makespan_s"] for r in requests if r["success"] and not math.isnan(r["makespan_s"])]
        out.update({
            "request_count": len(requests),
            "request_success_ratio": success_requests / len(requests) if requests else float("nan"),
            "request_makespan_mean_s": float(np.mean(makespans)) if makespans else float("nan"),
            "request_makespan_p95_s": float(np.percentile(makespans, 95)) if makespans else float("nan"),
        })
    return out


# --------------------------------------------------------------------------------------
# Main entry point
# --------------------------------------------------------------------------------------
def run_once(*, base_settings: Path | str, out_dir: Path | str, architecture: str, algorithm: str,
             devices: int, duration_s: float, seed: int, orchestrator=None, task_generator=None,
             manager_class=None, settings_mutator: Callable[[Path], None] | None = None,
             applications_file: Path | str | None = None, properties: dict[str, Any] | None = None,
             injectors: Iterable[Callable[[Any, Any], Any]] | None = None,
             observers: Iterable[Callable[[Any, Any], Any]] | None = None,
             keep_tasks: bool = True, run_id: str = "run",
             metadata: dict[str, Any] | None = None) -> dict:
    """Run one scenario deterministically and return row + task statistics.

    Parameters mirror the knobs the five projects need.  ``injectors`` and
    ``observers`` are factories receiving ``(manager, engine)``; injectors act on
    the simulation, observers expose a ``summary()`` dict merged into the result.
    """
    ensure_pureedgesim()
    from pureedgesim import Engine, Randomness, Scenario, SimulationManager
    from pureedgesim.config import load_settings
    from pureedgesim.parameters import load_parameters
    from pureedgesim.runner import summarize

    started = time.perf_counter()
    base = Path(base_settings)
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    run_settings = out / "settings"
    prepare_settings(base, run_settings, properties=properties,
                     applications_file=applications_file, mutate=settings_mutator)

    parameters = load_parameters(run_settings / "simulation_parameters.properties")
    parameters.simulation_duration = float(duration_s)
    parameters.min_number_of_devices = int(devices)
    parameters.max_number_of_devices = int(devices)
    parameters.devices_increment = 1
    parameters.orchestration_architectures = [architecture]
    parameters.orchestration_algorithms = [algorithm]
    parameters.random_seed = int(seed)
    parameters.save_log_file = False
    parameters.save_charts = False
    parameters.display_real_time_charts = False
    parameters.auto_close_real_time_charts = True

    files = load_settings(run_settings)
    Randomness.set_seed(int(seed))
    Randomness.begin_scenario(0)
    engine = Engine(parameters.simulation_duration)
    manager = (manager_class or SimulationManager)(
        engine, parameters, files, Scenario(architecture, algorithm, int(devices)),
        custom_orchestrator=orchestrator, custom_task_generator=task_generator,
    )
    manager.start_simulation()
    active_injectors = [factory(manager, engine) for factory in (injectors or [])]
    active_observers = [factory(manager, engine) for factory in (observers or [])]
    engine.start()

    row = summarize(manager)
    records = task_records(manager)
    requests = request_records(manager, records)
    derived = derive_metrics(row, records, requests)
    result = {
        "run_id": run_id,
        "architecture": architecture,
        "algorithm": algorithm,
        "devices": int(devices),
        "duration_s": float(duration_s),
        "seed": int(seed),
        "wall_clock_s": round(time.perf_counter() - started, 4),
        **row,
        **derived,
    }
    for observer in active_observers:
        summary = observer.summary() if hasattr(observer, "summary") else {}
        prefix = getattr(observer, "prefix", observer.__class__.__name__.lower())
        for key, value in summary.items():
            result[f"{prefix}_{key}"] = value
    injections = {}
    for injector in active_injectors:
        kind = getattr(injector, "kind", injector.__class__.__name__.lower())
        injections[kind] = getattr(injector, "log", [])
    if injections:
        result["injections"] = injections
    if metadata:
        result["metadata"] = metadata

    if keep_tasks and records:
        arrays = {
            "delay": np.array([r["total_delay_s"] for r in records], dtype=np.float64),
            "cpu": np.array([r["cpu_s"] for r in records], dtype=np.float64),
            "wait": np.array([r["wait_s"] for r in records], dtype=np.float64),
            "network": np.array([r["network_s"] for r in records], dtype=np.float64),
            "length": np.array([r["length_mi"] for r in records], dtype=np.float64),
            "request_kb": np.array([r["request_kb"] for r in records], dtype=np.float64),
            "result_kb": np.array([r["result_kb"] for r in records], dtype=np.float64),
            "status": np.array([r["status"] for r in records]),
            "destination_type": np.array([r["destination_type"] for r in records]),
            "subtask": np.array([r["subtask"] for r in records]),
            "application_id": np.array([r["application_id"] for r in records], dtype=np.int32),
            "request_id": np.array([r["request_id"] for r in records], dtype=np.int64),
            "arrival": np.array([r["arrival_s"] for r in records], dtype=np.float64),
            "request_deadline": np.array([r["request_deadline_s"] for r in records], dtype=np.float64),
            "success": np.array([r["success"] for r in records], dtype=np.int8),
            "admitted": np.array([r["admitted"] for r in records], dtype=np.int8),
            "retry_count": np.array([r["retry_count"] for r in records], dtype=np.int16),
            "recovered": np.array([r["recovered"] for r in records], dtype=np.int8),
            "security_level": np.array([r["security_level"] for r in records], dtype=np.int8),
            "fidelity": np.array([r["fidelity"] for r in records], dtype=np.float64),
            "compression_ratio": np.array([r["compression_ratio"] for r in records], dtype=np.float64),
            "cert_feasible": np.array([r["cert_feasible"] for r in records], dtype=np.int8),
            "predicted_delay": np.array([r["predicted_delay_s"] for r in records], dtype=np.float64),
        }
        np.savez_compressed(out / "tasks.npz", **arrays)
        with (out / "tasks.csv").open("w", encoding="utf-8", newline="") as handle:
            import csv

            writer = csv.DictWriter(handle, fieldnames=list(records[0].keys()))
            writer.writeheader()
            writer.writerows(records)
    if requests:
        with (out / "requests.csv").open("w", encoding="utf-8", newline="") as handle:
            import csv

            writer = csv.DictWriter(handle, fieldnames=list(requests[0].keys()))
            writer.writeheader()
            writer.writerows(requests)
    (out / "row.json").write_text(json.dumps(result, indent=2, default=_json_default), encoding="utf-8")
    return result


def _json_default(value):
    if isinstance(value, (np.integer, np.floating)):
        return value.item()
    if isinstance(value, np.ndarray):
        return value.tolist()
    if isinstance(value, float) and (math.isnan(value) or math.isinf(value)):
        return None
    raise TypeError(f"Not JSON serialisable: {type(value)}")


def load_task_arrays(run_dir: Path | str) -> dict:
    with np.load(Path(run_dir) / "tasks.npz", allow_pickle=False) as data:
        return {key: data[key] for key in data.files}
