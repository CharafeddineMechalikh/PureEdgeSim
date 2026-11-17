"""P2 experiments: overhead-aware agentic edge intelligence.

Workload profiles
-----------------
``edge``     non-agentic edge workload (applications_baseline.xml);
``agentic``  LLM/RAG workload (applications_agentic.xml) whose *nominal* profile is
             the pilot configuration, with multipliers applied on top for the
             overhead sweep.

Policies
--------
``naive``       agentic profile, simulator default orchestrator, no ladder;
``naive_fid``   simulator default placement + the fidelity ladder;
``edge``        static edge-only placement + the ladder;
``edge_nofid``  static edge-only placement, no ladder;
``aware``       joint placement + the ladder (the full mechanism);
``aware_nofid`` joint placement, no ladder.

The last six are the complete 2 x 3 placement-by-fidelity factorial.  The ablation
arms below ``aware`` remove one *component* at a time (energy weight, container
penalty, cache-context awareness) and are defined in ``grids()``.

Grids
-----
``main`` / ``multiplier`` / ``rate`` / ``cache`` / ``fidelity`` / ``energy`` /
``ablation`` are the published grids (the first six unchanged in meaning;
``ablation`` is superseded by ``ablation2``, which ablates the cache mechanism
rather than the two scalars that happened to be zero).  The revision adds:

``factorial``       6 arms x 4 overhead levels x 20 seeds (items 1 and 10);
``cp_split``        compute x payload multipliers, 4 x 4 lattice (item 2);
``fidelity_gamma``  nonlinear fidelity cost C(f) (item 3);
``ablation2``       energy / container / cache-awareness ablation with a real
                    cache (cache_hit_ratio 0.5), 20 seeds (items 4 and 10);
``tiers``           all three tiers available, host-selection evidence (item 7);
``horizon``         5 / 15 / 30 simulated minutes with a 60 s warm-up (item 9).

Seeds 1-10 are the pre-specified reporting subset of the 20-seed grids; the extra
seeds exist for the power analysis and are flagged in the grid metadata.
"""
from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from research_common.paths import ensure_pureedgesim  # noqa: E402
from research_common.runs import run_once, scale_application_rate, set_device_battery  # noqa: E402
from research_common.sweep import default_workers, execute, expand_grid  # noqa: E402

sys.path.insert(0, str(Path(__file__).resolve().parent))
ensure_pureedgesim()
from model import (AgenticConfig, AgenticTaskGenerator, EdgeOnlyOrchestrator,  # noqa: E402
                   FidelityOnlyOrchestrator, OverheadAwareOrchestrator, OverheadObserver)

PROJECT = "02_overhead_aware_agentic_edge"
SETTINGS = Path(__file__).resolve().parent / "settings"
RESULTS = ROOT / PROJECT / "results"
# Every policy runs the *same* workload.  An earlier version gave the edge-only
# baseline a different application file, which confounded the comparison; the
# baseline must differ only in where it is allowed to place.
APPS = {
    "edge": SETTINGS / "applications_agentic.xml",
    "agentic": SETTINGS / "applications_agentic.xml",
}

# Arm -> (orchestrator class, ladder forced off?, energy weight, container penalty,
#         cache awareness).  ``None`` means "take the value from the job".
FACTORIAL_ARMS = ("naive", "naive_fid", "edge_nofid", "edge", "aware_nofid", "aware")
ABLATION2_ARMS = ("aware", "aware_noenergy", "aware_nocontainer", "aware_nocache", "aware_none")


def _config(job: dict) -> AgenticConfig:
    return AgenticConfig(
        compute_multiplier=float(job.get("compute_multiplier", 1.0)),
        payload_multiplier=float(job.get("payload_multiplier", 1.0)),
        control_messages=int(job.get("control_messages", 3)),
        cache_hit_ratio=float(job.get("cache_hit_ratio", 0.0)),
        fidelity_enabled=bool(job.get("fidelity", False)),
        fidelity_cost_gamma=float(job.get("fidelity_cost_gamma", 1.0)),
        fidelity_cost_min=float(job.get("fidelity_cost_min", 0.0)),
        fidelity_cost_max=float(job.get("fidelity_cost_max", 1.0)),
        energy_weight=float(job.get("energy_weight", 0.02)),
        deadline_margin=float(job.get("deadline_margin", 0.15)),
        container_penalty_s=float(job.get("container_penalty_s", 0.02)),
        cache_aware=bool(job.get("cache_aware", True)),
    )


