"""P1 benchmark experiments: reproducible UAV/SAGIN edge-computing evaluation.

The module defines the benchmark contract as code:

- a fixed scenario matrix (architectures x algorithms x scale x seed),
- mobility variants from the simulator's validated mobility library,
- a production trace-replay workload derived from the Azure LLM code trace,
- link/energy sensitivity sweeps,
- and one job runner shared by every experiment.

Run from the repository root::

    python 01_sim2deploy_sagin_uav_benchmark/scenario/benchmark.py --workers 4

Results land in ``01_sim2deploy_sagin_uav_benchmark/results/``.
"""
from __future__ import annotations

import argparse
import copy
import csv
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(Path(__file__).resolve().parent))

from research_common.paths import ensure_pureedgesim  # noqa: E402
from research_common.runs import run_once  # noqa: E402
from research_common.sweep import default_workers, execute, expand_grid  # noqa: E402

PROJECT = "01_sim2deploy_sagin_uav_benchmark"
SETTINGS = Path(__file__).resolve().parent / "settings"
RESULTS = ROOT / PROJECT / "results"
TRACE = ROOT / "_deps_pureedgesim_python" / "examples" / "trace_replay" / "trace" / "azure_llm_code_1h.csv"

ARCHITECTURES = ("EDGE_ONLY", "EDGE_AND_CLOUD", "ALL")
ALGORITHMS = ("TRADE_OFF", "ROUND_ROBIN")
SEEDS = tuple(range(1, 11))
MOBILITY_MODELS = ("random-waypoint", "gauss-markov", "waypoint", "group")
ALGORITHM_NAMES = ("trade_off", "round_robin", "random", "minmin",
                   "leastloaded", "mct", "qos")


MIX_PROFILES = {
    "m1": (0.80, 0.10, 0.10),
    "m2": (0.60, 0.20, 0.20),
    "m3": (0.35, 0.35, 0.30),
    "m4": (0.20, 0.40, 0.40),
    "m5": (0.10, 0.20, 0.70),
}


def scale_deadlines(settings_dir: Path, factor: float) -> None:
    """Multiply every application latency by ``factor`` (deadline sweep)."""
    path = settings_dir / "applications.xml"
    tree = ET.parse(path)
    for application in tree.getroot().findall("application"):
        latency = application.find("latency")
        if latency is not None and latency.text:
            latency.text = f"{float(latency.text) * float(factor):.6g}"
    tree.write(path, encoding="utf-8", xml_declaration=True)


def set_mix(settings_dir: Path, weights: tuple[float, float, float]) -> None:
    """Set the device share of the three applications to the given mix."""
    path = settings_dir / "applications.xml"
    tree = ET.parse(path)
    applications = tree.getroot().findall("application")
    total = sum(weights)
    for application, weight in zip(applications, weights):
        share = application.find("usage_percentage")
        if share is not None:
            share.text = str(int(round(100.0 * weight / total)))
    tree.write(path, encoding="utf-8", xml_declaration=True)

def _core_properties(job: dict) -> dict:
    return {
        "simulation_time": 5,
        "realistic_network_model": "true",
        "network_update_interval": 1,
        "one_shared_wan_network": "true",
        "update_interval": float(job.get("update_interval", 1.0)),
        "network_update_interval": float(job.get("update_interval", 1.0)),
        "edge_devices_range": 80,
        "edge_datacenters_coverage": 350,
        "wan_bandwidth": 50,
        "wan_latency": job.get("wan_latency", 0.12),
        "wifi_bandwidth": job.get("wifi_bandwidth", 300),
        "save_log_file": "false",
        "save_charts": "false",
    }


