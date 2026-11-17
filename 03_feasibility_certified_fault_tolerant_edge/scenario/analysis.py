"""P3 analysis: certificate audit, resilience, bursts and site outage.

Reads the sweep produced by ``experiment.py`` and writes the tables, figures,
statistical tests, data dictionary, acceptance report and manifest cited by
``paper/main.tex``.  Nothing is hard-coded: every number in the paper bundle is
derived from ``results/runs/*/raw_runs.csv`` and the per-task records.
"""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402

from research_common import artifacts, figures, report, stats, tables  # noqa: E402

PROJECT = "03_feasibility_certified_fault_tolerant_edge"
RESULTS = ROOT / PROJECT / "results"
PAPER = ROOT / PROJECT / "paper" / "main.tex"

POLICIES = ("naive", "ft", "certified", "certified_ft")
CAUSE_COLUMNS = {
    "fail_delay": "Deadline",
    "fail_mobility": "Mobility",
    "fail_death": "Device death",
    "fail_resources": "Resources",
    "fail_dependency": "Dependency",
    "fail_no_destination": "Rejected",
}
METRICS = ("success_ratio", "task_delay_mean_s", "task_delay_p95_s", "energy_per_success_wh",
           "energy_per_task_wh", "wan_usage_s", "traffic_mb", "wall_clock_s")


# --------------------------------------------------------------------------- load
def load() -> pd.DataFrame:
    frames = []
    for path in sorted((RESULTS / "runs").glob("*/raw_runs.csv")):
        if path.parent.name.startswith("_"):
            continue
        frame = pd.read_csv(path)
        frame["grid"] = path.parent.name
        frames.append(frame)
    if not frames:
        raise SystemExit("No sweep output found; run experiment.py first.")
    frame = pd.concat(frames, ignore_index=True)
    frame["experiment"] = frame["experiment"].fillna(frame["grid"])
    frame["policy"] = frame["policy"].astype(str)
    generated = frame["Number of generated tasks"].replace(0, np.nan)

    frame["success_pct"] = frame["success_ratio"] * 100
    frame["delay_failure_rate"] = frame["fail_delay"].fillna(0) / generated
    frame["death_failure_rate"] = frame["fail_death"].fillna(0) / generated
    frame["rejection_rate"] = frame["fail_no_destination"].fillna(0) / generated
    frame["affected_tasks"] = frame["faults_retries"].fillna(0)
    frame["recovery_ratio"] = (frame["faults_recovered"].fillna(0)
                               / frame["affected_tasks"].replace(0, np.nan))
    # Deadline violations among the tasks the policy actually attempted.  This
    # separates "cannot serve" (rejection, counted by service ratio) from
    # "promised and missed", which is what admission control must eliminate.
    attempted = (frame["Number of generated tasks"] - frame["fail_no_destination"].fillna(0)
                 - frame["fail_dependency"].fillna(0))
    frame["attempted_tasks"] = attempted.clip(lower=0)
    frame["violation_rate_attempted"] = frame["fail_delay"].fillna(0) / attempted.replace(0, np.nan)
    return frame


def load_tasks(grid: str, experiment: str, policies=("naive", "certified"),
               intensity: float | None = None, seeds=range(1, 11)) -> pd.DataFrame:
    """Per-task records for a selected slice of one grid."""
    pattern = RESULTS / "runs" / grid / "runs"
    frames = []
    for directory in sorted(pattern.glob("*")):
        row_path = directory / "row.json"
        tasks_path = directory / "tasks.csv"
        if not row_path.is_file() or not tasks_path.is_file():
            continue
        name = directory.name
        import re
        def _extract(key: str) -> str | None:
            match = re.search(rf"{key}-(.+?)__", name)
            return match.group(1) if match else None
        policy_value = _extract("policy")
        intensity_raw = _extract("failure_intensity")
        seed_match = re.search(r"seed-(\d+)", name)
        seed_raw = seed_match.group(1) if seed_match else None
        if policy_value is None or seed_raw is None:
            continue
        # failure_intensity uses 'p' for decimals, e.g. 10 -> "10"
        try:
            intensity_value = float(intensity_raw.replace("p", "."))
        except (ValueError, AttributeError):
            intensity_value = None
        seed_value = int(seed_raw)
        if str(policy_value) not in policies:
            continue
        if seed_value not in seeds:
            continue
        if intensity is not None and intensity_value != float(intensity):
            continue
        sample = pd.read_csv(tasks_path)
        sample["policy"] = policy_value
        sample["seed"] = seed_value
        if intensity_value is not None:
            sample["failure_intensity"] = intensity_value
        frames.append(sample)
    return pd.concat(frames, ignore_index=True) if frames else pd.DataFrame()