def _overrides(policy: str) -> dict:
    """Per-arm component switches (the ablation and factorial definitions)."""
    if policy.endswith("_nofid"):
        return {"fidelity_enabled": False}
    if policy == "aware_noenergy":
        return {"energy_weight": 0.0}
    if policy == "aware_nocontainer":
        return {"container_penalty_s": 0.0}
    if policy == "aware_nocache":
        return {"cache_aware": False}
    if policy == "aware_none":
        return {"energy_weight": 0.0, "container_penalty_s": 0.0, "cache_aware": False}
    if policy == "aware_flat":          # the published (superseded) arm: lambda = beta = 0
        return {"energy_weight": 0.0, "container_penalty_s": 0.0}
    return {}


def _properties(job: dict) -> dict:
    duration_min = float(job.get("duration_min", 5))
    return {
        "simulation_time": duration_min,
        "realistic_network_model": "false",
        "save_log_file": "false",
        "save_charts": "false",
        "wan_bandwidth": 20,
        "wan_latency": 0.06,
        "wifi_bandwidth": 1300,
    }


def _bound(cls, config: AgenticConfig):
    """Bind a per-job config into the zero-argument factory the harness expects."""

    class BoundOrchestrator(cls):  # noqa: D101
        def __init__(self, manager):
            super().__init__(manager, config)

    return BoundOrchestrator


def run_job(job: dict, out_dir: Path) -> dict:
    ensure_pureedgesim()
    policy = job["policy"]
    profile = "agentic"
    config_values = _config(job).__dict__
    config_values.update(_overrides(policy))
    config = AgenticConfig(**config_values)

    def mutator(settings_dir: Path) -> None:
        if float(job.get("rate_factor", 1.0)) != 1.0:
            scale_application_rate(settings_dir, float(job["rate_factor"]))
        if job.get("battery_initial") is not None:
            set_device_battery(settings_dir, float(job["battery_initial"]))

    # All non-naive arms run the agentic overhead model; they differ only in the
    # orchestrator, so the comparison isolates the placement rule.
    task_generator = None
    if policy != "plain":
        def task_generator(manager):  # noqa: F811 - factory
            return AgenticTaskGenerator(manager, config)

    orchestrator = None
    if policy == "aware" or policy.startswith("aware_"):
        orchestrator = _bound(OverheadAwareOrchestrator, config)
    elif policy == "edge" or policy == "edge_nofid":
        orchestrator = _bound(EdgeOnlyOrchestrator, config)
    elif policy == "naive_fid":
        orchestrator = _bound(FidelityOnlyOrchestrator, config)

    result = run_once(
        base_settings=SETTINGS,
        out_dir=out_dir,
        architecture=job["architecture"],
        algorithm=job.get("algorithm", "TRADE_OFF"),
        devices=int(job.get("devices", 30)),
        duration_s=float(job.get("duration_min", 5)) * 60.0,
        seed=int(job["seed"]),
        orchestrator=orchestrator,
        task_generator=task_generator,
        applications_file=APPS[profile],
        properties=_properties(job),
        settings_mutator=mutator,
        observers=[OverheadObserver],
        run_id=f"{job['experiment']}-{policy}-{job['seed']}",
        metadata={"policy": policy, "profile": profile, "config": config.serialisable,
                  "warmup_s": float(job.get("warmup_s", 0.0))},
    )

    # Per-decision records: the fixed task-record schema of the shared harness
    # cannot carry the orchestrator's decision bookkeeping, so it is written
    # beside ``tasks.csv`` instead (one row per placement decision).
    if config.decision_log:
        target = Path(out_dir) / "decisions.csv"
        with target.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(config.decision_log[0].keys()))
            writer.writeheader()
            writer.writerows(config.decision_log)
    return result


def _with_payload(jobs: list[dict]) -> list[dict]:
    for job in jobs:
        job["payload_multiplier"] = job["compute_multiplier"]
    return jobs


