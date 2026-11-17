"""P5 experiments: cross-layer ISCC-MEC co-design for 6G edge networks.

Sweeps
------
* ``main``        -- ``agnostic`` vs ``iscc_aware`` across ``MIST_AND_EDGE``,
                     ``EDGE_AND_CLOUD``, and ``ALL`` (10 seeds);
* ``compression`` -- semantic compression ratios {0.20, 0.35, 0.50, 1.00} x policy (10 seeds);
* ``bandwidth``   -- PHY bandwidth factors {0.5, 1.0, 2.0} x antenna gains {1.0, 1.5, 2.5}
                     x compression ratios {0.25, 0.50, 1.00} (5 seeds);
* ``depth``       -- DAG pipeline depths {2, 4, 6} stages x policy x architecture (10 seeds);
* ``fidelity``    -- inference fidelity {0.6, 0.8, 1.0} x compression {0.35, 1.0} x policy (10 seeds).
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

from model import (ISCCAwareOrchestrator, ISCCConfig, ISCCManager,  # noqa: E402
                   ISCCTaskGenerator, ISCCObserver, apply_phy_parameters,
                   write_pipeline_depth_xml)

PROJECT = "05_crosslayer_iscc_mec_6g"
SETTINGS = Path(__file__).resolve().parent / "settings"
RESULTS = ROOT / PROJECT / "results"
APPLICATIONS = SETTINGS / "applications.xml"


def _config(job: dict) -> ISCCConfig:
    return ISCCConfig(
        policy=str(job.get("policy", "iscc_aware")),
        compression_ratio=float(job.get("compression_ratio", 0.50)),
        fidelity=float(job.get("fidelity", 1.00)),
        bandwidth_factor=float(job.get("bandwidth_factor", 1.00)),
        antenna_gain=float(job.get("antenna_gain", 1.50)),
        pipeline_depth=int(job.get("pipeline_depth", 4)),
        rate_factor=float(job.get("rate_factor", 1.00)),
    )


def run_job(job: dict, out_dir: Path) -> dict:
    ensure_pureedgesim()
    config = _config(job)

    def mutator(settings_dir: Path) -> None:
        if int(config.pipeline_depth) != 4:
            write_pipeline_depth_xml(settings_dir, int(config.pipeline_depth))
        if float(config.rate_factor) != 1.0:
            scale_application_rate(settings_dir, float(config.rate_factor))
        if float(config.bandwidth_factor) != 1.0 or float(config.antenna_gain) != 1.0:
            apply_phy_parameters(settings_dir, float(config.bandwidth_factor),
                                 float(config.antenna_gain))

    class BoundOrchestrator(ISCCAwareOrchestrator):
        def __init__(self, manager):
            super().__init__(manager, config)

    def task_generator(manager):
        return ISCCTaskGenerator(manager, config)

    manager_class = ISCCManager if config.policy == "iscc_aware" else None

    return run_once(
        base_settings=SETTINGS,
        out_dir=out_dir,
        architecture=job.get("architecture", "ALL"),
        algorithm=job.get("algorithm", "TRADE_OFF"),
        devices=30,
        duration_s=300.0,
        seed=int(job["seed"]),
        orchestrator=BoundOrchestrator,
        task_generator=task_generator,
        manager_class=manager_class,
        settings_mutator=mutator,
        applications_file=APPLICATIONS,
        properties={
            "simulation_time": 5,
            "realistic_network_model": "true",
            "one_shared_wan_network": "true",
            "save_log_file": "false",
            "save_charts": "false",
            # The ISCC request deadline is 0.8 s. PureEdgeSim's default update
            # interval of 1 s quantises every network hop to >= 1 s, which makes
            # *any* multi-stage pipeline structurally infeasible regardless of
            # placement policy. A 50 ms control interval keeps per-hop
            # quantisation an order of magnitude below the deadline while
            # remaining far above the compute times of individual stages.
            "update_interval": 0.05,
            "network_update_interval": 0.05,
        },
        observers=[ISCCObserver],
        run_id=f"{job['experiment']}-{config.policy}-{job['seed']}",
        metadata={"policy": config.policy, "config": config.__dict__},
    )


def grids() -> dict[str, list[dict]]:
    base = {
        "experiment": "main",
        "architecture": "ALL",
        "algorithm": "TRADE_OFF",
        "policy": "iscc_aware",
        "compression_ratio": 0.50,
        "fidelity": 1.00,
        "bandwidth_factor": 1.00,
        "antenna_gain": 1.50,
        "pipeline_depth": 4,
        "rate_factor": 1.50,
        "seed": 1,
    }
    seeds = tuple(range(1, 11))
    architectures = ("MIST_AND_EDGE", "EDGE_AND_CLOUD", "ALL")

    main = expand_grid(
        {**base, "experiment": "main"},
        {"policy": ("agnostic", "iscc_aware"), "architecture": architectures, "seed": seeds},
    )
    compression = expand_grid(
        {**base, "experiment": "compression"},
        {"policy": ("agnostic", "iscc_aware"),
         "compression_ratio": (0.20, 0.35, 0.50, 1.00),
         "architecture": ("ALL",), "seed": seeds},
    )
    bandwidth = expand_grid(
        {**base, "experiment": "bandwidth"},
        {"policy": ("agnostic", "iscc_aware"),
         "bandwidth_factor": (0.5, 1.0, 2.0),
         "compression_ratio": (0.25, 0.50, 1.00),
         "antenna_gain": (1.0, 2.0),
         "architecture": ("ALL",), "seed": tuple(range(1, 6))},
    )
    depth = expand_grid(
        {**base, "experiment": "depth"},
        {"policy": ("agnostic", "iscc_aware"),
         "pipeline_depth": (2, 4, 6),
         "architecture": ("EDGE_AND_CLOUD", "ALL"), "seed": seeds},
    )
    fidelity = expand_grid(
        {**base, "experiment": "fidelity"},
        {"policy": ("agnostic", "iscc_aware"),
         "fidelity": (0.6, 0.8, 1.0),
         "compression_ratio": (0.35, 1.00),
         "architecture": ("ALL",), "seed": tuple(range(1, 6))},
    )
    return {
        "main": main,
        "compression": compression,
        "bandwidth": bandwidth,
        "depth": depth,
        "fidelity": fidelity,
    }


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="P5 ISCC-MEC experiments")
    parser.add_argument("--workers", type=int, default=default_workers())
    parser.add_argument("--only", nargs="*", default=None)
    args = parser.parse_args(argv)
    all_grids = grids()
    selected = {name: all_grids[name] for name in (args.only or all_grids)}
    print(f"[P5] {sum(len(v) for v in selected.values())} runs across {len(selected)} grids")
    for name, jobs in selected.items():
        execute(jobs, f"{Path(__file__).resolve()}:run_job", RESULTS / "runs" / name,
                prefix=name, workers=args.workers,
                name_keys=("experiment", "policy", "architecture", "compression_ratio",
                           "fidelity", "bandwidth_factor", "antenna_gain", "pipeline_depth", "seed"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