class TraceTaskGenerator:
    """Replay the Azure LLM production trace as an edge workload.

    Each trace record is an LLM request: the context length drives prefill compute
    and uploaded bytes, the generated length drives decode compute and returned
    bytes.  The mapping is deliberately linear and documented in the paper; it is
    a *workload replay*, not a claim about the exact serving cost.
    """

    def __init__(self, manager, trace_path: Path = TRACE, scale: float = 1.0,
                 mode: str = "native", target_requests: int = 1990) -> None:
        """``mode`` selects which confound is being controlled.

        ``native``
            take every trace record that arrives inside the horizon.  This
            changes the arrival process, the deadline distribution, the class
            mix and the offered load all at once.
        ``rate_matched``
            take the first ``target_requests`` records and rescale their
            arrivals onto the horizon, so the offered load equals the synthetic
            arm and only the arrival *shape*, size distribution and deadline
            distribution differ.
        ``deadline_matched``
            keep the native arrivals but impose the synthetic per-class
            deadlines, isolating the deadline distribution from the arrivals.
        """
        from pureedgesim.tasks import DefaultTaskGenerator

        self._base = DefaultTaskGenerator(manager)
        self.manager = manager
        self.parameters = manager.parameters
        self.random = manager.random
        self.trace_path = Path(trace_path)
        self.scale = float(scale)
        self.mode = str(mode)
        self.target_requests = int(target_requests)

    def generate(self):
        import copy

        from pureedgesim.tasks import Task

        applications = self.manager.applications
        devices = [node for node in self.manager.edge_devices
                   if node.generates_tasks and node.is_alive()]
        if not applications or not devices:
            return []
        duration = self.parameters.simulation_duration
        rows = self._load_trace(duration)
        if self.mode == "rate_matched":
            rows = self._load_trace(float("inf"), limit=self.target_requests)
            if rows:
                last = rows[-1][0]
                stretch = (duration - 1.0) / last if last > 0 else 1.0
                rows = [(arrival * stretch, context, generated)
                        for arrival, context, generated in rows]
        if not rows:
            return self._base.generate()
        tasks = []
        identifier = 1
        for index, (arrival_s, context, generated) in enumerate(rows):
            application = applications[index % len(applications)]
            device = devices[index % len(devices)]
            task = Task(
                id=identifier,
                type=application.type,
                application_id=applications.index(application),
                length=500.0 + 2.0 * context + 8.0 * generated,
                container_size=application.container_size.mean(),
                request_size=(0.5 + 0.002 * context) * 8000.0,
                result_size=(0.1 + 0.004 * generated) * 8000.0,
                max_latency=(application.latency.sample()
                             if self.mode == "deadline_matched"
                             else max(0.5, 2.0 + 0.0005 * context)),
                time=float(arrival_s),
                source_device=device,
                registry=self.manager.registry,
            )
            task.set_request(index + 1, 1, float(arrival_s), float(arrival_s) + task.max_latency)
            task.research_trace = "azure_llm_code_1h"
            tasks.append(task)
            identifier += 1
        tasks.sort(key=lambda item: item.time)
        return tasks

    def _load_trace(self, duration: float,
                    limit: int | None = None) -> list[tuple[float, int, int]]:
        rows = []
        with self.trace_path.open("r", encoding="utf-8", newline="") as handle:
            reader = csv.DictReader(handle)
            for record in reader:
                arrival = float(record["arrival_ms"]) / 1000.0 * self.scale
                if arrival >= duration:
                    break
                rows.append((arrival, int(record["context_tokens"]), int(record["generated_tokens"])))
                if limit is not None and len(rows) >= limit:
                    break
        return rows


def sweep_name(job: dict) -> str:
    return str(job["experiment"])


def run_job(job: dict, out_dir: Path) -> dict:
    """Execute one benchmark configuration (called by the sweep workers)."""
    ensure_pureedgesim()
    seed = int(job["seed"])
    architecture = job["architecture"]
    algorithm = job.get("algorithm", "TRADE_OFF")
    devices = int(job["devices"])
    properties = _core_properties(job)
    if job.get("infra"):
        import heterogeneity

        properties.update(heterogeneity.profile_properties(job["infra"]))
    mutators = []
    if job.get("mobility"):
        from research_common.runs import set_device_mobility
        mutators.append(lambda settings_dir, model=job["mobility"]:
                        set_device_mobility(settings_dir, model))
    if job.get("deadline_factor") is not None:
        mutators.append(lambda settings_dir, f=job["deadline_factor"]:
                        scale_deadlines(settings_dir, float(f)))
    if job.get("mix"):
        mutators.append(lambda settings_dir, w=MIX_PROFILES[job["mix"]]:
                        set_mix(settings_dir, w))
    if job.get("infra"):
        import heterogeneity

        mutators.append(lambda settings_dir, name=job["infra"]:
                        heterogeneity.apply_profile(settings_dir, name))

    def mutator(settings_dir: Path) -> None:
        for function in mutators:
            function(settings_dir)

    if not mutators:
        mutator = None

    # A custom orchestrator (algorithm family) overrides the simulator's
    # built-in two rules; the scenario algorithm is kept at TRADE_OFF so that
    # the only difference between arms is the decision mechanism.
    orchestrator = None
    policy = job.get("policy")
    if policy:
        import algorithms as families

        orchestrator = families.ALGORITHMS[policy]

    task_generator = None
    if job.get("workload", "").startswith("trace"):
        mode = {"trace": "native", "trace_rate": "rate_matched",
                "trace_deadline": "deadline_matched"}[job["workload"]]

        def task_generator(manager, mode=mode):  # noqa: F811 - factory
            return TraceTaskGenerator(manager, TRACE, mode=mode)

    return run_once(
        base_settings=SETTINGS,
        out_dir=out_dir,
        architecture=architecture,
        algorithm=algorithm,
        devices=devices,
        duration_s=300.0,
        seed=seed,
        task_generator=task_generator,
        orchestrator=orchestrator,
        properties=properties,
        settings_mutator=mutator,
        run_id=f"{job['experiment']}-{seed}",
        metadata={"experiment": job["experiment"],
                  "mobility": job.get("mobility", "default"),
                  "infra": job.get("infra", "default")},
    )