def grids() -> dict[str, list[dict]]:
    base = {
        "architecture": "EDGE_AND_CLOUD", "algorithm": "TRADE_OFF", "devices": 30, "seed": 1,
        "compute_multiplier": 1.0, "payload_multiplier": 1.0, "control_messages": 3,
        "cache_hit_ratio": 0.0, "fidelity": True, "rate_factor": 1.0,
        "battery_initial": None, "energy_weight": 0.02, "deadline_margin": 0.15,
    }
    seeds = tuple(range(1, 11))
    seeds20 = tuple(range(1, 21))
    levels = (1.0, 1.5, 2.0, 3.0)

    main = expand_grid(
        {**base, "experiment": "main"},
        {"architecture": ("EDGE_AND_CLOUD", "ALL"), "policy": ("edge", "naive", "aware"),
         "seed": seeds},
    )
    multiplier = _with_payload(expand_grid(
        {**base, "experiment": "multiplier"},
        {"policy": ("naive", "edge", "aware"), "compute_multiplier": levels, "seed": seeds},
    ))
    rate = _with_payload(expand_grid(
        {**base, "experiment": "rate"},
        {"policy": ("naive", "aware"), "rate_factor": (0.5, 1.0, 2.0, 4.0),
         "compute_multiplier": levels, "seed": seeds},
    ))
    cache = _with_payload(expand_grid(
        {**base, "experiment": "cache"},
        {"policy": ("aware",), "cache_hit_ratio": (0.0, 0.5, 0.9), "fidelity": (False, True),
         "compute_multiplier": (1.0, 3.0), "seed": seeds},
    ))
    fidelity = _with_payload(expand_grid(
        {**base, "experiment": "fidelity"},
        {"policy": ("naive", "aware"), "fidelity": (False, True), "deadline_margin": (0.1, 0.3),
         "compute_multiplier": (1.0, 3.0), "seed": seeds},
    ))
    energy = expand_grid(
        {**base, "experiment": "energy"},
        {"policy": ("naive", "aware"), "battery_initial": (20.0, 50.0, 100.0), "seed": seeds},
    )
    ablation = _with_payload(expand_grid(
        {**base, "experiment": "ablation"},
        {"policy": ("edge", "naive_fid", "aware_nofid", "aware_flat", "aware"),
         "compute_multiplier": (1.5, 2.0), "seed": seeds},
    ))

    # ---- revision grids -------------------------------------------------------
    # 1 + 10. Complete placement x fidelity factorial, 20 seeds (seeds 1-10 are the
    # reporting subset used by the published-style tables).
    factorial = []
    for job in _with_payload(expand_grid(
            {**base, "experiment": "factorial"},
            {"policy": FACTORIAL_ARMS, "compute_multiplier": levels, "seed": seeds20})):
        job["fidelity"] = job["policy"] not in ("naive", "edge_nofid", "aware_nofid")
        factorial.append(job)

    # 2. Compute and payload overhead separated: each axis moved on its own.
    cp_split = []
    for job in expand_grid(
            {**base, "experiment": "cp_split"},
            {"policy": ("naive", "edge", "aware"), "compute_multiplier": levels,
             "payload_multiplier": levels, "seed": seeds}):
        job["fidelity"] = job["policy"] != "naive"
        cp_split.append(job)

    # 3. Nonlinear fidelity cost.  ``linear`` is the published C(f) = f; the
    # envelope arms use C(f) = 0.05 + 0.95 * f**gamma, i.e. a floor of 5 % of the
    # nominal work (the fixed cost of loading the model and emitting a token) and a
    # ceiling of the full effective work, so gamma only changes the *shape*.
    fidelity_gamma = []
    for model, (gamma, low, high) in {
            "linear": (1.0, 0.0, 1.0),
            "envelope0p5": (0.5, 0.05, 1.0),
            "envelope1": (1.0, 0.05, 1.0),
            "envelope2": (2.0, 0.05, 1.0)}.items():
        for job in _with_payload(expand_grid(
                {**base, "experiment": "fidelity_gamma"},
                {"policy": ("aware",), "cost_model": (model,),
                 "compute_multiplier": (1.5, 2.0, 3.0), "seed": seeds})):
            job.update({"fidelity": True, "fidelity_cost_gamma": gamma,
                        "fidelity_cost_min": low, "fidelity_cost_max": high})
            fidelity_gamma.append(job)

    # 4 + 10. Cache-awareness ablation, run where a cache actually exists: the
    # workload carries cache_hit_ratio 0.5, so the mechanism has information to
    # use.  Each arm's switches are also written into the job (provenance); the
    # policy name is what the orchestrator obeys, see ``_overrides``.
    arm_settings = {
        "aware": {},
        "aware_noenergy": {"energy_weight": 0.0},
        "aware_nocontainer": {"container_penalty_s": 0.0},
        "aware_nocache": {"cache_aware": False},
        "aware_none": {"energy_weight": 0.0, "container_penalty_s": 0.0, "cache_aware": False},
    }
    ablation2 = []
    for job in _with_payload(expand_grid(
            {**base, "experiment": "ablation2"},
            {"policy": ABLATION2_ARMS, "compute_multiplier": (1.5, 2.0, 3.0),
             "seed": seeds20})):
        job.update({"cache_hit_ratio": 0.5, "fidelity": True,
                    "container_penalty_s": 0.02, "cache_aware": True})
        job.update(arm_settings[job["policy"]])
        ablation2.append(job)

    # 7. All three tiers available, so host selection has a genuine choice.
    tiers = []
    for job in _with_payload(expand_grid(
            {**base, "experiment": "tiers", "architecture": "ALL"},
            {"policy": ("naive", "naive_fid", "edge", "aware"),
             "compute_multiplier": (1.0, 2.0, 3.0), "seed": seeds})):
        job["fidelity"] = job["policy"] not in ("naive",)
        tiers.append(job)

    # 9. Warm-up and horizon.  warmup_s is an analysis-side filter, not a run knob.
    horizon = []
    for job in _with_payload(expand_grid(
            {**base, "experiment": "horizon"},
            {"policy": ("naive", "edge", "aware"), "compute_multiplier": (1.0, 2.0),
             "duration_min": (5.0, 15.0, 30.0), "seed": seeds})):
        job["fidelity"] = job["policy"] != "naive"
        job["warmup_s"] = 60.0
        horizon.append(job)

    return {"main": main, "multiplier": multiplier, "rate": rate, "cache": cache,
            "fidelity": fidelity, "energy": energy, "ablation": ablation,
            "factorial": factorial, "cp_split": cp_split, "fidelity_gamma": fidelity_gamma,
            "ablation2": ablation2, "tiers": tiers, "horizon": horizon}