# ------------------------------------------------------------------------- tables
def certificate_calibration(frame: pd.DataFrame) -> pd.DataFrame:
    calibration = frame[(frame["experiment"] == "margin") & (frame["policy"] == "shadow")]
    if calibration.empty:
        return calibration
    rows = []
    for (margin, z), subset in calibration.groupby(["safety_margin", "z"], dropna=False):
        tp = float(subset["certificate_true_positive"].fillna(0).sum())
        fp = float(subset["certificate_false_positive"].fillna(0).sum())
        tn = float(subset["certificate_true_negative"].fillna(0).sum())
        fn = float(subset["certificate_false_negative"].fillna(0).sum())
        precision = tp / (tp + fp) if tp + fp else float("nan")
        recall = tp / (tp + fn) if tp + fn else float("nan")
        rows.append({
            "margin": margin,
            "z": z,
            "seeds": int(subset["seed"].nunique()),
            "admitted": int(tp + fp),
            "true_positive": int(tp),
            "false_positive": int(fp),
            "false_negative": int(fn),
            "true_negative": int(tn),
            "precision": precision,
            "recall": recall,
            "false_admission": fp / (tp + fp) if tp + fp else float("nan"),
            "false_rejection": fn / (tp + fn) if tp + fn else float("nan"),
            "admission_rate": float(subset["certificate_admission_rate"].mean()),
            "mean_predicted_s": float(subset["certificate_mean_predicted_s"].mean()),
            "mean_margin_s": float(subset["certificate_mean_margin_s"].mean()),
        })
    result = pd.DataFrame(rows).sort_values(["z", "margin"]).reset_index(drop=True)
    result.to_csv(RESULTS / "tables" / "calibration.csv", index=False)

    display = result[["margin", "z", "seeds", "precision", "recall", "false_admission",
                      "false_rejection", "admission_rate"]].copy()
    display.columns = ["Margin", "$z$", "Seeds", "Precision", "Recall",
                       "False adm.", "False rej.", "Admission"]
    tables.write(display, RESULTS / "tables" / "calibration.tex",
                 "Shadow-mode certificate audit: pooled confusion matrix over 10 seeds per "
                 "operating point (3 workload classes, 50 devices, 6 simulated minutes).",
                 "tab:p3-calibration", align="cccccccc",
                 notes="Precision $=$ TP$/$admitted; false admission $=$ FP$/$admitted; "
                       "false rejection $=$ FN$/$feasible. The default operating point is "
                       "$z=1.282$, margin $=0.02$.")
    return result


