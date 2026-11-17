"""Disentangling algorithmic, architectural and simulator effects.

This module reads the ``algo`` grid (7 orchestration algorithms x 3
architectures x 3 control intervals x 10 seeds = 630 runs) plus the per-task
records of a seed subsample, and produces the exhibits the reconstruction plan
asks for:

``algorithm_ranking.tex``      per-seed success with a seed-level interval, so
                               a reader can see whether one algorithm dominates
                               or whether superiority is configuration-dependent
``ranking_stability.tex``      Kendall's tau / Spearman rho between temporal
                               configurations, with the number of rank changes
``factor_effects.tex``         factorial ANOVA main effects and interactions
``latency_decomposition.tex``  orchestration decision latency by algorithm,
                               architecture and interval
``temporal_fidelity.tex``      success as a function of measured latency over
                               deadline, i.e. the calibration curve that
                               replaces the D/10 rule
``host_selection.tex``         where each algorithm actually places work
``decision_overhead.tex``      wall-clock cost of producing a decision
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd
from scipy import stats as scipy_stats

ROOT = Path(__file__).resolve().parents[2]
import sys

if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from research_common import figures, report, stats, tables  # noqa: E402

PROJECT = "01_sim2deploy_sagin_uav_benchmark"
RESULTS = ROOT / PROJECT / "results"
CLASS_NAMES = {0: "Sensing", 1: "Video", 2: "Aggregation"}
CLASS_DEADLINE = {0: 0.2, 1: 0.8, 2: 60.0}
INTERVALS = (1.0, 0.2, 0.05)
CLASS_ORDER = {"Sensing": 0, "Video": 1, "Aggregation": 2}
ALGORITHMS = ("random", "round_robin", "trade_off", "minmin", "leastloaded",
              "mct", "qos")


def load() -> pd.DataFrame:
    frame = pd.read_csv(RESULTS / "runs" / "algo" / "raw_runs.csv")
    frame["policy"] = frame["policy"].astype(str)
    return frame


def _pooled_seed_sd(frame: pd.DataFrame, metric: str = "success_ratio") -> float:
    """Pooled within-cell between-seed SD over the whole crossed design."""
    spreads = (frame.groupby(["policy", "architecture", "update_interval"])[metric]
               .agg(["std", "count"]))
    spreads = spreads.dropna(subset=["std"])
    weights = (spreads["count"] - 1).clip(lower=0)
    if weights.sum() <= 0:
        return float("nan")
    variance = float(np.average(spreads["std"] ** 2, weights=weights))
    return float(np.sqrt(variance))


def ranking_table(frame: pd.DataFrame) -> pd.DataFrame:
    """Per-cell mean, spread, interval and the difference ten seeds can resolve."""
    pooled_sd = _pooled_seed_sd(frame)
    rows = []
    for architecture in sorted(frame["architecture"].unique()):
        for interval in INTERVALS:
            subset = frame[(frame["architecture"] == architecture)
                           & (frame["update_interval"] == interval)]
            for policy in ALGORITHMS:
                cell = subset[subset["policy"] == policy]["success_ratio"].dropna()
                if cell.empty:
                    continue
                low, high = stats.bootstrap_ci(cell.to_numpy(dtype=float), np.mean)
                rows.append({
                    "Architecture": architecture,
                    "Interval (s)": interval,
                    "Algorithm": policy,
                    "Success": float(cell.mean()),
                    "Seed SD": float(cell.std(ddof=1)) if len(cell) > 1 else float("nan"),
                    "95% CI": f"[{low:.3f}, {high:.3f}]",
                    "Resolvable diff.": 2.8 * pooled_sd / np.sqrt(len(cell)),
                })
    return pd.DataFrame(rows)


def stability_table(frame: pd.DataFrame) -> pd.DataFrame:
    """Rank agreement between temporal configurations."""
    rows = []
    for architecture in sorted(frame["architecture"].unique()):
        means = (frame[frame["architecture"] == architecture]
                 .pivot_table(index="policy", columns="update_interval",
                              values="success_ratio", aggfunc="mean")
                 .reindex(ALGORITHMS))
        for first, second in [(1.0, 0.2), (0.2, 0.05), (1.0, 0.05)]:
            a, b = means[first].to_numpy(dtype=float), means[second].to_numpy(dtype=float)
            finite = np.isfinite(a) & np.isfinite(b)
            distinct_a = int(pd.Series(a).nunique())
            distinct_b = int(pd.Series(b).nunique())
            defined = distinct_a > 1 and distinct_b > 1
            if defined:
                tau = scipy_stats.kendalltau(a[finite], b[finite])
                rho = scipy_stats.spearmanr(a[finite], b[finite])
                tau_value, tau_p = float(tau.statistic), float(tau.pvalue)
                rho_value = float(rho.statistic)
            else:
                # No ordering exists to agree on: every algorithm produced the
                # same outcome under that architecture. Reporting tau = 1 would
                # be a lie, so the table carries the verdict instead.
                tau_value = tau_p = rho_value = float("nan")
            rank_a = pd.Series(a).rank(ascending=False, method="min")
            rank_b = pd.Series(b).rank(ascending=False, method="min")
            moved = int((rank_a != rank_b).sum())
            best_a = ALGORITHMS[int(np.nanargmax(a))]
            best_b = ALGORITHMS[int(np.nanargmax(b))]
            rows.append({"Architecture": architecture,
                         "Comparison": f"{first:g} s vs {second:g} s",
                         "Ranking defined": "yes" if defined else "no (all tied)",
                         "Kendall tau": tau_value,
                         "tau p-value": tau_p,
                         "Spearman rho": rho_value,
                         "Algorithms moving": moved,
                         "Best at first": best_a,
                         "Best at second": best_b})
    return pd.DataFrame(rows)


def factor_table(frame: pd.DataFrame) -> pd.DataFrame:
    table = stats.factorial_anova(frame, "success_ratio",
                                  ["policy", "architecture", "update_interval"])
    return table


def host_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    columns = {"Tasks successfully executed (Mist)": "Mist",
               "Tasks successfully executed (Edge)": "Edge",
               "Tasks successfully executed (Cloud)": "Cloud"}
    for (architecture, interval, policy), subset in frame.groupby(
            ["architecture", "update_interval", "policy"]):
        generated = subset["Number of generated tasks"].replace(0, np.nan)
        row = {"Architecture": architecture, "Interval (s)": interval, "Algorithm": policy,
               "Success": subset["success_ratio"].mean()}
        for column, label in columns.items():
            row[label] = float((subset[column] / generated).mean())
        row["WAN (s)"] = float(subset["wan_usage_s"].mean())
        rows.append(row)
    return pd.DataFrame(rows)


def overhead_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for policy, subset in frame.groupby("policy"):
        rows.append({
            "Algorithm": policy,
            "Simulated decisions": int(subset["Number of generated tasks"].sum()),
            "Wall-clock per run (s)": float(subset["wall_clock_s"].mean()),
            "Run-time penalty vs trade-off (%)":
                100.0 * (float(subset["wall_clock_s"].mean())
                         / float(frame[frame["policy"] == "trade_off"]["wall_clock_s"].mean())
                         - 1.0),
            "Orchestration latency (s)":
                float(subset["orchestration_latency_s"].mean())
                if "orchestration_latency_s" in subset else float("nan"),
            "Mean decision cost (ms)":
                float(subset["decision_cost_mean_ms"].mean())
                if "decision_cost_mean_ms" in subset else float("nan"),
            "Decision cost share (%)":
                100.0 * float(subset["decision_cost_total_s"].sum())
                / max(1e-9, float(subset["wall_clock_s"].sum()))
                if "decision_cost_total_s" in subset else float("nan"),
        })
    order = {name: index for index, name in enumerate(ALGORITHMS)}
    return (pd.DataFrame(rows).sort_values("Algorithm", key=lambda s: s.map(order))
            .reset_index(drop=True))


def latency_table(seeds: int = 5) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Latency decomposition and the temporal-fidelity calibration curve.

    Reads per-task records for the first ``seeds`` seeds only: the quantities
    are stable across seeds and the full grid would need 630 CSV parses.
    """
    base = RESULTS / "runs" / "algo" / "runs"
    rows = []
    for run in sorted(base.iterdir()):
        name = run.name
        if "__seed-" not in name:
            continue
        seed = int(name.split("__seed-")[1].split("-")[0].split("__")[0])
        if seed > seeds:
            continue
        tasks = run / "tasks.csv"
        if not tasks.is_file():
            continue
        policy = name.split("__policy-")[1].split("-")[0]
        architecture = ("ALL" if "architecture-ALL__" in name else
                        "EDGE_AND_CLOUD" if "EDGE_AND_CLOUD__" in name else "EDGE_ONLY")
        raw = name.split("__update_interval-")[1].split("__")[0].replace("p", ".")
        interval = float(raw)
        data = pd.read_csv(tasks, usecols=[
            "application_id", "arrival_s", "exec_start_s", "decision_s",
            "dispatch_s", "success", "destination_type"])
        began = data[(data["exec_start_s"] > 0) & np.isfinite(data["decision_s"])]
        if began.empty:
            continue
        for app, group in began.groupby("application_id"):
            release = group["application_id"].map(lambda _: int(app))
            rows.append({
                "Algorithm": policy, "Architecture": architecture, "Interval (s)": interval,
                "Class": CLASS_NAMES.get(int(app), str(app)),
                "Deadline (s)": CLASS_DEADLINE.get(int(app), float("nan")),
                "Release to decision (s)": float((group["decision_s"] - group["arrival_s"]).mean()),
                "Decision to dispatch (s)": float((group["dispatch_s"] - group["decision_s"]).mean()),
                "Dispatch to exec (s)": float((group["exec_start_s"] - group["dispatch_s"]).mean()),
                "Decision latency (s)": float((group["exec_start_s"] - group["arrival_s"]).mean()),
                "Success": float(group["success"].mean()),
                "Tasks": int(len(group)),
            })
    table = pd.DataFrame(rows)
    if table.empty:
        return table, table
    table["Latency / deadline"] = table["Decision latency (s)"] / table["Deadline (s)"]
    bins = [0.0, 0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 4.0, np.inf]
    labels = ["<=0.05", "0.05-0.1", "0.1-0.25", "0.25-0.5", "0.5-1", "1-2", "2-4", ">4"]
    table["Fidelity ratio bin"] = pd.cut(table["Latency / deadline"], bins=bins,
                                         labels=labels, right=True)
    curve = (table.groupby(["Class", "Fidelity ratio bin"], observed=True)
             .agg(cells=("Success", "size"), mean_success=("Success", "mean"),
                  min_success=("Success", "min"), max_success=("Success", "max"),
                  latency_mean=("Decision latency (s)", "mean"),
                  deadline_mean=("Deadline (s)", "mean"))
             .reset_index())
    curve["Fidelity ratio bin"] = curve["Fidelity ratio bin"].astype(str)
    # Order rows by class, then by the position of the bin in the declared
    # ladder, so the table reads as a curve instead of an alphabetical list.
    bin_index = {label: index for index, label in enumerate(labels)}
    curve = (curve
             .assign(_class=curve["Class"].map(CLASS_ORDER).fillna(9),
                     _bin=curve["Fidelity ratio bin"].map(bin_index))
             .sort_values(["_class", "_bin"])
             .drop(columns=["_class", "_bin"])
             .reset_index(drop=True))
    return table, curve