def grids() -> dict[str, list[dict]]:
    base = {"architecture": "ALL", "algorithm": "TRADE_OFF", "devices": 40, "seed": 1,
            "mobility": "random-waypoint", "workload": "synthetic",
            "wan_latency": 0.12, "wifi_bandwidth": 300}

    scale = expand_grid(
        {**base, "experiment": "scale"},
        {"architecture": ARCHITECTURES, "algorithm": ALGORITHMS,
         "devices": (20, 40, 60, 80), "seed": SEEDS},
    )
    mobility = expand_grid(
        {**base, "experiment": "mobility", "workload": "synthetic"},
        {"mobility": MOBILITY_MODELS, "architecture": ARCHITECTURES, "seed": SEEDS},
    )
    # Four arms so that the confounds in the trace comparison can be told
    # apart instead of being acknowledged: arrivals only, arrivals with the
    # offered load matched, and arrivals with the deadline distribution matched.
    trace = expand_grid(
        {**base, "experiment": "trace"},
        {"workload": ("synthetic", "trace", "trace_rate", "trace_deadline"),
         "architecture": ARCHITECTURES, "seed": SEEDS},
    )
    # The control interval is the object of study, not a nuisance parameter:
    # at 1 s every infrastructure hand-off costs a full tick, which is longer
    # than the two tight deadlines in the workload.
    clock = expand_grid(
        {**base, "experiment": "clock", "devices": 40},
        {"architecture": ARCHITECTURES, "update_interval": (1.0, 0.2, 0.05),
         "seed": SEEDS},
    )
    # Algorithm x architecture x temporal configuration: the crossed design
    # that separates what an algorithm decides from what the simulator does.
    # Infrastructure heterogeneity: the condition under which an adaptive
    # orchestrator is supposed to earn its complexity.  Crossed with the
    # architecture set and the temporal configuration so that a null result
    # cannot be blamed on a missing factor.
    hetero = expand_grid(
        {**base, "experiment": "hetero", "devices": 40},
        {"infra": ("uniform", "edge_weak", "edge_strong", "split_task"),
         "architecture": ARCHITECTURES,
         "policy": ("trade_off", "round_robin", "random", "minmin", "leastloaded",
                    "mct", "qos"),
         "update_interval": (1.0, 0.05), "seed": SEEDS},
    )
    algo = expand_grid(
        {**base, "experiment": "algo", "devices": 40},
        {"policy": ALGORITHM_NAMES, "architecture": ARCHITECTURES,
         "update_interval": (1.0, 0.2, 0.05), "seed": SEEDS},
    )
    deadline = expand_grid(
        {**base, "experiment": "deadline", "devices": 40},
        {"architecture": ("EDGE_ONLY", "ALL"),
         "deadline_factor": (1.0, 1.5, 3.0, 8.0, 16.0),
         "seed": SEEDS},
    )
    mix = expand_grid(
        {**base, "experiment": "mix", "devices": 40},
        {"architecture": ("EDGE_ONLY", "ALL"), "mix": tuple(MIX_PROFILES), "seed": SEEDS},
    )
    sensitivity = expand_grid(
        {**base, "experiment": "sensitivity"},
        {"wan_latency": (0.03, 0.12, 0.30), "wifi_bandwidth": (50, 300, 1300),
         "architecture": ARCHITECTURES, "seed": tuple(range(1, 6))},
    )
    return {"scale": scale, "mobility": mobility, "trace": trace,
            "sensitivity": sensitivity, "clock": clock,
            "deadline": deadline, "mix": mix, "algo": algo, "hetero": hetero}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="P1 reproducible benchmark")
    parser.add_argument("--workers", type=int, default=default_workers())
    parser.add_argument("--only", nargs="*", default=None,
                        help="subset of grids: scale mobility trace sensitivity")
    parser.add_argument("--resume", action="store_true", default=True)
    args = parser.parse_args(argv)

    all_grids = grids()
    selected = {name: all_grids[name] for name in (args.only or all_grids)}
    total = sum(len(jobs) for jobs in selected.values())
    print(f"[P1] {total} runs across {len(selected)} grids, workers={args.workers}")
    for name, jobs in selected.items():
        execute(jobs, f"{Path(__file__).resolve()}:run_job",
                RESULTS / "runs" / name, prefix=name, workers=args.workers, resume=args.resume,
                name_keys=("experiment", "architecture", "algorithm", "devices", "seed",
                           "mobility", "workload", "wan_latency", "wifi_bandwidth",
                           "update_interval", "deadline_factor", "mix", "policy",
                           "infra"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