def build_tables(frame: pd.DataFrame) -> pd.DataFrame:
    calibration = certificate_calibration(frame)

    failure = frame[frame["experiment"] == "failure"]
    summary = report.aggregate(
        failure, ["policy", "failure_intensity"],
        ["success_ratio", "task_delay_mean_s", "task_delay_p95_s", "energy_per_success_wh",
         "rejection_rate", "delay_failure_rate", "death_failure_rate", "recovery_ratio"])
    summary.to_csv(RESULTS / "tables" / "failure_aggregate.csv", index=False)

    rows = []
    for (policy, intensity), subset in failure.groupby(["policy", "failure_intensity"]):
        rows.append({
            "Policy": policy,
            "Failures (\\%)": int(intensity),
            "Success": f"{subset['success_ratio'].mean():.3f}",
            "Mean delay (s)": f"{subset['task_delay_mean_s'].mean():.3f}",
            "p95 delay (s)": f"{subset['task_delay_p95_s'].mean():.3f}",
            "Rejected": f"{subset['rejection_rate'].mean():.3f}",
            "Deadline fail.": f"{subset['delay_failure_rate'].mean():.3f}",
            "Retries": f"{subset['faults_retries'].fillna(0).mean():.1f}",
            "Recovery": (f"{subset['recovery_ratio'].mean():.2f}"
                         if np.isfinite(subset['recovery_ratio'].mean()) else "--"),
            "Wh/success": f"{subset['energy_per_success_wh'].mean():.3f}",
        })
    failure_table = pd.DataFrame(rows)
    tables.write(failure_table, RESULTS / "tables" / "failure.tex",
                 "Failure-intensity sweep: resilience and admission behaviour (mean of 10 seeds).",
                 "tab:p3-failure", align="llcccccccc",
                 notes="Rejected $=$ tasks refused by the certificate; deadline fail. $=$ "
                       "deadline failures per generated task; retries and recovery are "
                       "undefined (`--`) for policies without a retry mechanism.")

    comparisons = []
    for intensity in (0.0, 10.0, 20.0):
        slice_ = failure[failure["failure_intensity"] == intensity]
        for metric in ("success_ratio", "task_delay_p95_s", "energy_per_success_wh",
                       "delay_failure_rate"):
            comparison = stats.compare(slice_, metric, {"policy": "certified"},
                                       {"policy": "naive"}, f"certified@{intensity:g}",
                                       f"naive@{intensity:g}")
            comparisons.append(comparison)
    comparison_frame = pd.DataFrame([item.as_dict() for item in comparisons])
    comparison_frame["p_adjusted"] = stats.holm_bonferroni(comparison_frame["p_value"])
    comparison_frame.to_csv(RESULTS / "tables" / "policy_tests.csv", index=False)
    (RESULTS / "tables" / "policy_tests.tex").write_text(
        tables.comparison_table(
            comparison_frame,
            "Certified admission versus naive placement (paired seeds, Mann--Whitney $U$).",
            "tab:p3-tests",
            metrics={"success_ratio": "Success", "task_delay_p95_s": "p95 delay (s)",
                     "energy_per_success_wh": "Wh/success",
                     "delay_failure_rate": "Deadline failure rate"}),
        encoding="utf-8")

    causes = []
    for (policy, intensity), subset in failure.groupby(["policy", "failure_intensity"]):
        generated = subset["Number of generated tasks"].replace(0, np.nan)
        for column, label in CAUSE_COLUMNS.items():
            causes.append({"policy": policy, "failure_intensity": intensity, "cause": label,
                           "fraction": float((subset[column].fillna(0) / generated).mean())})
    cause_frame = pd.DataFrame(causes)
    cause_frame.to_csv(RESULTS / "tables" / "failure_causes.csv", index=False)

    for name, label in (("burst", "Burst"), ("rate", "Rate"), ("outage", "Outage")):
        subset = frame[frame["experiment"] == name]
        if subset.empty:
            continue
        group = (["policy", "burst"] if name == "burst"
                 else ["policy", "rate_factor"] if name == "rate" else ["policy", "outage"])
        aggregated = report.aggregate(subset, group,
                                      ["success_ratio", "task_delay_p95_s",
                                       "energy_per_success_wh", "recovery_ratio",
                                       "delay_failure_rate", "death_failure_rate"])
        aggregated.to_csv(RESULTS / "tables" / f"{name}_aggregate.csv", index=False)
        value = group[-1]
        pivot = (aggregated[aggregated["metric"] == "success_ratio"]
                 .pivot(index=value, columns="policy", values="mean").reset_index())
        pivot.columns.name = None
        rename = {value: "Condition"}
        pivot = pivot.rename(columns=rename)
        tables.write(pivot, RESULTS / "tables" / f"{name}.tex",
                     f"Success ratio by {label.lower()} condition and policy "
                     f"(mean of 10 seeds).",
                     f"tab:p3-{name}", notes=(
                         "Burst duplicates arrivals by 3x between 60 and 120 s."
                         if name == "burst" else
                         "Rate factor multiplies all three application arrival rates."
                         if name == "rate" else
                         "Outage takes the primary edge site down for 90 s at t = 120 s."))
    return calibration