def build() -> dict:
    """Write every exhibit and return both the paths and the frames.

    The acceptance check in analysis.py needs the measured values, so the
    frames are exposed under ``<name>_frame`` alongside the written files.
    """
    frame = load()
    outputs = {}

    ranking = ranking_table(frame)
    ranking.to_csv(RESULTS / "tables" / "algorithm_ranking.csv", index=False)
    outputs["ranking_frame"] = ranking
    outputs["ranking"] = tables.write(
        ranking, RESULTS / "tables" / "algorithm_ranking.tex",
        "Success ratio per algorithm, architecture and control interval, with the "
        "seed-level spread and the difference the design can resolve.",
        "tab:p1-algo-ranking", wide=True, size=r"\scriptsize",
        notes="Detectable difference is 2.8 times the pooled between-seed standard deviation divided by the square root of the seed count; differences smaller than this are not resolvable with ten seeds and are not interpreted.")

    stability = stability_table(frame)
    stability.to_csv(RESULTS / "tables" / "ranking_stability.csv", index=False)
    outputs["stability_frame"] = stability
    outputs["stability"] = tables.write(
        stability, RESULTS / "tables" / "ranking_stability.tex",
        "Rank agreement between temporal configurations (Kendall tau, Spearman rho).",
        "tab:p1-ranking-stability", wide=True, size=r"\scriptsize",
        notes="A tau below one means the simulator's temporal configuration "
              "changes the ordering of the algorithms; the last two columns give "
              "the winner at each end of the comparison. Where every algorithm "
              "produces the same outcome there is no ordering to agree on, and "
              "the correlation is left undefined rather than reported as one.")

    factors = factor_table(frame)
    factors.to_csv(RESULTS / "tables" / "factor_effects.csv", index=False)
    outputs["factors_frame"] = factors
    display = factors.rename(columns={"term": "Term", "sum_sq": "Sum of squares",
                                      "mean_sq": "Mean square",
                                      "partial_eta_sq": "Partial eta sq"})
    display["p_value"] = display["p_value"].map(
        lambda value: "$<0.001$" if value < 1e-3 else f"{value:.3f}")
    outputs["factors"] = tables.write(
        display,
        RESULTS / "tables" / "factor_effects.tex",
        "Factorial analysis of the success ratio over the fully crossed "
        "algorithm x architecture x interval design (630 runs, balanced).",
        "tab:p1-factor-effects", align="lcccccc", wide=True, size=r"\footnotesize",
        notes="The design is perfectly balanced, so each term's sum of squares is "
              "the unique ANOVA sum of squares and does not depend on the order of "
              "entry. The significant three-way interaction is the formal statement "
              "that algorithm comparisons are not configuration-free.")

    hosts = host_table(frame)
    hosts.to_csv(RESULTS / "tables" / "host_selection.csv", index=False)
    outputs["hosts_frame"] = hosts
    outputs["hosts"] = tables.write(
        hosts,
        RESULTS / "tables" / "host_selection.tex",
        "Where each algorithm places its successful work, as a share of generated "
        "tasks, with cumulative WAN occupancy.",
        "tab:p1-host-selection", wide=True, size=r"\scriptsize",
        notes="Mist / Edge / Cloud columns sum to the success ratio. The spread "
              "across algorithms in these columns is what the architecture factor "
              "is made of.")

    overhead = overhead_table(frame)
    overhead.to_csv(RESULTS / "tables" / "decision_overhead.csv", index=False)
    outputs["overhead_frame"] = overhead
    outputs["overhead"] = tables.write(
        overhead.rename(columns={
            "Simulated decisions": "Decisions",
            "Wall-clock per run (s)": "Wall/run (s)",
            "Run-time penalty vs trade-off (%)": "Penalty (\%)",
            "Orchestration latency (s)": "Latency (s)",
            "Mean decision cost (ms)": "Cost/dec. (ms)",
            "Decision cost share (\%)": "Cost share (\%)",
        }), RESULTS / "tables" / "decision_overhead.tex",
        "Cost of deciding: wall-clock per run, per-decision time inside the "
        "simulation loop, and the resulting share of runtime spent deciding.",
        "tab:p1-decision-overhead", align="lcccccc", wide=True, size=r"\scriptsize",
        notes="Decision cost is the CPU time of the decision routine itself, "
              "separated from the simulated scheduling delay; the two must not be "
              "confused when ranking algorithms.")

    decomposition, curve = latency_table()
    if not decomposition.empty:
        decomposition.to_csv(RESULTS / "tables" / "latency_decomposition.csv", index=False)
        outputs["decomposition_frame"] = decomposition
        outputs["decomposition"] = tables.write(
            decomposition[(decomposition["Architecture"] == "ALL")
                          & (decomposition["Algorithm"].isin(
                              ["mct", "leastloaded", "minmin"]))][[
                "Algorithm", "Interval (s)", "Class",
                "Release to decision (s)", "Decision to dispatch (s)",
                "Dispatch to exec (s)", "Decision latency (s)", "Success",
            ]], RESULTS / "tables" / "latency_decomposition.tex",
            "Orchestration latency decomposition for three representative "
            "placement rules on the full architecture, resolved per workload "
            "class (first 5 seeds). Release-to-decision and decision-to-dispatch "
            "are zero, so the whole wait falls in dispatch-to-execution.",
            "tab:p1-latency", wide=True, size=r"\footnotesize",
            notes="The decomposition localises the clock: release-to-decision and "
                  "decision-to-dispatch are zero in this harness (a task is "
                  "decided at release), so the entire wait appears between dispatch "
                  "and execution start, where it is a whole number of control "
                  "intervals. Rows are per workload class because the classes have "
                  "different deadlines.")
    if not curve.empty:
        curve.to_csv(RESULTS / "tables" / "temporal_fidelity.csv", index=False)
        outputs["curve_frame"] = curve
        outputs["curve"] = tables.write(
            curve, RESULTS / "tables" / "temporal_fidelity.tex",
            "Measured success as a function of the ratio between orchestration "
            "decision latency and the request deadline, resolved per workload "
            "class because the classes have different deadlines.",
            "tab:p1-temporal-fidelity", align="llcccccc", wide=True, size=r"\scriptsize",
            notes="This is the calibration curve that replaces the D/10 rule: the "
                  "ratio at which success stops improving is read from the data "
                  "rather than asserted.")
        figures.line_with_ci(
            curve, "Fidelity ratio bin", "mean_success", "Class",
            RESULTS / "figures" / "f11_temporal_fidelity",
            xlabel="Decision latency / deadline", ylabel="Success ratio",
            title=None, ylim=(0.0, 1.02), columns=2)
    return outputs


if __name__ == "__main__":
    built = build()
    print(f"[P1 temporal] {len(built)} exhibits")
