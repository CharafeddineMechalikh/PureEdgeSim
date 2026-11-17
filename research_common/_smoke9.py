import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    sys.path.insert(0, str(ROOT))
    from research_common.sweep import execute

    spec = importlib.util.spec_from_file_location(
        "p3exp", ROOT / "03_feasibility_certified_fault_tolerant_edge/scenario/experiment.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    jobs = [job for job in mod.grids()["outage"] if job["seed"] == 1]
    result = execute(jobs, f"{Path(mod.__file__).resolve()}:run_job",
                     ROOT / "03_feasibility_certified_fault_tolerant_edge/results/_outage",
                     prefix="outage", workers=4, name_keys=("policy", "outage"))
    frame = result.frame
    cols = ["policy", "outage", "success_ratio", "task_delay_mean_s", "fail_delay", "fail_death",
            "fail_resources", "faults_retries", "faults_recovered", "certificate_admission_rate"]
    print(frame[[c for c in cols if c in frame]].sort_values(["outage", "policy"]).to_string())


if __name__ == "__main__":
    main()