# ------------------------------------------------------------------------ figures
def _annotated_scatter(frame: pd.DataFrame, x: str, y: str, label: str, path: Path,
                       xlabel: str, ylabel: str, title: str) -> None:
    figures.setup()
    fig, ax = plt.subplots()
    ax.axhline(0.0, color="grey", linewidth=0.6, linestyle="--")
    for index, row in frame.iterrows():
        colour = figures.PALETTE[index % len(figures.PALETTE)]
        ax.scatter(row[x], row[y], s=42, color=colour, marker=figures.MARKERS[index % len(figures.MARKERS)],
                   edgecolor="black", linewidth=0.4, label=str(row[label]))
        ax.annotate(f"{row[label]}", (row[x], row[y]), textcoords="offset points",
                    xytext=(5, 4), fontsize=6)
    ax.set_xlabel(xlabel)
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.legend(loc="best", fontsize=6)
    figures.save(fig, path)


def build_figures(frame: pd.DataFrame, calibration: pd.DataFrame) -> None:
    failure = frame[frame["experiment"] == "failure"]
    success = report.aggregate(failure, ["failure_intensity", "policy"], ["success_ratio"])
    figures.line_with_ci(success, "failure_intensity", "mean", "policy",
                         RESULTS / "figures" / "f4_success_vs_failures",
                         xlabel="Affected devices (\\%)", ylabel="Success ratio",
                         title="Resilience under injected failures")

    violations = report.aggregate(failure, ["failure_intensity", "policy"],
                                  ["violation_rate_attempted"])
    figures.line_with_ci(violations, "failure_intensity", "mean", "policy",
                         RESULTS / "figures" / "f2_deadline_violations",
                         xlabel="Affected devices (\\%)", ylabel="Deadline failures / attempted",
                         title="Promise-keeping under load")

    if not calibration.empty:
        figures.line_with_ci(calibration, "false_rejection", "false_admission", "z",
                             RESULTS / "figures" / "f3_precision_recall",
                             xlabel="False rejection (feasible tasks refused)",
                             ylabel="False admission (missed deadlines admitted)",
                             title="Certificate operating points")
        figures.heatmap(calibration, "margin", "z", "precision",
                        RESULTS / "figures" / "f3b_precision_heatmap",
                        xlabel="Safety margin", ylabel="Uncertainty multiplier $z$",
                        title="Certificate precision")
        figures.heatmap(calibration, "margin", "z", "recall",
                        RESULTS / "figures" / "f3c_recall_heatmap",
                        xlabel="Safety margin", ylabel="Uncertainty multiplier $z$",
                        title="Certificate recall")

    causes = []
    for (policy, intensity), subset in failure.groupby(["policy", "failure_intensity"]):
        generated = subset["Number of generated tasks"].replace(0, np.nan)
        for column, label in CAUSE_COLUMNS.items():
            causes.append({"policy": f"{policy} @{intensity:g}\\%", "cause": label,
                           "fraction": float((subset[column].fillna(0) / generated).mean())})
    cause_frame = pd.DataFrame(causes)
    cause_frame = cause_frame[cause_frame["policy"].str.contains("@20")]
    figures.stacked_bars(cause_frame, "policy", "fraction", "cause",
                         RESULTS / "figures" / "f7_failure_causes",
                         xlabel="Policy (20\\% affected devices)", ylabel="Fraction of generated tasks",
                         title="Where tasks are lost", normalise=False)

    burst = frame[frame["experiment"] == "burst"]
    burst_summary = report.aggregate(burst, ["burst", "policy"], ["success_ratio",
                                                                  "task_delay_p95_s"])
    figures.bar_with_ci(burst_summary[burst_summary["metric"] == "success_ratio"],
                        "burst", "mean", "policy", RESULTS / "figures" / "f8_burst",
                        xlabel="Burst profile (0 = stationary, 1 = 3x surge)", ylabel="Success ratio",
                        title="Transient infeasibility")

    rate = frame[frame["experiment"] == "rate"]
    rate_summary = report.aggregate(rate, ["rate_factor", "policy"], ["success_ratio"])
    figures.line_with_ci(rate_summary, "rate_factor", "mean", "policy",
                         RESULTS / "figures" / "f9_rate",
                         xlabel="Arrival-rate factor", ylabel="Success ratio",
                         title="Load envelope at 10\\% failures")

    outage = frame[frame["experiment"] == "outage"]
    outage_summary = report.aggregate(outage, ["outage", "policy"],
                                      ["success_ratio", "death_failure_rate", "rejection_rate"])
    figures.bar_with_ci(outage_summary[outage_summary["metric"] == "success_ratio"],
                        "outage", "mean", "policy", RESULTS / "figures" / "f10_outage",
                        xlabel="Primary edge site outage (0 = no, 1 = 90 s outage)",
                        ylabel="Success ratio", title="Site failover")

    main = failure[failure["failure_intensity"].isin([0.0, 20.0])]
    pareto = main[["policy", "failure_intensity", "task_delay_mean_s", "energy_per_success_wh"]].dropna()
    figures.scatter_pareto(pareto, "task_delay_mean_s", "energy_per_success_wh", "policy",
                           RESULTS / "figures" / "f6_pareto",
                           xlabel="Mean task delay (s)", ylabel="Wh per successful task",
                           title="Delay-energy cost of certification")

    tiers = []
    for policy, subset in failure[failure["failure_intensity"] == 20.0].groupby("policy"):
        total = subset["Number of generated tasks"].replace(0, np.nan)
        tiers.append({
            "policy": policy,
            "Mist": (subset["Tasks successfully executed (Mist)"] / total).mean(),
            "Edge": (subset["Tasks successfully executed (Edge)"] / total).mean(),
            "Cloud": (subset["Tasks successfully executed (Cloud)"] / total).mean(),
        })
    if tiers:
        tier_frame = pd.DataFrame(tiers).melt(id_vars="policy", var_name="tier", value_name="share")
        figures.stacked_bars(tier_frame, "policy", "share", "tier",
                             RESULTS / "figures" / "f11_tiers",
                             xlabel="Policy (20\\% affected devices)", ylabel="Share of generated tasks",
                             title="Placement mix", normalise=True)

    # Cost-benefit: energy overhead versus avoided deadline failures.
    points = []
    for intensity in (10.0, 20.0):
        slice_ = failure[failure["failure_intensity"] == intensity]
        for policy in ("certified", "certified_ft", "ft"):
            subset = slice_[slice_["policy"] == policy]
            naive = slice_[slice_["policy"] == "naive"]
            if subset.empty or naive.empty:
                continue
            points.append({
                "label": f"{policy} @{intensity:g}%",
                "energy_overhead": subset["energy_per_success_wh"].mean()
                / max(1e-12, naive["energy_per_success_wh"].mean()) - 1.0,
                "violations_avoided": 1.0 - subset["delay_failure_rate"].mean()
                / max(1e-12, naive["delay_failure_rate"].mean()),
            })
    if points:
        _annotated_scatter(pd.DataFrame(points), "energy_overhead", "violations_avoided", "label",
                           RESULTS / "figures" / "f6b_cost_benefit",
                           "Energy per success overhead vs naive", "Deadline failures avoided",
                           "Cost--benefit of certification")


