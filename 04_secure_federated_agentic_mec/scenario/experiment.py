"""P4 experiments: secure-by-design federated and agentic MEC.

Sweeps
------
* ``main``   -- security levels L0..L4 + ``adaptive`` across ``EDGE_AND_CLOUD`` and ``ALL``;
* ``attack`` -- security levels L0..L4 + ``adaptive`` across threat levels {0.0, 0.1, 0.2, 0.4};
* ``load``   -- arrival-rate factors {0.5, 1.0, 2.0, 4.0} across policies {L0, L2, L4, adaptive};
* ``size``   -- model-size multipliers {0.5, 1.0, 2.0, 4.0} across policies {L0, L2, L4, adaptive}.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from research_common.paths import ensure_pureedgesim  # noqa: E402
from research_common.runs import run_once, scale_application_rate  # noqa: E402
from research_common.sweep import default_workers, execute, expand_grid  # noqa: E402

sys.path.insert(0, str(Path(__file__).resolve().parent))
ensure_pureedgesim()

from model import (SECURITY_PROFILES, SecureManager, SecureTaskGenerator,  # noqa: E402
                   SecurityAwareOrchestrator, SecurityConfig, SecurityObserver)

PROJECT = "04_secure_federated_agentic_mec"
SETTINGS = Path(__file__).resolve().parent / "settings"
RESULTS = ROOT / PROJECT / "results"
APPLICATIONS = SETTINGS / "applications_baseline.xml"
POLICIES = ("level0", "level1", "level2", "level3", "level4", "adaptive")


def _config(job: dict) -> SecurityConfig:
    policy = str(job.get("policy", f"level{job.get('security_level', 2)}"))
    if policy == "adaptive":
        level = 2
    elif policy.startswith("level"):
        level = int(policy.replace("level", ""))
    else:
        level = int(job.get("security_level", 2))
    return SecurityConfig(
        security_level=level,
        policy=policy,
        threat_level=float(job.get("threat_level", 0.20)),
        model_size_factor=float(job.get("model_size_factor", 1.0)),
        rate_factor=float(job.get("rate_factor", 1.0)),
    )


def run_job(job: dict, out_dir: Path) -> dict:
    ensure_pureedgesim()
    config = _config(job)

    class BoundOrchestrator(SecurityAwareOrchestrator):
        def __init__(self, manager):
            super().__init__(manager, config)

    def task_generator(manager):
        return SecureTaskGenerator(manager, config)

    rate_factor = float(job.get("rate_factor", 1.0))
    mutator = (lambda d: scale_application_rate(d, rate_factor)) if rate_factor != 1.0 else None
    manager_class = SecureManager if config.policy == "adaptive" else None

    return run_once(
        base_settings=SETTINGS,
        out_dir=out_dir,
        architecture=job.get("architecture", "ALL"),
        algorithm=job.get("algorithm", "TRADE_OFF"),
        devices=35,
        duration_s=300.0,
        seed=int(job["seed"]),
        orchestrator=BoundOrchestrator,
        task_generator=task_generator,
        manager_class=manager_class,
        settings_mutator=mutator,
        applications_file=APPLICATIONS,
        properties={"simulation_time": 5, "save_log_file": "false", "save_charts": "false"},
        observers=[SecurityObserver],
        run_id=f"{job['experiment']}-{config.policy}-{job['seed']}",
        metadata={"policy": config.policy, "security_level": config.security_level,
                  "profile": SECURITY_PROFILES[config.security_level]["name"],
                  "config": config.__dict__},
    )


def grids() -> dict[str, list[dict]]:
    base = {
        "experiment": "main",
        "architecture": "ALL",
        "algorithm": "TRADE_OFF",
        "policy": "level2",
        "threat_level": 0.20,
        "model_size_factor": 1.0,
        "rate_factor": 1.0,
        "seed": 1,
    }
    seeds = tuple(range(1, 11))

    main = expand_grid(
        {**base, "experiment": "main"},
        {"policy": POLICIES, "architecture": ("EDGE_AND_CLOUD", "ALL"), "seed": seeds},
    )
    attack = expand_grid(
        {**base, "experiment": "attack"},
        {"policy": POLICIES, "threat_level": (0.0, 0.1, 0.2, 0.4),
         "architecture": ("ALL",), "seed": seeds},
    )
    load = expand_grid(
        {**base, "experiment": "load"},
        {"policy": ("level0", "level2", "level4", "adaptive"),
         "rate_factor": (0.5, 1.0, 2.0, 4.0), "architecture": ("ALL",), "seed": seeds},
    )
    size = expand_grid(
        {**base, "experiment": "size"},
        {"policy": ("level0", "level2", "level4", "adaptive"),
         "model_size_factor": (0.5, 1.0, 2.0, 4.0), "architecture": ("ALL",), "seed": seeds},
    )
    return {"main": main, "attack": attack, "load": load, "size": size}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="P4 secure MEC experiments")
    parser.add_argument("--workers", type=int, default=default_workers())
    parser.add_argument("--only", nargs="*", default=None)
    args = parser.parse_args(argv)
    all_grids = grids()
    selected = {name: all_grids[name] for name in (args.only or all_grids)}
    print(f"[P4] {sum(len(v) for v in selected.values())} runs across {len(selected)} grids")
    for name, jobs in selected.items():
        execute(jobs, f"{Path(__file__).resolve()}:run_job", RESULTS / "runs" / name,
                prefix=name, workers=args.workers,
                name_keys=("experiment", "policy", "threat_level", "rate_factor",
                           "model_size_factor", "architecture", "seed"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