# The published grids keep their original folder naming (job ids and resume keys
# must not move); the revision grids use a compact naming because a Windows path
# component may not exceed 255 characters and the verbose form overflowed it.
PUBLISHED_KEYS = ("experiment", "policy", "architecture", "compute_multiplier", "rate_factor",
                  "cache_hit_ratio", "fidelity", "battery_initial", "deadline_margin", "seed")
COMPACT_KEYS = ("policy", "architecture", "compute_multiplier", "payload_multiplier",
                "cost_model", "duration_min", "rate_factor", "cache_hit_ratio", "seed")
NAME_KEYS = {
    # factorial already completed with the verbose naming; keep it so a re-run
    # resumes instead of duplicating every directory.
    "factorial": PUBLISHED_KEYS[:4] + ("payload_multiplier",) + PUBLISHED_KEYS[4:] +
                 ("container_penalty_s", "cache_aware"),
    "cp_split": COMPACT_KEYS,
    "fidelity_gamma": COMPACT_KEYS,
    "ablation2": COMPACT_KEYS,
    "tiers": COMPACT_KEYS,
    "horizon": COMPACT_KEYS,
}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="P2 agentic overhead experiments")
    parser.add_argument("--workers", type=int, default=default_workers())
    parser.add_argument("--only", nargs="*", default=None)
    parser.add_argument("--limit", type=int, default=None,
                        help="keep only the first N jobs per grid (smoke testing)")
    parser.add_argument("--seeds", type=int, default=None,
                        help="keep only the first N seeds of every selected grid")
    args = parser.parse_args(argv)
    all_grids = grids()
    selected = {name: list(all_grids[name]) for name in (args.only or all_grids)}
    if args.seeds:
        keep = set(range(1, args.seeds + 1))
        selected = {name: [job for job in jobs if int(job.get("seed", 1)) in keep]
                    for name, jobs in selected.items()}
    if args.limit:
        selected = {name: jobs[:args.limit] for name, jobs in selected.items()}
    print(f"[P2] {sum(len(v) for v in selected.values())} runs across {len(selected)} grids")
    for name, jobs in selected.items():
        execute(jobs, f"{Path(__file__).resolve()}:run_job", RESULTS / "runs" / name,
                prefix=name, workers=args.workers,
                name_keys=NAME_KEYS.get(name, PUBLISHED_KEYS))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