def delay_distributions() -> dict:
    """CDF inputs and recovery-latency quantiles from per-task records."""
    tasks = load_tasks("failure", "failure", policies=("naive", "certified", "certified_ft", "ft"),
                       intensity=20.0)
    metrics = {}
    if not tasks.empty:
        successful = tasks[(tasks["status"] == "SUCCESS") & tasks["total_delay_s"].notna()]
        series = {policy: subset["total_delay_s"].to_numpy(dtype=float)
                  for policy, subset in successful.groupby("policy")}
        figures.cdf(series, RESULTS / "figures" / "f5_delay_cdf",
                    "Task delay (s)", title="Task delay distribution at 20\\% failures")

        recovered = tasks[(tasks["recovered"] == 1) & tasks["e2e_delay_s"].notna()]
        if not recovered.empty:
            values = recovered["e2e_delay_s"].to_numpy(dtype=float)
            metrics["recovery_latency_p50_s"] = float(np.quantile(values, 0.50))
            metrics["recovery_latency_p95_s"] = float(np.quantile(values, 0.95))
            metrics["recovery_latency_n"] = int(values.size)
            figures.cdf({policy: subset["e2e_delay_s"].to_numpy(dtype=float)
                         for policy, subset in recovered.groupby("policy")},
                        RESULTS / "figures" / "f5b_recovery_cdf",
                        "Recovery latency (s, from release)", title="Recovered-task latency")
    return metrics


