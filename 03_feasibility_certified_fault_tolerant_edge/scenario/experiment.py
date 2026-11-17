"""P3 experiments: feasibility-certified and fault-tolerant edge orchestration.

Policies
--------
``naive``          default placement, no admission control, no recovery;
``ft``             default placement + retries after failures;
``certified``      feasibility-certified admission control (no retries);
``certified_ft``   certificate + retries.

Sweeps
------
* failure intensity (0, 5, 10, 20% of devices affected by kills, link drops, drains);
* certificate safety margin (shadow mode, for precision/recall);
* non-stationary burst workload;
* arrival-rate (infeasibility) sweep.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from research_common.paths import ensure_pureedgesim  # noqa: E402
from research_common.runs import (BatteryDrainSchedule, KillSchedule, LinkDropSchedule,  # noqa: E402
                                  OutageSchedule, run_once, scale_application_rate)
from research_common.sweep import default_workers, execute, expand_grid  # noqa: E402

sys.path.insert(0, str(Path(__file__).resolve().parent))
ensure_pureedgesim()
from model import (BurstTaskGenerator, CertificateConfig, CertificateObserver,  # noqa: E402
                   CertifiedFaultTolerantManager, CertifiedManager, FaultToleranceObserver,
                   FaultTolerantManager, FeasibilityCertifiedOrchestrator, RetryAwareOrchestrator)

PROJECT = "03_feasibility_certified_fault_tolerant_edge"
SETTINGS = Path(__file__).resolve().parent / "settings"
RESULTS = ROOT / PROJECT / "results"


def _config(job: dict) -> CertificateConfig:
    return CertificateConfig(
        z=float(job.get("z", 1.282)),
        base_sigma=float(job.get("base_sigma", 0.08)),
        queue_sigma=float(job.get("queue_sigma", 0.20)),
        mobility_sigma=float(job.get("mobility_sigma", 0.10)),
        safety_margin=float(job.get("safety_margin", 0.02)),
        mode=str(job.get("mode", "enforce")),
    )


def _injectors(job: dict, devices: int):
    intensity = float(job.get("failure_intensity", 0.0)) / 100.0
    factories = []
    if job.get("outage"):
        share = max(1, round(devices / 50))
        factories.append(lambda manager, engine: OutageSchedule(
            manager, engine, [(120.0, 90.0, "edge_datacenters", share)]))
    if intensity <= 0:
        return factories
    kill_count = round(0.5 * intensity * devices)
    drop_count = round(0.3 * intensity * devices)
    drain_count = round(0.2 * intensity * devices)
    if kill_count:
        factories.append(lambda manager, engine: KillSchedule(
            manager, engine, [(90.0, "highest_load", kill_count),
                              (150.0, "highest_load", kill_count),
                              (210.0, "highest_load", kill_count),
                              (270.0, "highest_load", kill_count)]))
    if drop_count:
        factories.append(lambda manager, engine: LinkDropSchedule(
            manager, engine, [(90.0, 30.0, "highest_load", drop_count),
                              (180.0, 30.0, "spread", max(1, drop_count // 2))]))
    if drain_count:
        factories.append(lambda manager, engine: BatteryDrainSchedule(
            manager, engine, [(60.0, "random", drain_count, 0.4)]))
    return factories


def run_job(job: dict, out_dir: Path) -> dict:
    ensure_pureedgesim()
    policy = job["policy"]
    config = _config(job)

    def mutator(settings_dir: Path) -> None:
        if float(job.get("rate_factor", 1.0)) != 1.0:
            scale_application_rate(settings_dir, float(job["rate_factor"]))

    orchestrator = None
    if policy in ("certified", "certified_ft", "shadow"):
        bound_config = CertificateConfig(**{**config.__dict__,
                                            "mode": "shadow" if policy == "shadow" else "enforce"})
        class BoundOrchestrator(FeasibilityCertifiedOrchestrator):
            def __init__(self, manager):
                super().__init__(manager, bound_config)
        orchestrator = BoundOrchestrator

    manager_class = None
    if policy in ("ft",):
        class BoundManager(FaultTolerantManager):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, max_retries=int(job.get("max_retries", 2)), **kwargs)
        manager_class = BoundManager
        orchestrator = RetryAwareOrchestrator
    elif policy == "certified_ft":
        class BoundManager(CertifiedFaultTolerantManager):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, max_retries=int(job.get("max_retries", 2)), **kwargs)
        manager_class = BoundManager
    elif policy in ("certified", "shadow"):
        manager_class = CertifiedManager

    task_generator = None
    if job.get("burst"):
        def task_generator(manager):  # noqa: F811
            return BurstTaskGenerator(manager, burst_start=60.0, burst_end=120.0, factor=3)

    devices = int(job.get("devices", 50))
    return run_once(
        base_settings=SETTINGS,
        out_dir=out_dir,
        architecture=job.get("architecture", "EDGE_AND_CLOUD"),
        algorithm=job.get("algorithm", "TRADE_OFF"),
        devices=devices,
        duration_s=360.0,
        seed=int(job["seed"]),
        orchestrator=orchestrator,
        task_generator=task_generator,
        manager_class=manager_class,
        properties={"simulation_time": 6, "save_log_file": "false", "save_charts": "false"},
        settings_mutator=mutator,
        injectors=_injectors(job, devices),
        observers=[CertificateObserver, FaultToleranceObserver],
        run_id=f"{job['experiment']}-{policy}-{job['seed']}",
        metadata={"policy": policy, "config": config.__dict__},
    )


def grids() -> dict[str, list[dict]]:
    base = {
        "architecture": "ALL", "algorithm": "TRADE_OFF", "devices": 50, "seed": 1,
        "failure_intensity": 0.0, "safety_margin": 0.02, "rate_factor": 1.0, "burst": False,
        "outage": False, "max_retries": 2, "z": 1.282, "base_sigma": 0.08, "queue_sigma": 0.20,
        "mobility_sigma": 0.10,
    }
    seeds = tuple(range(1, 11))

    failure = expand_grid(
        {**base, "experiment": "failure"},
        {"policy": ("naive", "ft", "certified", "certified_ft"),
         "failure_intensity": (0.0, 5.0, 10.0, 20.0), "seed": seeds},
    )
    margin = expand_grid(
        {**base, "experiment": "margin"},
        {"policy": ("shadow",), "safety_margin": (0.0, 0.02, 0.1, 0.2), "z": (0.0, 1.282, 1.645, 1.96), "seed": seeds},
    )
    burst = expand_grid(
        {**base, "experiment": "burst"},
        {"policy": ("naive", "ft", "certified_ft"), "burst": (False, True), "seed": seeds},
    )
    rate = expand_grid(
        {**base, "experiment": "rate"},
        {"policy": ("naive", "certified", "certified_ft"), "rate_factor": (0.5, 1.0, 2.0, 4.0),
         "failure_intensity": (10.0,), "seed": seeds},
    )
    outage = expand_grid(
        {**base, "experiment": "outage"},
        {"policy": ("naive", "ft", "certified", "certified_ft"), "outage": (False, True),
         "seed": seeds},
    )
    return {"failure": failure, "margin": margin, "burst": burst, "rate": rate, "outage": outage}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="P3 certified fault tolerance experiments")
    parser.add_argument("--workers", type=int, default=default_workers())
    parser.add_argument("--only", nargs="*", default=None)
    args = parser.parse_args(argv)
    all_grids = grids()
    selected = {name: all_grids[name] for name in (args.only or all_grids)}
    print(f"[P3] {sum(len(v) for v in selected.values())} runs across {len(selected)} grids")
    for name, jobs in selected.items():
        execute(jobs, f"{Path(__file__).resolve()}:run_job", RESULTS / "runs" / name,
                prefix=name, workers=args.workers,
                name_keys=("experiment", "policy", "failure_intensity", "safety_margin", "z",
                           "rate_factor", "burst", "outage", "seed"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
