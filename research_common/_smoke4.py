import importlib.util
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    sys.path.insert(0, str(ROOT))
    from research_common.sweep import execute

    spec = importlib.util.spec_from_file_location(
        "p2exp", ROOT / "02_overhead_aware_agentic_edge/scenario/experiment.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    jobs = [job for job in mod.grids()["main"] if job["seed"] == 1]
    result = execute(jobs, f"{Path(mod.__file__).resolve()}:run_job",
                     ROOT / "02_overhead_aware_agentic_edge/results/_smoke2",
                     prefix="main", workers=3, name_keys=("policy", "architecture"))
    cols = ["policy", "architecture", "success_ratio", "task_delay_mean_s",
            "energy_per_success_wh", "network_usage_s", "task_admission_rate"]
    cols = [c for c in cols if c in result.frame]
    print(result.frame[cols].to_string())
    for col in ("overhead_mean_fidelity", "overhead_low_fidelity_rate", "overhead_estimated_deadline_rate"):
        if col in result.frame:
            print(col, result.frame[col].tolist())


if __name__ == "__main__":
    main()
