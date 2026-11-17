import importlib.util
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    sys.path.insert(0, str(ROOT))
    from research_common.sweep import execute

    spec = importlib.util.spec_from_file_location(
        "p1bench", ROOT / "01_sim2deploy_sagin_uav_benchmark/scenario/benchmark.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)

    jobs = mod.grids()["scale"][:3]
    result = execute(jobs, f"{Path(mod.__file__).resolve()}:run_job",
                     ROOT / "01_sim2deploy_sagin_uav_benchmark/results/_smoke",
                     prefix="scale", workers=2, name_keys=("experiment", "architecture", "seed"))
    print(result.frame[["architecture", "seed", "success_ratio", "wall_clock_s"]].to_string())

    jobs = [job for job in mod.grids()["trace"] if job["workload"] == "trace"][:2]
    result = execute(jobs, f"{Path(mod.__file__).resolve()}:run_job",
                     ROOT / "01_sim2deploy_sagin_uav_benchmark/results/_smoke_trace",
                     prefix="trace", workers=2)
    print(result.frame[["workload", "success_ratio", "task_delay_p95_s"]].to_string())


if __name__ == "__main__":
    main()
