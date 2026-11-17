import importlib.util
import sys
from pathlib import Path

import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    sys.path.insert(0, str(ROOT))
    spec = importlib.util.spec_from_file_location(
        "p3exp", ROOT / "03_feasibility_certified_fault_tolerant_edge/scenario/experiment.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)

    out = ROOT / "03_feasibility_certified_fault_tolerant_edge/results/_calib"
    job = {"experiment": "margin", "policy": "shadow", "architecture": "EDGE_AND_CLOUD",
           "algorithm": "TRADE_OFF", "devices": 50, "seed": 1, "failure_intensity": 0.0,
           "safety_margin": 0.1, "rate_factor": 1.0, "burst": False, "max_retries": 2,
           "z": 1.645, "base_sigma": 0.12, "queue_sigma": 0.35, "mobility_sigma": 0.25}
    row = mod.run_job(job, out)
    print({k: row[k] for k in ("success_ratio", "certificate_precision", "certificate_recall",
                               "certificate_admission_rate", "certificate_false_admission_rate",
                               "certificate_false_rejection_rate", "certificate_mean_predicted_s",
                               "certificate_mean_margin_s") if k in row})
    frame = pd.read_csv(out / "tasks.csv")
    frame = frame[frame["predicted_delay_s"].notna() & np.isfinite(frame["predicted_delay_s"])]
    print("tasks with certificate:", len(frame))
    for app, group in frame.groupby("application_id"):
        print("\napp", app, "n", len(group),
              "feasible_rate", group["cert_feasible"].mean(),
              "success_rate", group["success"].mean(),
              "mean_pred", round(group["predicted_delay_s"].mean(), 4),
              "mean_actual", round(group["total_delay_s"].clip(upper=5).mean(), 4),
              "deadline_med", group["max_latency_s"].median())
        print("  pred p50/p90", np.round(np.percentile(group["predicted_delay_s"], [50, 90]), 4),
              "actual p50/p90", np.round(np.percentile(group["total_delay_s"], [50, 90]), 4))
    tp = int(((frame["cert_feasible"] == 1) & (frame["success"] == 1)).sum())
    fp = int(((frame["cert_feasible"] == 1) & (frame["success"] == 0)).sum())
    fn = int(((frame["cert_feasible"] == 0) & (frame["success"] == 1)).sum())
    tn = int(((frame["cert_feasible"] == 0) & (frame["success"] == 0)).sum())
    print("\nconfusion tp/fp/fn/tn", tp, fp, fn, tn,
          "precision", tp / max(1, tp + fp), "recall", tp / max(1, tp + fn))


if __name__ == "__main__":
    main()