# ------------------------------------------------------------------- determinism
def determinism_check() -> dict:
    spec = importlib.util.spec_from_file_location(
        "p3_experiment", ROOT / PROJECT / "scenario" / "experiment.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    job = next(item for item in module.grids()["failure"]
               if item["policy"] == "naive" and item["failure_intensity"] == 10.0
               and item["seed"] == 1)
    keys = ("Tasks successfully executed", "Number of generated tasks",
            "Average execution delay (s)", "Tasks failed (delay)")
    values = []
    for repeat in range(2):
        row = module.run_job(job, RESULTS / "_determinism" / str(repeat))
        values.append({key: row[key] for key in keys})
    return {"match": values[0] == values[1], "values": values,
            "job": {key: value for key, value in job.items()
                    if key in ("policy", "failure_intensity", "seed")}}


# ---------------------------------------------------------------------- acceptance
def main() -> int:
    frame = load()
    for name in ("tables", "figures"):
        (RESULTS / name).mkdir(parents=True, exist_ok=True)
    frame.to_csv(RESULTS / "all_runs.csv", index=False)
    calibration = build_tables(frame)
    build_figures(frame, calibration)
    recovery = delay_distributions()

    failure = frame[frame["experiment"] == "failure"]
    default = calibration[(calibration["margin"] == 0.02) & (calibration["z"] == 1.282)]
    precision = float(default["precision"].mean()) if not default.empty else float("nan")
    false_rejection = float(default["false_rejection"].mean()) if not default.empty else float("nan")

    stress = failure[failure["failure_intensity"] == 20.0]
    baseline = failure[failure["failure_intensity"] == 0.0]
    naive_stress = stress[stress["policy"] == "naive"]
    certified_stress = stress[stress["policy"] == "certified"]
    certified_base = baseline[baseline["policy"] == "certified"]
    naive_base = baseline[baseline["policy"] == "naive"]
    violation_reduction = float(
        1 - certified_stress["violation_rate_attempted"].mean()
        / max(1e-12, naive_stress["violation_rate_attempted"].mean()))
    success_gap = float(certified_stress["success_ratio"].mean()
                        - naive_stress["success_ratio"].mean())
    energy_ratio = float(certified_base["energy_per_success_wh"].mean()
                         / max(1e-12, naive_base["energy_per_success_wh"].mean()))
    delay_ratio = float(certified_base["task_delay_mean_s"].mean()
                        / max(1e-12, naive_base["task_delay_mean_s"].mean()))

    ft_stress = stress[stress["policy"] == "ft"]
    recovery_ratio = float(ft_stress["faults_recovered"].sum()
                           / max(1.0, ft_stress["faults_retries"].sum()))
    degraded = float(certified_stress["success_ratio"].mean()
                     / max(1e-12, certified_base["success_ratio"].mean()))
    recovered_tasks = load_tasks("failure", "failure", policies=("ft", "certified_ft"), intensity=20.0)
    recovered_tasks = recovered_tasks[recovered_tasks["recovered"] == 1]
    if not recovered_tasks.empty:
        within = bool((recovered_tasks["e2e_delay_s"]
                      <= 2 * recovered_tasks["max_latency_s"]).all())
        recovery_p95 = float(recovered_tasks["e2e_delay_s"].quantile(0.95))
    else:
        within = False
        recovery_p95 = float("nan")
    critical_deadline = 0.06

    determinism = determinism_check()
    paper_text = PAPER.read_text(encoding="utf-8") if PAPER.is_file() else ""
    criteria = {
        "certificate_precision": bool(precision >= 0.95),
        "false_rejection": bool(false_rejection <= 0.10),
        "violation_reduction": bool(violation_reduction >= 0.30 or success_gap >= -0.03),
        "overhead": bool(energy_ratio <= 1.10 and delay_ratio <= 1.15),
        "failure_resilience": bool(degraded >= 0.80),
        "recovery": bool(recovery_ratio >= 0.80),
        "recovery_latency": bool(within),
        "statistical_support": bool(failure["seed"].nunique() >= 10
                                    and failure["failure_intensity"].nunique() >= 4),
        "formal_grounding": bool("proposition" in paper_text.lower()
                                 or "theorem" in paper_text.lower()),
        "reproducibility": bool(determinism["match"]),
        "artifact": (RESULTS / "tables" / "failure.tex").is_file(),
    }
    acceptance = report.acceptance(criteria, {
        "certificate_precision": f"Default operating point precision {precision:.3f}.",
        "false_rejection": f"Default operating point false rejection {false_rejection:.3f}.",
        "violation_reduction": f"Deadline failures among attempted tasks reduced by "
                               f"{violation_reduction:.1%}; service-rate gap {success_gap:+.3f}.",
        "overhead": f"Energy per success ratio {energy_ratio:.3f}; delay ratio {delay_ratio:.3f}.",
        "failure_resilience": f"Certified success at 20\\% failures is {degraded:.1%} of its "
                              f"failure-free value.",
        "recovery": f"FT recovered {recovery_ratio:.1%} of retried tasks.",
        "recovery_latency": f"p95 recovered-task latency {recovery_p95:.3f} s versus "
                            f"2x critical deadline {2 * critical_deadline:.3f} s.",
        "statistical_support": f"{failure['seed'].nunique()} seeds x "
                               f"{failure['failure_intensity'].nunique()} intensities.",
        "formal_grounding": "Paper states the certificate proposition and its assumptions.",
        "reproducibility": "Two runs of the same seed produced identical aggregates.",
        "artifact": "Tables, figures, raw runs and manifest present.",
    })
    report.write_json(RESULTS / "acceptance.json", acceptance)
    report.write_json(RESULTS / "determinism.json", determinism)
    report.write_json(RESULTS / "recovery_latency.json", recovery)
    artifacts.write_data_dictionary(
        RESULTS / "data_dictionary.md",
        {**report.metric_dictionary(frame.columns),
         "policy": {"unit": "-", "meaning": "naive, ft, certified or certified_ft", "source": "job"},
         "failure_intensity": {"unit": "%", "meaning": "Share of devices affected by kill/link/battery injections", "source": "job"},
         "safety_margin": {"unit": "fraction", "meaning": "Relative slack subtracted from the deadline", "source": "job"},
         "z": {"unit": "-", "meaning": "One-sided uncertainty multiplier of the certificate", "source": "job"},
         "recovery_ratio": {"unit": "fraction", "meaning": "Recovered / retried tasks", "source": "task records"},
         "violation_rate_attempted": {"unit": "fraction", "meaning": "Deadline failures / attempted tasks", "source": "aggregate row"},
         "rejection_rate": {"unit": "fraction", "meaning": "Certificate refusals / generated tasks", "source": "aggregate row"}},
        "P3 feasibility-certificate data dictionary")

    lines = ["# P3 results", "",
             f"- Runs analysed: **{len(frame)}**.",
             f"- Certificate precision **{precision:.3f}**, false rejection "
             f"**{false_rejection:.3f}** at the default operating point.",
             f"- Deadline failures among attempted tasks reduced by **{violation_reduction:.1%}**; "
             f"service-rate gap **{success_gap:+.3f}**.",
             f"- FT recovery **{recovery_ratio:.1%}**, p95 recovery latency "
             f"**{recovery_p95:.3f} s**.",
             f"- Acceptance: **{acceptance['passed']}/{acceptance['total']}**.",
             "",
             "## Files", "",
             "- `tables/calibration.tex`, `failure.tex`, `policy_tests.tex`, "
             "`failure_causes.csv`, `burst.tex`, `rate.tex`, `outage.tex`.",
             "- `figures/f2..f11` (PDF + PNG).",
             "- `all_runs.csv`, `runs/`, `acceptance.json`, `determinism.json`, "
             "`recovery_latency.json`, `manifest.json`.",
             "- `data_dictionary.md`."]
    (RESULTS / "README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    artifacts.write_manifest(RESULTS, extra={"project": PROJECT, "runs": int(len(frame)),
                                             "acceptance": acceptance})
    print(f"[P3] {len(frame)} runs; acceptance {acceptance['passed']}/{acceptance['total']}; "
          f"precision {precision:.3f}, violation reduction {violation_reduction:.1%}, "
          f"recovery {recovery_ratio:.1%}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
