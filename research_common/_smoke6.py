import importlib.util
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    sys.path.insert(0, str(ROOT))
    from research_common.sweep import execute

    spec = importlib.util.spec_from_file_location(
        "p3exp", ROOT / "03_feasibility_certified_fault_tolerant_edge/scenario/experiment.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    jobs = [job for job in mod.grids()["failure"]
            if job["seed"] == 1 and job["failure_intensity"] == 10.0]
    result = execute(jobs, f"{Path(mod.__file__).resolve()}:run_job",
                     ROOT / "03_feasibility_certified_fault_tolerant_edge/results/_smoke",
                     prefix="failure", workers=2, name_keys=("policy",))
    cols = ["policy", "success_ratio", "task_delay_mean_s", "fail_delay", "fail_death",
            "faults_retries", "faults_recovered", "certificate_precision",
            "certificate_false_admission_rate"]
    print(result.frame[[c for c in cols if c in result.frame]].to_string())


if __name__ == "__main__":
    main()
