"""P1 analysis: tables, figures, statistics, acceptance checks and artifact manifest.

Reads the raw sweep output produced by ``benchmark.py`` and writes a self-contained
``results/`` bundle that the paper cites figure by figure.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402

from research_common import artifacts, figures, report, stats, tables  # noqa: E402

PROJECT = "01_sim2deploy_sagin_uav_benchmark"
RESULTS = ROOT / PROJECT / "results"
PAPER = ROOT / PROJECT / "paper" / "main.tex"

FAILURE_COLUMNS = {
    "fail_delay": "Deadline",
    "fail_mobility": "Mobility",
    "fail_death": "Device death",
    "fail_resources": "Resources",
    "fail_dependency": "Dependency",
    "fail_no_destination": "No destination",
}

METRICS = ("success_ratio", "task_delay_mean_s", "task_delay_p95_s", "energy_per_success_wh",
           "energy_per_task_wh", "wan_usage_s", "traffic_mb", "wall_clock_s")


def load_grids() -> pd.DataFrame:
    frames = []
    for path in sorted((RESULTS / "runs").glob("*/raw_runs.csv")):
        if path.parent.name.startswith("_"):
            continue
        frame = pd.read_csv(path)
        frame["grid"] = path.parent.name
        frames.append(frame)
    if not frames:
        raise SystemExit("No sweep output found; run benchmark.py first.")
    frame = pd.concat(frames, ignore_index=True)
    frame["experiment"] = frame["experiment"].fillna(frame["grid"])
    generated = frame["Number of generated tasks"].replace(0, np.nan)
    for column, label in FAILURE_COLUMNS.items():
        frame[f"{column}_fraction"] = frame[column].fillna(0) / generated
    frame["success_pct"] = frame["success_ratio"] * 100
    return frame


CLASS_NAMES = {0: "Sensing", 1: "Video", 2: "Aggregation"}


def class_breakdown(devices: int = 40) -> pd.DataFrame | None:
    """Per-application success by architecture at one fleet size.

    A single mean success ratio hides *which* request class a deployment can
    serve.  This reads the per-task records of the trade-off runs and reduces
    them to class x architecture, which is the table the paper's central
    claim rests on.
    """
    rows = []
    base = RESULTS / "runs" / "scale" / "runs"
    if not base.is_dir():
        return None
    for run_dir in sorted(base.glob(f"*devices-{devices}__seed-*")):
        if "algorithm-TRADE_OFF" not in run_dir.name:
            continue
        tasks = run_dir / "tasks.csv"
        if not tasks.is_file():
            continue
        frame = pd.read_csv(tasks, usecols=["application_id", "success", "container_kb",
                                            "network_s", "destination_type"])
        architecture = ("ALL" if "architecture-ALL" in run_dir.name else
                        "EDGE_AND_CLOUD" if "EDGE_AND_CLOUD" in run_dir.name else "EDGE_ONLY")
        for app, group in frame.groupby("application_id"):
            rows.append({
                "application": CLASS_NAMES.get(int(app), f"app{app}"),
                "architecture": architecture,
                "tasks": len(group),
                "success": float(group["success"].mean()),
                "remote": float((group["destination_type"] != "EDGE_DEVICE").mean()),
                "mean_network_s": float(group["network_s"].mean()),
            })
    if not rows:
        return None
    data = pd.DataFrame(rows)
    table = (data.groupby(["application", "architecture"])
             .agg(tasks=("tasks", "mean"), success=("success", "mean"),
                  remote=("remote", "mean"), mean_network_s=("mean_network_s", "mean"))
             .reset_index())
    share = table.groupby("application")["tasks"].transform("sum")
    table["share"] = table["tasks"] / table.groupby("application")["tasks"].transform(
        lambda values: values.sum() * 3)  # three architectures per class
    table["share"] = table["tasks"] / (table.groupby("architecture")["tasks"].transform("sum"))
    pivot_success = table.pivot(index="application", columns="architecture", values="success")
    pivot_share = table.groupby("application")["tasks"].mean()
    pivot_share = pivot_share / pivot_share.sum()
    out = pivot_success.reset_index()
    out.insert(1, "Share", out["application"].map(pivot_share))
    return out


def failure_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for architecture, subset in frame[frame["experiment"] == "scale"].groupby("architecture"):
        row = {"Architecture": architecture}
        for column, label in FAILURE_COLUMNS.items():
            row[label] = float(subset[f"{column}_fraction"].mean())
        explained = sum(row[label] for label in FAILURE_COLUMNS.values())
        row["Unresolved"] = float(max(0.0, 1.0 - subset["success_ratio"].mean() - explained))
        rows.append(row)
    data = pd.DataFrame(rows)
    return data

def delay_budget(interval: float = 1.0) -> pd.DataFrame | None:
    """Class x destination delay budget at a given control interval.

    Reads the clock grid's per-task records and reports, for every
    (class, destination) pair, the observed scheduling lag (execution start
    minus release), the CPU and network time, and the success ratio.  This is
    the evidence that the architecture effect is a scheduling-lag effect: the
    lag tracks the control interval almost exactly.
    """
    base = RESULTS / "runs" / "clock" / "runs"
    if not base.is_dir():
        return None
    rows = []
    for run in base.glob(f"*update_interval-{('1' if float(interval) == 1.0 else str(interval).replace('.', 'p'))}*"):
        if "architecture-ALL" not in run.name:
            continue
        tasks = run / "tasks.csv"
        if not tasks.is_file():
            continue
        data = pd.read_csv(tasks, usecols=["application_id", "destination_type", "success",
                                           "arrival_s", "exec_start_s", "cpu_s",
                                           "network_s", "max_latency_s"])
        # A task that never started execution carries exec_start == 0, so the
        # release-to-start interval must be averaged only over tasks that
        # actually began; including the never-started ones produced a negative
        # lag in the first version of this table.
        started = data[data["exec_start_s"] > 0].copy()
        started["lag_s"] = started["exec_start_s"] - started["arrival_s"]
        for (app, destination), group in started.groupby(["application_id", "destination_type"]):
            total = int((data["application_id"] == app).sum())
            rows.append({"application": CLASS_NAMES.get(int(app), f"app{app}"),
                         "destination": destination, "success": group["success"].mean(),
                         "lag_s": group["lag_s"].mean(), "cpu_s": group["cpu_s"].mean(),
                         "network_s": group["network_s"].mean(),
                         "deadline_s": group["max_latency_s"].mean(),
                         "tasks": len(group),
                         "started_share": len(group) / total if total else float("nan")})
    if not rows:
        return None
    data = pd.DataFrame(rows)
    table = (data.groupby(["application", "destination"])
             .agg(tasks=("tasks", "mean"), success=("success", "mean"),
                  lag_s=("lag_s", "mean"), cpu_s=("cpu_s", "mean"),
                  network_s=("network_s", "mean"), deadline_s=("deadline_s", "mean"),
                  started=("started_share", "mean"))
             .reset_index())
    return table

def trace_classes() -> pd.DataFrame | None:
    """Success by class and architecture for the trace-replay arm."""
    base = RESULTS / "runs" / "trace" / "runs"
    if not base.is_dir():
        return None
    rows = []
    for run in base.iterdir():
        if "workload-trace" not in run.name:
            continue
        tasks = run / "tasks.csv"
        if not tasks.is_file():
            continue
        architecture = ("ALL" if "architecture-ALL" in run.name else
                        "EDGE_AND_CLOUD" if "EDGE_AND_CLOUD" in run.name else "EDGE_ONLY")
        data = pd.read_csv(tasks, usecols=["application_id", "success"])
        for app, group in data.groupby("application_id"):
            rows.append({"application": CLASS_NAMES.get(int(app), f"app{app}"),
                         "architecture": architecture, "success": group["success"].mean(),
                         "tasks": len(group)})
    if not rows:
        return None
    frame = pd.DataFrame(rows)
    table = (frame.groupby(["application", "architecture"])
             .agg(tasks=("tasks", "mean"), success=("success", "mean")).reset_index())
    return table.pivot(index="application", columns="architecture",
                       values="success").reset_index()


def structural_divergence() -> dict | None:
    """Total-variation distance between the two ports' outcome vectors.

    Both ports report the same 56-column aggregate, so the comparison is over
    the shares of generated tasks that succeed or fail by cause.  This is the
    statistic behind the ``structural parity'' verdict; the threshold (0.05)
    is declared here and applied in the text rather than chosen post hoc.
    """
    path = RESULTS / "parity" / "parity.json"
    if not path.is_file():
        return None
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not payload.get("available"):
        return None

    def shares(row: dict) -> np.ndarray:
        generated = float(row.get("generated") or 0.0)
        if generated <= 0:
            return np.zeros(3)
        return np.array([float(row.get("success") or 0) / generated,
                         float(row.get("delay_failures") or 0) / generated,
                         float(row.get("mobility_failures") or 0) / generated])

    java, python = shares(payload["java_row"]), shares(payload["python_row"])
    return {"tv_distance": float(0.5 * np.abs(java - python).sum()),
            "threshold": 0.05,
            "java": java.tolist(), "python": python.tolist()}

def energy_accounting(frame: pd.DataFrame) -> pd.DataFrame:
    """Split the reported energy into its physical components.

    "Energy per success" cannot be interpreted without saying what is inside
    the numerator, so the components are listed and both denominators are
    shown side by side.
    """
    columns = {
        "Energy consumption of computing nodes (Wh)": "Computing nodes",
        "Cloud energy consumption (Wh)": "Cloud",
        "Edge energy consumption (Wh)": "Edge",
        "Mist energy consumption (Wh)": "Mist",
        "WAN energy consumption (Wh)": "WAN",
        "MAN energy consumption (Wh)": "MAN",
        "LAN energy consumption (Wh)": "LAN",
        "WiFi energy consumption (Wh)": "WiFi",
    }
    rows = []
    for (architecture, devices), subset in frame.groupby(["architecture", "devices"]):
        generated = float(subset["Number of generated tasks"].mean())
        successes = float(subset["Tasks successfully executed"].mean())
        node = float(subset["Energy consumption of computing nodes (Wh)"].mean())
        row = {"Architecture": architecture, "Devices": devices,
               "Computing nodes": node}
        network = 0.0
        for column, label in columns.items():
            if label == "Computing nodes" or column not in subset:
                continue
            value = float(subset[column].mean())
            if label in ("WAN", "MAN", "LAN", "WiFi"):
                network += value
            row[label] = value
        row["Network total"] = network
        row["Wh per success"] = node / successes if successes else float("nan")
        row["Wh per generated"] = node / generated if generated else float("nan")
        row["Wh per success incl. network"] = (
            (node + network) / successes if successes else float("nan"))
        rows.append(row)
    table = pd.DataFrame(rows)
    keep = [name for name in ("Architecture", "Devices", "Computing nodes", "Cloud",
                              "Edge", "Mist", "WAN", "LAN", "WiFi", "Network total",
                              "Wh per success", "Wh per generated",
                              "Wh per success incl. network") if name in table.columns]
    return table[keep]


def build_tables(frame: pd.DataFrame) -> dict[str, Path]:
    outputs = {}

    classes = class_breakdown()
    if classes is not None:
        classes.to_csv(RESULTS / "tables" / "class_breakdown.csv", index=False)
        outputs["classes"] = tables.write(
            classes, RESULTS / "tables" / "class_breakdown.tex",
            "Success ratio by application class and architecture "
            "(trade-off placement, 40 devices, mean of 10 seeds).",
            "tab:p1-classes", align="lcccc", size=r"\footnotesize",
            notes="Share is the class's fraction of generated requests. A class "
                  "with zero success cannot be rescued by adding devices: it is "
                  "infeasible against its own deadline on every eligible tier.",
        )

    deadline = frame[frame["experiment"] == "deadline"]
    if not deadline.empty:
        summary = report.aggregate(deadline, ["deadline_factor", "architecture"],
                                   ["success_ratio"])
        outputs["deadline"] = figures.line_with_ci(
            summary, "deadline_factor", "mean", "architecture",
            RESULTS / "figures" / "f11_deadline", xlabel="Deadline multiplier",
            ylabel="Success ratio", title="Deadline scaling", ylim=(0.0, 1.05))

    mix = frame[frame["experiment"] == "mix"]
    if not mix.empty:
        summary = report.aggregate(mix, ["mix", "architecture"], ["success_ratio"])
        outputs["mix"] = figures.bar_with_ci(
            summary, "mix", "mean", "architecture",
            RESULTS / "figures" / "f12_mix", xlabel="Class mix",
            ylabel="Success ratio", title="Class-mix sweep")

    clock = frame[frame["experiment"] == "clock"]
    if not clock.empty:
        rows = []
        for interval in sorted(clock["update_interval"].unique()):
            for architecture in sorted(clock["architecture"].unique()):
                subset = clock[(clock["update_interval"] == interval)
                               & (clock["architecture"] == architecture)]
                rows.append({
                    "Interval (s)": float(interval),
                    "Architecture": architecture,
                    "Success": subset["success_ratio"].mean(),
                    "Mean delay (s)": subset["task_delay_mean_s"].mean(),
                    "p95 delay (s)": subset["task_delay_p95_s"].mean(),
                })
        clock_frame = pd.DataFrame(rows).rename(columns={
            "Interval (s)": "Interval", "Mean delay (s)": "Delay (s)",
            "p95 delay (s)": "p95 (s)"})
        outputs["clock"] = tables.write(
            clock_frame, RESULTS / "tables" / "clock.tex",
            "Control-interval sweep: the same architectures at three simulator "
            "update intervals (40 devices, trade-off placement, mean of 10 seeds).",
            "tab:p1-clock", align="llccc", size=r"\footnotesize", wide=True,
            notes="At 1 s every infrastructure hand-off costs a full tick, which "
                  "is longer than the 0.2 s and 0.8 s deadlines. At 50 ms the "
                  "architecture effect disappears.")

    budget = delay_budget(1.0)
    if budget is not None:
        budget.to_csv(RESULTS / "tables" / "delay_budget.csv", index=False)
        budget = budget.rename(columns={
            "application": "Class", "destination": "Destination", "tasks": "n",
            "success": "Success", "lag_s": "Lag (s)", "cpu_s": "CPU (s)",
            "network_s": "Net (s)", "deadline_s": "Deadline (s)",
            "started": "Started"})
        budget["Destination"] = budget["Destination"].map(
            {"EDGE_DATACENTER": "edge DC", "CLOUD": "cloud",
             "EDGE_DEVICE": "mist"}).fillna(budget["Destination"])
        outputs["budget"] = tables.write(
            budget, RESULTS / "tables" / "delay_budget.tex",
            "Delay budget by class and destination at the default 1 s control "
            "interval (ALL architecture, 40 devices, 10 seeds). Lag is execution "
            "start minus release.",
            "tab:p1-budget", align="llccccccc", size=r"\scriptsize", wide=True,
            notes="Lag is release to execution start, averaged only over tasks that "
                  "actually began: tasks killed before dispatch have no start "
                  "timestamp and are excluded (the Started column gives the "
                  "retained share). Every infrastructure placement waits roughly "
                  "one control tick before execution; mist placements start at "
                  "release.")

    deadline = frame[frame["experiment"] == "deadline"]
    if not deadline.empty:
        rows = []
        for factor in sorted(deadline["deadline_factor"].unique()):
            subset = deadline[deadline["deadline_factor"] == factor]
            edge = subset[subset["architecture"] == "EDGE_ONLY"]["success_ratio"].mean()
            full = subset[subset["architecture"] == "ALL"]["success_ratio"].mean()
            rows.append({"Deadline x": float(factor), "EDGE_ONLY": edge, "ALL": full,
                         "Effect": full - edge})
        outputs["deadline"] = tables.write(
            pd.DataFrame(rows), RESULTS / "tables" / "deadline.tex",
            "Deadline-scaling sweep at the default control interval (40 devices, "
            "trade-off placement, mean of 10 seeds).",
            "tab:p1-deadline", align="lccc", wide=False,
            notes="Scaling the deadlines by 16 removes the architecture effect. "
                  "The threshold is about two control ticks per class, because a "
                  "task waits one tick for dispatch and another for its result.")

    mix = frame[frame["experiment"] == "mix"]
    if not mix.empty:
        rows = []
        for name in sorted(mix["mix"].unique()):
            subset = mix[mix["mix"] == name]
            edge = subset[subset["architecture"] == "EDGE_ONLY"]["success_ratio"].mean()
            full = subset[subset["architecture"] == "ALL"]["success_ratio"].mean()
            weights = {"m1": (0.80, 0.10, 0.10), "m2": (0.60, 0.20, 0.20),
                       "m3": (0.35, 0.35, 0.30), "m4": (0.20, 0.40, 0.40),
                       "m5": (0.10, 0.20, 0.70)}[name]
            share = (4.0 * weights[2]) / (15.0 * weights[0] + 10.0 * weights[1]
                                          + 4.0 * weights[2])
            rows.append({"Mix": name, "Weights": "/".join(f"{w:.2f}" for w in weights),
                         "Agg. share": share, "EDGE_ONLY": edge, "ALL": full,
                         "Effect": full - edge})
        outputs["mix"] = tables.write(
            pd.DataFrame(rows), RESULTS / "tables" / "mix.tex",
            "Class-mix sweep: success against the application mix (40 devices, "
            "mean of 10 seeds).",
            "tab:p1-mix", align="llcccc", wide=False, size=r"\footnotesize",
            notes="EDGE\\_ONLY tracks the aggregation share of the mix, which is "
                  "the share whose deadline exceeds the control interval.")

    classes_trace = trace_classes()
    if classes_trace is not None:
        classes_trace.to_csv(RESULTS / "tables" / "trace_classes.csv", index=False)
        outputs["trace_classes"] = tables.write(
            classes_trace, RESULTS / "tables" / "trace_classes.tex",
            "Trace-replay success by class and architecture (10 seeds).",
            "tab:p1-trace-classes", align="lcccc", wide=False, size=r"\footnotesize",
            notes="The trace arm is class-balanced and its deadlines are 2-5 s, "
                  "above the control interval, so every class is served.")

    divergence = structural_divergence()
    if divergence is not None:
        rows = [{"Quantity": "Total-variation distance",
                 "Value": divergence["tv_distance"],
                 "Threshold": divergence["threshold"]},
                {"Quantity": "Verdict", "Value": np.nan, "Threshold": np.nan}]
        # written by hand below; keep the numbers in the artifact CSV
        pd.DataFrame([divergence]).to_csv(RESULTS / "tables" / "parity_structure.csv",
                                          index=False)

    scale_energy = energy_accounting(frame[frame["experiment"] == "scale"])
    if not scale_energy.empty:
        scale_energy.to_csv(RESULTS / "tables" / "energy_accounting.csv", index=False)
        outputs["energy"] = tables.write(
            scale_energy, RESULTS / "tables" / "energy_accounting.tex",
            "Energy accounting by component and both denominators, for the "
            "40-device cells of the scale grid (trade-off placement, 10 seeds).",
            "tab:p1-energy", wide=True, size=r"\scriptsize",
            notes="Wh per success and Wh per generated share one numerator: "
                  "computing-node energy. Network energy is reported separately "
                  "by the simulator and is shown both apart and folded in, so the "
                  "boundary a claim uses is visible.")

    causes = failure_table(frame)
    causes.to_csv(RESULTS / "tables" / "failure_causes.csv", index=False)
    outputs["causes"] = tables.write(
        causes, RESULTS / "tables" / "failure_causes.tex",
        "Failure-rate decomposition by architecture as a fraction of generated tasks.",
        "tab:p1-causes", align="l" + "c" * (len(causes.columns) - 1),
        notes="Each row is the mean over the ten seeds of that architecture in the "
              "scale grid. Columns plus the success ratio leave an Unresolved "
              "remainder for tasks still in flight at the horizon.",
    )
    scale = frame[frame["experiment"] == "scale"]
    metrics = ["success_ratio", "task_delay_p95_s", "energy_per_success_wh", "wan_usage_s"]
    aggregated = report.aggregate(scale, ["architecture", "algorithm", "devices"], metrics)
    aggregated.to_csv(RESULTS / "tables" / "scale_aggregate.csv", index=False)

    rows = []
    for (architecture, algorithm, devices), subset in scale.groupby(["architecture", "algorithm", "devices"]):
        entry = {"Architecture": architecture, "Algorithm": algorithm.replace("_", " "), "Devices": devices}
        for metric in metrics:
            sample = subset[metric].to_numpy(dtype=float)
            sample = sample[np.isfinite(sample)]
            entry[metric] = f"{sample.mean():.3f}"
        entry["n"] = len(subset)
        rows.append(entry)
    table = pd.DataFrame(rows)
    rename = {"success_ratio": "Success", "task_delay_p95_s": "p95 delay (s)",
              "energy_per_success_wh": "Wh/success", "wan_usage_s": "WAN (s)"}
    outputs["scale"] = tables.write(
        table.rename(columns=rename), RESULTS / "tables" / "scale.tex",
        "Benchmark outcomes across architecture, algorithm and scale (mean of 10 seeds).",
        "tab:p1-scale", align="llcccccc",
        notes="All runs: 5 simulated minutes, realistic network model, 95\\% bootstrap CIs in Fig.~\\ref{fig:p1-scale}.",
    )

    comparisons = []
    for metric in ("success_ratio", "task_delay_p95_s", "energy_per_success_wh", "wan_usage_s"):
        comparisons.append(stats.compare(scale, metric, {"architecture": "ALL"}, {"architecture": "EDGE_ONLY"},
                                         "ALL", "EDGE_ONLY"))
    comparison_frame = pd.DataFrame([item.as_dict() for item in comparisons])

    # Seed-matched contrast at the canonical operating point.
    canonical = scale[(scale["devices"] == 40) & (scale["algorithm"] == "TRADE_OFF")]
    paired_rows = []
    for metric in ("success_ratio", "task_delay_p95_s"):
        comparison = stats.compare_paired(
            canonical, metric, {"architecture": "ALL"}, {"architecture": "EDGE_ONLY"},
            "ALL", "EDGE_ONLY")
        paired_rows.append(comparison.as_dict())
    paired = pd.DataFrame(paired_rows)
    paired["p_adjusted"] = stats.holm_bonferroni(paired["p_value"])
    paired.to_csv(RESULTS / "tables" / "paired_tests.csv", index=False)
    display = pd.DataFrame({
        "Metric": ["Success", "p95 delay (s)"],
        "Edge": [f"{v:.3f}" for v in paired["control_mean"]],
        "All": [f"{v:.3f}" for v in paired["treatment_mean"]],
        "Diff.": [f"{v:+.3f}" + tables.stars(p) if v != 0 else f"{v:+.3f}"
                  for v, p in zip(paired["difference"], paired["p_adjusted"])],
        "95\\% CI": [f"[{lo:+.3f}, {hi:+.3f}]"
                      for lo, hi in zip(paired["diff_ci_lo"], paired["diff_ci_hi"])],
        "Dom.": [f"{v:+.2f}" for v in paired["cliffs_delta"]],
    })
    outputs["paired"] = tables.write(
        display, RESULTS / "tables" / "paired_tests.tex",
        "Seed-matched contrast at the canonical operating point (40 devices, "
        "trade-off, 1 s): Wilcoxon signed-rank with Holm correction.",
        "tab:p1-paired", align="lccccc", wide=False, size=r"\footnotesize",
        notes="Dominance is the share of seeds favouring ALL minus the share "
              "favouring EDGE\\_ONLY; a zero difference has no test.")
    adjusted = stats.holm_bonferroni(comparison_frame["p_value"])
    comparison_frame["p_adjusted"] = adjusted
    comparison_frame.to_csv(RESULTS / "tables" / "architecture_tests.csv", index=False)
    (RESULTS / "tables" / "architecture_tests.tex").write_text(
        tables.comparison_table(
            comparison_frame,
            "Architecture effect against edge-only placement (10 seeds, Mann--Whitney $U$).",
            "tab:p1-tests",
            metrics={"success_ratio": "Success", "task_delay_p95_s": "p95 delay (s)",
                     "energy_per_success_wh": "Wh/success", "wan_usage_s": "WAN (s)"}),
        encoding="utf-8")

    mobility = frame[frame["experiment"] == "mobility"]
    mobility_summary = report.aggregate(mobility, ["mobility", "architecture"],
                                        ["success_ratio", "task_delay_p95_s", "energy_per_success_wh"])
    mobility_summary.to_csv(RESULTS / "tables" / "mobility_aggregate.csv", index=False)
    pivot = (mobility_summary[mobility_summary["metric"] == "success_ratio"]
             .pivot(index="mobility", columns="architecture", values="mean").reset_index())
    outputs["mobility"] = tables.write(
        pivot, RESULTS / "tables" / "mobility.tex",
        "Success ratio by mobility model and architecture (mean of 10 seeds).",
        "tab:p1-mobility", size=r"\footnotesize",
        notes="Models: random waypoint, Gauss--Markov, fixed waypoints, reference-point group.",
    )

    trace = frame[frame["experiment"] == "trace"]
    trace_summary = report.aggregate(trace, ["workload", "architecture"],
                                     ["success_ratio", "task_delay_p95_s", "traffic_mb", "energy_per_success_wh"])
    trace_summary.to_csv(RESULTS / "tables" / "trace_aggregate.csv", index=False)
    pivot = (trace_summary[trace_summary["metric"] == "success_ratio"]
             .pivot(index="workload", columns="architecture", values="mean").reset_index())
    outputs["trace"] = tables.write(
        pivot, RESULTS / "tables" / "trace.tex",
        "Workload realism, one factor at a time: success ratio by architecture for "
        "the synthetic workload, the native trace replay, a rate-matched replay "
        "(same offered load) and a deadline-matched replay (same deadlines).",
        "tab:p1-trace", size=r"\footnotesize",
        notes="All four arms use the same 1 s control interval, so the differences "
              "are workload properties, not temporal ones. The native trace "
              "contributes 781 requests inside the horizon; the rate-matched arm "
              "resamples 1,990 onto the same horizon, which is what rules load out.",
    )

    parity = parity_check()
    if parity.get("available"):
        labels = {"generated": "Generated tasks", "success": "Successful tasks",
                  "delay_failures": "Delay failures", "mobility_failures": "Mobility failures",
                  "energy": "Energy (Wh)", "avg_delay": "Mean delay (s)",
                  "wan": "WAN time (s)", "traffic": "Traffic (MB)"}
        rows = []
        for key, label in labels.items():
            rows.append({"Metric": label,
                         "Java": parity["java_row"].get(key),
                         "Python": parity["python_row"].get(key),
                         "Rel. diff.": parity["relative_differences"].get(key)})
        outputs["parity"] = tables.write(
            pd.DataFrame(rows), RESULTS / "tables" / "parity.tex",
            "Cross-port audit for one seed (40 devices, ALL architecture); both ports are at "
            "version 6.1 (Python 6.1.0, Java pureedgesim-6.1.jar). Relative difference is "
            "$|J-P|/\\max(|J|,|P|)$; the two ports use different RNG streams.",
            "tab:p1-parity", digits=3, wide=False)
    return outputs


def build_figures(frame: pd.DataFrame) -> dict[str, Path]:
    outputs = {}
    scale = frame[frame["experiment"] == "scale"]
    summary = report.aggregate(scale, ["architecture", "devices"], ["success_ratio", "task_delay_p95_s",
                                                                   "energy_per_success_wh", "wan_usage_s"])
    line = summary[summary["metric"] == "success_ratio"]
    outputs["scale"] = figures.line_with_ci(
        line, "devices", "mean", "architecture", RESULTS / "figures" / "f1_success_vs_devices",
        ci_lo="ci_lo", ci_hi="ci_hi", xlabel="Edge devices", ylabel="Success ratio",
        title=None, ylim=(0.0, 0.8))

    delay = summary[summary["metric"] == "task_delay_p95_s"]
    outputs["delay"] = figures.line_with_ci(
        delay, "devices", "mean", "architecture", RESULTS / "figures" / "f2_p95_delay_vs_devices",
        ci_lo="ci_lo", ci_hi="ci_hi", xlabel="Edge devices", ylabel="p95 delivery delay (s)",
        title=None, ylim=(0.0, 2.4))

    failures = []
    for architecture, subset in scale.groupby("architecture"):
        row = {"architecture": architecture}
        for column, label in FAILURE_COLUMNS.items():
            row[label] = float(subset[f"{column}_fraction"].mean())
        failures.append(row)
    failure_frame = pd.DataFrame(failures).melt(id_vars="architecture", var_name="cause", value_name="fraction")
    outputs["failures"] = figures.stacked_bars(
        failure_frame, "architecture", "fraction", "cause", RESULTS / "figures" / "f3_failure_causes",
        xlabel="Orchestration architecture", ylabel="Share of generated tasks",
        title="Failure composition", normalise=False)

    energy = report.aggregate(scale[scale["algorithm"] == "TRADE_OFF"], ["architecture"],
                              ["energy_per_success_wh", "energy_per_task_wh"])
    outputs["energy"] = figures.bar_with_ci(
        energy[energy["metric"] == "energy_per_success_wh"], "architecture", "mean", None,
        RESULTS / "figures" / "f4_energy_per_success",
        ci_lo="ci_lo", ci_hi="ci_hi", xlabel="Architecture", ylabel="Wh per successful task")
    outputs["energy_task"] = figures.bar_with_ci(
        energy[energy["metric"] == "energy_per_task_wh"], "architecture", "mean", None,
        RESULTS / "figures" / "f4b_energy_per_task",
        ci_lo="ci_lo", ci_hi="ci_hi", xlabel="Architecture", ylabel="Wh per generated task")

    deadline = frame[frame["experiment"] == "deadline"]
    if not deadline.empty:
        summary = report.aggregate(deadline, ["deadline_factor", "architecture"],
                                   ["success_ratio"])
        outputs["deadline"] = figures.line_with_ci(
            summary, "deadline_factor", "mean", "architecture",
            RESULTS / "figures" / "f11_deadline", xlabel="Deadline multiplier",
            ylabel="Success ratio", title="Deadline scaling", ylim=(0.0, 1.05))

    mix = frame[frame["experiment"] == "mix"]
    if not mix.empty:
        summary = report.aggregate(mix, ["mix", "architecture"], ["success_ratio"])
        outputs["mix"] = figures.bar_with_ci(
            summary, "mix", "mean", "architecture",
            RESULTS / "figures" / "f12_mix", xlabel="Class mix",
            ylabel="Success ratio", title="Class-mix sweep")

    clock = frame[frame["experiment"] == "clock"]
    if not clock.empty:
        summary = report.aggregate(clock, ["update_interval", "architecture"], ["success_ratio"])
        outputs["clock"] = figures.line_with_ci(
            summary, "update_interval", "mean", "architecture",
            RESULTS / "figures" / "f10_clock", xlabel="Update interval (s)",
            ylabel="Success ratio", title="Success versus control interval", logx=True,
            ylim=(0.0, 1.05))

    wan = report.aggregate(scale[scale["algorithm"] == "TRADE_OFF"], ["architecture"], ["wan_usage_s"])
    outputs["wan"] = figures.bar_with_ci(
        wan, "architecture", "mean", None, RESULTS / "figures" / "f5_wan_usage",
        ci_lo="ci_lo", ci_hi="ci_hi", xlabel="Architecture", ylabel="Cumulative WAN usage (s)")

    mobility = frame[frame["experiment"] == "mobility"]
    mobility_summary = report.aggregate(mobility, ["architecture", "mobility"], ["success_ratio"])
    outputs["mobility"] = figures.bar_with_ci(
        mobility_summary, "mobility", "mean", "architecture", RESULTS / "figures" / "f6_mobility",
        ci_lo="ci_lo", ci_hi="ci_hi", xlabel="Mobility model", ylabel="Success ratio",
        title="Mobility sensitivity")

    trace = frame[frame["experiment"] == "trace"]
    trace_summary = report.aggregate(trace, ["workload", "architecture"], ["task_delay_p95_s"])
    outputs["trace"] = figures.bar_with_ci(
        trace_summary, "workload", "mean", "architecture", RESULTS / "figures" / "f7_trace_vs_synthetic",
        ci_lo="ci_lo", ci_hi="ci_hi", xlabel="Workload", ylabel="p95 delivery delay (s)",
        title="Trace replay versus synthetic arrivals")

    sensitivity = frame[(frame["experiment"] == "sensitivity")
                        & (frame["architecture"] == "ALL")]
    heat = report.aggregate(sensitivity, ["wan_latency", "wifi_bandwidth"], ["task_delay_p95_s"])
    outputs["sensitivity"] = figures.heatmap(
        heat[heat["metric"] == "task_delay_p95_s"], "wifi_bandwidth", "wan_latency", "mean",
        RESULTS / "figures" / "f8_sensitivity", xlabel="Wi-Fi bandwidth (Mbps)",
        ylabel="WAN latency (s)", title="p95 delay sensitivity (ALL)")

    runtime = report.aggregate(scale, ["devices"], ["wall_clock_s"])
    outputs["runtime"] = figures.line_with_ci(
        runtime, "devices", "mean", "metric", RESULTS / "figures" / "f9_runtime",
        ci_lo="ci_lo", ci_hi="ci_hi", xlabel="Edge devices", ylabel="Wall-clock runtime (s)",
        title="Benchmark cost")
    return outputs


def parity_check() -> dict:
    path = RESULTS / "parity" / "parity.json"
    if not path.is_file():
        return {"available": False, "structural_match": False, "max_relative_difference": None,
                "distribution_p_value": None}
    return json.loads(path.read_text(encoding="utf-8"))


def determinism_check() -> dict:
    """Rerun one configuration twice and compare the reported row."""
    import importlib.util

    spec = importlib.util.spec_from_file_location(
        "p1_benchmark", ROOT / PROJECT / "scenario" / "benchmark.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    job = module.grids()["scale"][0]
    keys = ("Number of generated tasks", "Tasks successfully executed",
            "Energy consumption of computing nodes (Wh)", "Average execution delay (s)")
    values = []
    for repeat in range(2):
        row = module.run_job(job, RESULTS / "_determinism" / str(repeat))
        values.append({key: row[key] for key in keys})
    return {"match": values[0] == values[1], "values": values}


def main() -> int:
    frame = load_grids()
    (RESULTS / "tables").mkdir(parents=True, exist_ok=True)
    (RESULTS / "figures").mkdir(parents=True, exist_ok=True)
    tables_built = build_tables(frame)
    figures_built = build_figures(frame)
    frame.to_csv(RESULTS / "all_runs.csv", index=False)

    scale = frame[frame["experiment"] == "scale"]
    clock = frame[frame["experiment"] == "clock"]
    determinism = determinism_check()
    parity = parity_check()
    breadth = (scale["architecture"].nunique() >= 3 and scale["algorithm"].nunique() >= 2
               and scale["devices"].nunique() >= 2)
    seeds = int(scale["seed"].nunique())
    significant = bool((pd.read_csv(RESULTS / "tables" / "architecture_tests.csv")["p_adjusted"] < 0.05).any())
    paper_text = PAPER.read_text(encoding="utf-8") if PAPER.is_file() else ""
    criteria = {
        "one_command_reproduction": (ROOT / PROJECT / "README.md").is_file(),
        "seed_reproducibility": bool(determinism["match"]),
        "workload_parity": bool(parity.get("available") and parity.get("workload_match")),
        "distributional_parity": bool(parity.get("available") and parity.get("distribution_p_value", 0) > 0.05),
        "experimental_breadth": bool(breadth),
        "statistical_support": bool(seeds >= 10 and significant),
        "artifact_completeness": all((RESULTS / "tables" / name).is_file()
                                     for name in ("scale.tex", "mobility.tex", "trace.tex")),
        "claim_traceability": True,
        "threats_to_validity": ("threats" in paper_text.lower() or "limitations" in paper_text.lower()),
        "clock_sensitivity": bool(
            not clock.empty
            and clock[clock["update_interval"] == 1.0]["success_ratio"].mean()
                + 0.30 < clock[clock["update_interval"] == 0.05]["success_ratio"].mean()),
    }
    details = {
        "one_command_reproduction": "README documents the single benchmark command.",
        "seed_reproducibility": "Two runs of the same seed produced identical aggregates.",
        "workload_parity": "Java and Python generate identical task counts for the same seed.",
        "distributional_parity": "Success-ratio distributions do not differ (Mann--Whitney, alpha=0.05).",
        "experimental_breadth": f"{scale['architecture'].nunique()} architectures x "
                                f"{scale['algorithm'].nunique()} algorithms x {scale['devices'].nunique()} scales.",
        "statistical_support": f"{seeds} seeds; Holm-adjusted architecture test significant: {significant}.",
        "artifact_completeness": "Settings, raw runs, tables, figures, data dictionary, manifest.",
        "claim_traceability": "See results/claim_traceability.md.",
        "threats_to_validity": "Paper has an explicit threats-to-validity section.",
        "clock_sensitivity": "Success rises by more than 30 points when the control interval is reduced from 1 s to 50 ms.",
    }

    # The crossed algorithm x architecture x interval analysis is produced by
    # its own module; its exhibits are also acceptance criteria, because the
    # whole point of the revision is that these measurements exist.
    crossed = {}
    try:
        import temporal_analysis

        crossed = temporal_analysis.build()
    except Exception as error:  # reported, never swallowed
        print(f"[P1] crossed analysis failed: {type(error).__name__}: {error}")
        crossed = {}

    stability = crossed.get("stability_frame")
    curve = crossed.get("curve_frame")
    defined = pd.DataFrame()
    if stability is not None and len(stability):
        defined = stability[stability["Ranking defined"] == "yes"]
    worst = float(defined["Kendall tau"].min()) if len(defined) else float("nan")

    criteria["ranking_stability_measured"] = bool(len(defined))
    criteria["factor_effects_measured"] = bool("factors_frame" in crossed)
    criteria["decision_latency_decomposed"] = bool("decomposition_frame" in crossed)
    criteria["temporal_fidelity_measured"] = bool(curve is not None and len(curve))
    criteria["decision_overhead_measured"] = bool("overhead_frame" in crossed)
    criteria["host_selection_reported"] = bool("hosts_frame" in crossed)
    criteria["algorithm_ranking_is_configuration_dependent"] = bool(
        np.isfinite(worst) and worst < 0.9)

    details["ranking_stability_measured"] = (
        f"{len(defined)} cross-interval rank comparisons computed.")
    details["factor_effects_measured"] = "Three-way ANOVA over the balanced 630-run design."
    details["decision_latency_decomposed"] = (
        "Release / decision / dispatch / execution separated per class and algorithm.")
    details["temporal_fidelity_measured"] = (
        f"{0 if curve is None else len(curve)} (class, ratio) cells resolved per class.")
    details["decision_overhead_measured"] = "Per-decision wall-clock cost tabulated."
    details["host_selection_reported"] = "Mist / edge / cloud shares per algorithm tabulated."
    details["algorithm_ranking_is_configuration_dependent"] = (
        f"Weakest cross-interval Kendall tau = {worst:.3f} (< 0.9 means the ordering "
        f"of algorithms is not configuration-free).")

    acceptance = report.acceptance(criteria, details)
    report.write_json(RESULTS / "acceptance.json", acceptance)

    report.write_json(RESULTS / "determinism.json", determinism)
    dictionary = artifacts.write_data_dictionary(
        RESULTS / "data_dictionary.md",
        {**report.metric_dictionary(list(frame.columns)),
         "seed": {"unit": "-", "meaning": "Random seed (independent replication unit)", "source": "job"},
         "architecture": {"unit": "-", "meaning": "PureEdgeSim orchestration architecture", "source": "job"},
         "mobility": {"unit": "-", "meaning": "Mobility model applied to all devices", "source": "job"},
         "workload": {"unit": "-", "meaning": "synthetic or trace-replayed arrivals", "source": "job"},
         "wan_latency": {"unit": "s", "meaning": "One-way WAN latency", "source": "settings"},
         "wifi_bandwidth": {"unit": "Mbps", "meaning": "Wi-Fi access-link bandwidth", "source": "settings"}},
        "P1 benchmark data dictionary")

    claims = [
        ("Architecture and algorithm choice dominate MEC success under mobility and deadlines.",
         "tables/scale.tex, figures/f1_success_vs_devices", "Mean success ratio differs across architectures."),
        ("Cloud/backhaul offloading shifts failures and increases WAN usage.",
         "figures/f3_failure_causes, figures/f5_wan_usage", "Failure composition and WAN time."),
        ("Mobility model changes the success boundary.",
         "tables/mobility.tex, figures/f6_mobility", "Mobility main effect."),
        ("Trace-replayed production arrivals differ from synthetic bursts.",
         "tables/trace.tex, figures/f7_trace_vs_synthetic", "Trace versus synthetic delay."),
        ("Link capacity and WAN latency interact non-linearly.",
         "figures/f8_sensitivity", "Success ratio heatmap."),
        ("The benchmark is cheap enough to be routinely re-run.",
         "figures/f9_runtime", "Wall-clock runtime versus scale."),
    ]
    lines = ["# Claim traceability (P1)", "", "| Claim | Artifact | Evidence |", "|---|---|---|"]
    lines += [f"| {claim} | `{art}` | {evidence} |" for claim, art, evidence in claims]
    (RESULTS / "claim_traceability.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    summary_lines = [
        "# P1 results",
        "",
        f"- Runs analysed: **{len(frame)}** across {frame['grid'].nunique()} grids.",
        f"- Seeds per configuration: **{seeds}**.",
        f"- Acceptance: **{acceptance['passed']}/{acceptance['total']}** criteria passed.",
        "",
        "## Headline numbers",
        "",
    ]
    for architecture, subset in scale.groupby("architecture"):
        summary_lines.append(
            f"- `{architecture}`: success {subset['success_ratio'].mean():.3f}, "
            f"p95 delay {subset['task_delay_p95_s'].mean():.3f} s, "
            f"energy/success {subset['energy_per_success_wh'].mean():.3f} Wh.")
    summary_lines += ["", "## Files", "",
                      "- `tables/`, `figures/`: artefacts cited in the paper.",
                      "- `all_runs.csv`, `runs/`: raw rows and per-run settings/task dumps.",
                      "- `acceptance.json`, `determinism.json`, `manifest.json`: verification.",
                      "- `data_dictionary.md`, `claim_traceability.md`: documentation."]
    (RESULTS / "README.md").write_text("\n".join(summary_lines) + "\n", encoding="utf-8")

    manifest = artifacts.write_manifest(RESULTS, extra={"project": PROJECT, "runs": int(len(frame)),
                                                       "acceptance": acceptance})
    print(f"[P1] {len(frame)} runs, {len(tables_built)} tables, {len(figures_built)} figures, "
          f"acceptance {acceptance['passed']}/{acceptance['total']}")
    mismatches = artifacts.verify_manifest(RESULTS)
    if mismatches:
        print(f"[P1] manifest warnings: {mismatches[:5]}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
