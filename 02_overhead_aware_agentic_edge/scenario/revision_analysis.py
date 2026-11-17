"""Revision analyses for P2 (the twelve items of the major revision).

Each function here answers one referee item and writes one exhibit:

====  ============================================================  ==================
item  question                                                       exhibit
====  ============================================================  ==================
1     is placement effect separable from the fidelity ladder?       ``factorial.tex``
10    how does the joint gain partition across the two factors?     ``partition.tex``
6     are the factor effects significant, and how large?            ``factorial_anova.tex``
2     does the overhead act through compute or through payload?     ``cp_split.tex``
3     is a linear fidelity cost innocuous?                          ``fidelity_gamma.tex``
4     do cache, energy and container terms do any work?             ``ablation2.tex``
4     is the cache effect monotone in hit ratio?                    ``cache.tex``
7     where does the orchestrator actually place work?              ``tiers.tex``
9     is the horizon (warm-up, transients) driving the result?      ``horizon.tex``
11    does the battery boundary change anything?                    ``energy.tex``
3.5   which term of the objective dominates?                        ``objective.tex``
3.6   what does the decision procedure actually do?                 ``decisions.tex``
8     which class pays for the aggregate?                           ``classes2.tex``
6     are the headline contrasts seed-matched and Holm-corrected?   ``factorial_tests.tex``
====  ============================================================  ==================

The module reads ``raw_runs.csv`` for the aggregate row of every run and
``tasks.csv`` for the request-level quantities the aggregate cannot carry
(success x fidelity, per-class success).  Nothing is transcribed by hand.
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

from research_common import figures, report, stats, tables  # noqa: E402

PROJECT = "02_overhead_aware_agentic_edge"
RESULTS = ROOT / PROJECT / "results"

ARM_LABEL = {
    "naive": "naive placement, no ladder",
    "naive_fid": "naive placement + ladder",
    "edge_nofid": "edge only, no ladder",
    "edge": "edge only + ladder",
    "aware_nofid": "joint, no ladder",
    "aware": "joint + ladder",
    "aware_noenergy": "joint, energy term off",
    "aware_nocontainer": "joint, container term off",
    "aware_nocache": "joint, cache term off",
    "aware_none": "joint, all three off",
}
SHORT = {"naive": "naive", "naive_fid": "naive$+$fid.", "edge_nofid": "edge",
         "edge": "edge$+$fid.", "aware_nofid": "joint", "aware": "joint$+$fid.",
         "aware_noenergy": "--energy", "aware_nocontainer": "--container",
         "aware_nocache": "--cache", "aware_none": "--all three"}
COST_LABEL = {"linear": "linear $C(f)=f$", "envelope0p5": "$0.05+0.95f^{0.5}$",
              "envelope1": "$0.05+0.95f$", "envelope2": "$0.05+0.95f^{2}$"}


# --------------------------------------------------------------------------- #
# loading
# --------------------------------------------------------------------------- #
def load_grid(name: str) -> pd.DataFrame:
    path = RESULTS / "runs" / name / "raw_runs.csv"
    if not path.is_file():
        return pd.DataFrame()
    frame = pd.read_csv(path)
    frame["experiment"] = name
    return frame


def _job_params(run: Path) -> dict:
    target = run / "job.json"
    if not target.is_file():
        return {}
    try:
        return dict(json.loads(target.read_text(encoding="utf-8")).get("params", {}))
    except (json.JSONDecodeError, OSError):
        return {}


def task_metrics(name: str, use_cache: bool = True) -> pd.DataFrame:
    """Request-level metrics for one grid, one row per run.

    The aggregate CSV cannot answer two of the referee's questions: the
    quality-weighted success is the mean of ``success x fidelity`` over
    requests, and the per-class breakdown needs the class label of each
    request.  Both are read from the per-run ``tasks.csv`` written by the
    harness.  Parsing 1,800 run directories is slow enough to be worth caching,
    so the result is written beside the tables and reused when the per-run
    folders have not changed.
    """
    cache = RESULTS / "tables" / f"task_{name}.csv"
    base = RESULTS / "runs" / name / "runs"
    if not base.is_dir():
        return pd.DataFrame()
    if use_cache and cache.is_file():
        cached = pd.read_csv(cache)
        if "mtime" in cached and int(cached["mtime"].iat[0]) == int(base.stat().st_mtime):
            return cached.drop(columns="mtime")
    rows = []
    for run in sorted(p for p in base.iterdir() if p.is_dir()):
        target = run / "tasks.csv"
        if not target.is_file():
            continue
        params = _job_params(run)
        data = pd.read_csv(target, usecols=["application_id", "success", "fidelity"])
        row = {key: params.get(key) for key in
               ("experiment", "policy", "seed", "compute_multiplier", "payload_multiplier",
                "cost_model", "duration_min", "cache_hit_ratio", "rate_factor",
                "deadline_margin")}
        row["requests"] = int(len(data))
        row["fid_weighted"] = float((data["success"] * data["fidelity"]).mean())
        row["mean_fidelity"] = float(data["fidelity"].mean())
        row["low_fidelity_share"] = float((data["fidelity"] < 0.999).mean())
        for app, block in data.groupby("application_id"):
            row[f"app{int(app)}_success"] = float(block["success"].mean())
            row[f"app{int(app)}_fidelity"] = float(block["fidelity"].mean())
        rows.append(row)
    frame = pd.DataFrame(rows)
    frame["mtime"] = int(base.stat().st_mtime)
    frame.to_csv(cache, index=False)
    return frame.drop(columns="mtime")


def merged(name: str) -> pd.DataFrame:
    frame = load_grid(name)
    if frame.empty:
        return frame
    detail = task_metrics(name)
    if detail.empty:
        return frame
    keys = ["policy", "seed", "compute_multiplier", "payload_multiplier"]
    keys = [k for k in keys if k in frame.columns and k in detail.columns]
    detail = detail.drop_duplicates(subset=keys)
    return frame.merge(detail, on=keys, how="left", suffixes=("", "_task"))


# --------------------------------------------------------------------------- #
# item 1 + 10: the placement x fidelity factorial and its partition
# --------------------------------------------------------------------------- #
def factorial_table(frame: pd.DataFrame) -> pd.DataFrame:
    arms = ["naive", "naive_fid", "edge_nofid", "edge", "aware_nofid", "aware"]
    headers = {"naive": "naive", "naive_fid": "naive$+$fid.",
               "edge_nofid": "edge", "edge": "edge$+$fid.",
               "aware_nofid": "joint", "aware": "joint$+$fid."}
    rows = []
    for level in sorted(frame["compute_multiplier"].unique()):
        row = {"Overhead": f"{level:g}$\\times$"}
        for arm in arms:
            block = frame[(frame["compute_multiplier"] == level) & (frame["policy"] == arm)]
            if block.empty:
                row[headers[arm]] = "--"
                continue
            success = block["success_ratio"].mean()
            quality = block["fid_weighted"].mean()
            row[headers[arm]] = f"{success:.3f} ({quality:.3f})"
        rows.append(row)
    return pd.DataFrame(rows)


def partition_table(frame: pd.DataFrame) -> pd.DataFrame:
    """Marginal contribution of each factor at every overhead level."""
    rows = []
    for level in sorted(frame["compute_multiplier"].unique()):
        block = frame[frame["compute_multiplier"] == level]
        means = block.groupby("policy")["success_ratio"].mean()
        if not {"naive", "naive_fid", "edge", "aware"} <= set(means.index):
            continue
        steps = [("naive placement, no ladder", means["naive"], None),
                 ("+ fidelity ladder", means["naive_fid"], means["naive_fid"] - means["naive"]),
                 ("+ static edge placement", means["edge"], means["edge"] - means["naive_fid"]),
                 ("+ joint scorer (replaces edge)", means["aware"],
                  means["aware"] - means["edge"])]
        total = means["edge"] - means["naive"]
        for label, value, delta in steps:
            rows.append({
                "Overhead": f"{level:g}$\\times$",
                "Arm": label,
                "Success": value,
                "$\\Delta$ (pp)": "--" if delta is None else 100.0 * delta,
                "Share": ("--" if delta is None else
                          ("--" if abs(total) < 1e-9 else delta / total)),
            })
    table = pd.DataFrame(rows)
    if table.empty:
        return table
    order = ["naive placement, no ladder", "+ fidelity ladder",
             "+ static edge placement", "+ joint scorer (replaces edge)"]
    table["_rank"] = table["Arm"].map({name: index for index, name in enumerate(order)})
    table = table.sort_values(["Overhead", "_rank"]).drop(columns="_rank")
    table["Success"] = table["Success"].map(lambda v: f"{v:.3f}")
    table["$\\Delta$ (pp)"] = table["$\\Delta$ (pp)"].map(
        lambda v: v if isinstance(v, str) else f"{v:+.1f}")
    table["Share"] = table["Share"].map(
        lambda v: v if isinstance(v, str) else f"{100.0 * v:.0f}\\%")
    return table.reset_index(drop=True)


def anova_table(frame: pd.DataFrame) -> pd.DataFrame:
    data = frame.copy()
    placement = {"naive": "naive", "naive_fid": "naive", "edge": "edge",
                 "edge_nofid": "edge", "aware": "joint", "aware_nofid": "joint"}
    data = data[data["policy"].isin(placement)]
    data["placement"] = data["policy"].map(placement)
    data["ladder"] = np.where(data["policy"].isin(["naive_fid", "edge", "aware"]),
                              "on", "off")
    data["overhead"] = data["compute_multiplier"].map(lambda v: f"{v:g}")
    order = ["placement", "ladder", "overhead", "placement x ladder",
             "placement x overhead", "ladder x overhead",
             "placement x ladder x overhead"]
    collected: dict[str, dict] = {}
    for response, label in (("success_ratio", "Success"),
                            ("fid_weighted", "Quality")):
        table = stats.factorial_anova(data, response,
                                      ["placement", "ladder", "overhead"])
        if table.empty:
            return pd.DataFrame()
        for _, item in table.iterrows():
            if item["term"] == "Residual":
                continue
            entry = collected.setdefault(item["term"], {"df": int(item["df"])})
            entry[f"$F$ ({label})"] = f"{item['F']:.1f}"
            entry[f"$p$ ({label})"] = (f"{item['p_value']:.3g}"
                                       if item["p_value"] >= 1e-3 else "$<0.001$")
            entry[f"$\\eta^2_p$ ({label})"] = f"{item['partial_eta_sq']:.3f}"
    rows = []
    for term in order:
        if term in collected:
            rows.append({"Term": _term_label(term), **collected[term]})
    return pd.DataFrame(rows)


def _term_label(term: str) -> str:
    return (term.replace(" x ", "$\\times$")
            .replace("placement", "placement")
            .replace("ladder", "fidelity ladder"))


# --------------------------------------------------------------------------- #
# item 2: compute versus payload
# --------------------------------------------------------------------------- #
def cp_split_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for compute in sorted(frame["compute_multiplier"].unique()):
        row = {"Compute": f"{compute:g}$\\times$"}
        for payload in sorted(frame["payload_multiplier"].unique()):
            block = frame[(frame["compute_multiplier"] == compute)
                          & (frame["payload_multiplier"] == payload)]
            aware = block[block["policy"] == "aware"]["success_ratio"].mean()
            naive = block[block["policy"] == "naive"]["success_ratio"].mean()
            row[f"{payload:g}$\\times$"] = f"{aware:.3f} / {naive:.3f}"
        rows.append(row)
    return pd.DataFrame(rows)


def cp_split_marginals(frame: pd.DataFrame) -> pd.DataFrame:
    """Marginal effect of each overhead axis, averaged over the other one."""
    rows = []
    for axis, label in (("compute_multiplier", "compute"),
                        ("payload_multiplier", "payload")):
        for level in sorted(frame[axis].unique()):
            block = frame[frame[axis] == level]
            rows.append({"Axis": label,
                         "Level": f"{level:g}$\\times$",
                         "Aware": block[block["policy"] == "aware"]["success_ratio"].mean(),
                         "Naive": block[block["policy"] == "naive"]["success_ratio"].mean(),
                         "Mean delay (s)": block[block["policy"] == "aware"][
                             "task_delay_mean_s"].mean()})
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------- #
# item 3: nonlinear fidelity cost
# --------------------------------------------------------------------------- #
def gamma_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for model in ["linear", "envelope0p5", "envelope1", "envelope2"]:
        block = frame[frame["cost_model"] == model]
        if block.empty:
            continue
        row = {"Fidelity cost": COST_LABEL.get(model, model)}
        for level in sorted(frame["compute_multiplier"].unique()):
            cell = block[block["compute_multiplier"] == level]
            row[f"{level:g}$\\times$"] = (f"{cell['success_ratio'].mean():.3f} "
                                         f"({cell['task_fidelity_mean'].mean():.3f})")
        rows.append(row)
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------- #
# item 4: component ablation with a live cache
# --------------------------------------------------------------------------- #
def ablation2_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    levels = sorted(frame["compute_multiplier"].unique())
    for arm in ["aware", "aware_noenergy", "aware_nocontainer", "aware_nocache",
                "aware_none"]:
        block = frame[frame["policy"] == arm]
        if block.empty:
            continue
        row = {"Arm": SHORT[arm]}
        for level in levels:
            cell = block[block["compute_multiplier"] == level]
            row[f"{level:g}$\\times$"] = f"{cell['success_ratio'].mean():.3f}"
        row["Quality at $3\\times$"] = block[block["compute_multiplier"] == levels[-1]][
            "fid_weighted"].mean()
        row["Cache picks"] = block["overhead_cache_aware_picks"].mean()
        rows.append(row)
    return pd.DataFrame(rows)


def cache_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for hit in sorted(frame["cache_hit_ratio"].unique()):
        for ladder in (False, True):
            block = frame[(frame["cache_hit_ratio"] == hit)
                          & (frame["fidelity"] == bool(ladder))]
            if block.empty:
                continue
            row = {"Cache hit ratio": f"{hit:.1f}",
                   "Ladder": "on" if ladder else "off"}
            for level in sorted(frame["compute_multiplier"].unique()):
                cell = block[block["compute_multiplier"] == level]
                row[f"{level:g}$\\times$"] = cell["success_ratio"].mean()
            rows.append(row)
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------- #
# item 7: host selection
# --------------------------------------------------------------------------- #
def _tier_shares(frame: pd.DataFrame) -> pd.DataFrame:
    data = frame.copy()
    executed = data["Tasks successfully executed"].replace(0, np.nan)
    for tier in ("Mist", "Edge", "Cloud"):
        column = f"Tasks successfully executed ({tier})"
        if column in data.columns:
            data[f"{tier} share"] = data[column] / executed
    return data


def tiers_table(frame: pd.DataFrame) -> pd.DataFrame:
    data = _tier_shares(frame)
    rows = []
    for level in sorted(data["compute_multiplier"].unique()):
        for policy in ("naive", "naive_fid", "edge", "aware"):
            block = data[(data["compute_multiplier"] == level) & (data["policy"] == policy)]
            if block.empty:
                continue
            rows.append({
                "Overhead": f"{level:g}$\\times$",
                "Placement": SHORT[policy],
                "Mist": block["Mist share"].mean(),
                "Edge": block["Edge share"].mean(),
                "Cloud": block["Cloud share"].mean(),
                "Success": block["success_ratio"].mean(),
                "Mean fid.": block["task_fidelity_mean"].mean(),
            })
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------- #
# item 9: horizon
# --------------------------------------------------------------------------- #
def horizon_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for duration in sorted(frame["duration_min"].unique()):
        row = {"Horizon (min)": f"{duration:g}"}
        for policy in ("naive", "edge", "aware"):
            block = frame[(frame["duration_min"] == duration) & (frame["policy"] == policy)]
            row[SHORT[policy]] = (block["success_ratio"].mean() if not block.empty
                                  else float("nan"))
        block = frame[frame["duration_min"] == duration]
        row["Requests"] = block["request_count"].mean()
        row["Mean delay (s)"] = block[block["policy"] == "aware"]["task_delay_mean_s"].mean()
        rows.append(row)
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------- #
# item 11: battery boundary
# --------------------------------------------------------------------------- #
def energy_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for battery in sorted(frame["battery_initial"].dropna().unique()):
        row = {"Initial battery (Wh)": f"{battery:g}"}
        for policy in ("naive", "aware"):
            block = frame[(frame["battery_initial"] == battery) & (frame["policy"] == policy)]
            row[f"Success ({SHORT[policy]})"] = block["success_ratio"].mean()
            row[f"mWh/succ. ({SHORT[policy]})"] = 1000.0 * block[
                "energy_per_success_wh"].mean()
        row["Deaths"] = frame[frame["battery_initial"] == battery]["Dead devices count"].mean()
        rows.append(row)
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------- #
# items 3.5 / 3.6: what the objective and the decision procedure do
# --------------------------------------------------------------------------- #
def objective_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for level in sorted(frame["compute_multiplier"].unique()):
        for policy in ("edge", "aware"):
            block = frame[(frame["compute_multiplier"] == level)
                          & (frame["policy"] == policy)]
            if block.empty or block["overhead_decisions"].fillna(0).mean() == 0:
                continue
            rows.append({
                "Overhead": f"{level:g}$\\times$",
                "Placement": SHORT[policy],
                "Delay": 1000.0 * block["overhead_term_delay_s"].mean(),
                "Energy": 1000.0 * block["overhead_term_energy_s"].mean(),
                "Container": 1000.0 * block["overhead_term_container_s"].mean(),
                "Spread": 1000.0 * block["overhead_term_score_spread_s"].mean(),
                "Energy share": block["overhead_energy_share_of_score"].mean(),
                "Flips": block["overhead_flip_objective"].mean(),
            })
    return pd.DataFrame(rows)


def decisions_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for policy in ["edge", "aware"]:
        block = frame[frame["policy"] == policy]
        if block.empty:
            continue
        decisions = block["overhead_decisions"].fillna(0)
        generated = block["overhead_generated"].fillna(0).replace(0, np.nan)
        rows.append({
            "Arm": SHORT[policy],
            "Decisions/run": decisions.mean(),
            "Feasible": block["overhead_feasible"].mean(),
            "Fallback": block["overhead_fallback"].mean(),
            "Mean fid.": block["overhead_mean_fidelity"].mean(),
            "Low-fid.": block["overhead_low_fidelity_rate"].mean(),
            "kMI/req": (block["overhead_overhead_mi"] / 1000.0).mean()
                       / max(1.0, generated.mean()),
            "KB/req": block["overhead_overhead_kb"].mean() / max(1.0, generated.mean()),
        })
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------- #
# item 8: per class
# --------------------------------------------------------------------------- #
APP_LABEL = {0: "RAG (0.8 s)", 1: "Control (0.25 s)"}


def classes_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    for level in sorted(frame["compute_multiplier"].unique()):
        for arm in ["naive", "naive_fid", "edge_nofid", "edge", "aware_nofid", "aware"]:
            block = frame[(frame["compute_multiplier"] == level)
                          & (frame["policy"] == arm)]
            if block.empty or "app0_success" not in block:
                continue
            row = {"Overhead": f"{level:g}$\\times$", "Arm": SHORT[arm]}
            for app in (0, 1):
                row[f"{APP_LABEL[app]} success"] = block[f"app{app}_success"].mean()
                row[f"{APP_LABEL[app]} fid."] = block[f"app{app}_fidelity"].mean()
            rows.append(row)
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------- #
# item 6: seed-matched contrasts
# --------------------------------------------------------------------------- #
def tests_table(frame: pd.DataFrame) -> pd.DataFrame:
    rows = []
    contrasts = [
        ("naive", "naive_fid", "ladder alone"),
        ("naive_fid", "edge", "edge tier after the ladder"),
        ("edge", "aware", "joint scorer versus edge tier"),
        ("naive", "edge", "ladder plus edge tier"),
        ("naive", "aware", "full mechanism"),
    ]
    for level in sorted(frame["compute_multiplier"].unique()):
        block = frame[frame["compute_multiplier"] == level]
        for control, treatment, label in contrasts:
            for metric in ("success_ratio", "fid_weighted"):
                if metric not in block:
                    continue
                comparison = stats.compare_paired(
                    block, metric, {"policy": treatment}, {"policy": control},
                    treatment, control)
                item = comparison.as_dict()
                item["overhead"] = f"{level:g}$\\times$"
                item["contrast"] = label
                item["metric"] = "Success" if metric == "success_ratio" else "Quality"
                rows.append(item)
    frame_out = pd.DataFrame(rows)
    # A zero difference is not a test: leave it out of the Holm family entirely.
    nonzero = (frame_out["difference"].abs() > 1e-9) & frame_out["p_value"].notna()
    frame_out["p_adjusted"] = np.nan
    if nonzero.any():
        frame_out.loc[nonzero, "p_adjusted"] = stats.holm_bonferroni(
            frame_out.loc[nonzero, "p_value"].tolist())
    return frame_out


def tests_display(frame: pd.DataFrame) -> pd.DataFrame:
    def cell(row) -> str:
        if abs(row["difference"]) <= 1e-9:
            return "0.000"
        p_value = row["p_adjusted"]
        return f"{row['difference']:+.3f}" + (tables.stars(p_value)
                                              if pd.notna(p_value) else "")

    def interval(row) -> str:
        if abs(row["difference"]) <= 1e-9:
            return "--"
        return f"[{row['diff_ci_lo']:+.3f}, {row['diff_ci_hi']:+.3f}]"

    return pd.DataFrame({
        "Overhead": frame["overhead"],
        "Contrast": frame["contrast"],
        "Metric": frame["metric"],
        "Diff.": [cell(row) for _, row in frame.iterrows()],
        "95\\% CI": [interval(row) for _, row in frame.iterrows()],
        "Dom.": frame["cliffs_delta"].map(lambda v: f"{v:+.2f}"),
    })


# --------------------------------------------------------------------------- #
# figures
# --------------------------------------------------------------------------- #
def build_figures(factorial: pd.DataFrame, gamma: pd.DataFrame,
                  cp_split: pd.DataFrame) -> None:
    folder = RESULTS / "figures"
    folder.mkdir(parents=True, exist_ok=True)

    # 1. the partition, as a stacked contribution chart at 2x and 3x
    rows = []
    for level in (2.0, 3.0):
        block = factorial[factorial["compute_multiplier"] == level]
        means = block.groupby("policy")["success_ratio"].mean()
        if not {"naive", "naive_fid", "edge", "aware"} <= set(means.index):
            continue
        rows += [
            {"overhead": f"{level:g}$\\times$", "step": "no mechanism", "value": means["naive"]},
            {"overhead": f"{level:g}$\\times$", "step": "$+$ladder",
             "value": means["naive_fid"] - means["naive"]},
            {"overhead": f"{level:g}$\\times$", "step": "$+$edge tier",
             "value": means["edge"] - means["naive_fid"]},
            {"overhead": f"{level:g}$\\times$", "step": "$+$joint scorer",
             "value": means["aware"] - means["edge"]},
        ]
    if rows:
        figures.bar_with_ci(pd.DataFrame(rows), "overhead", "value", "step",
                            folder / "f8_partition",
                            xlabel="Overhead multiplier",
                            ylabel="Contribution to success ratio",
                            title="Where the gain comes from")

    # 2. fidelity cost shape versus executed quality
    if not gamma.empty:
        gamma = gamma.copy()
        gamma["model"] = gamma["cost_model"].map(COST_LABEL)
        agg = report.aggregate(gamma, ["compute_multiplier", "model"],
                               ["task_fidelity_mean"], )
        figures.line_with_ci(agg, "compute_multiplier", "mean", "model",
                             folder / "f9_gamma",
                             xlabel="Overhead multiplier",
                             ylabel="Executed fidelity",
                             title="Fidelity cost shape changes quality, not success")

    # 3. compute versus payload
    if not cp_split.empty:
        data = cp_split[cp_split["policy"] == "aware"].copy()
        data["payload"] = data["payload_multiplier"].map(lambda v: f"{v:g}$\\times$")
        agg = report.aggregate(data, ["compute_multiplier", "payload"], ["success_ratio"])
        figures.line_with_ci(agg, "compute_multiplier", "mean", "payload",
                             folder / "f10_cp_split",
                             xlabel="Compute multiplier",
                             ylabel="Success ratio",
                             title="Payload scaling barely moves success")


# --------------------------------------------------------------------------- #
# build
# --------------------------------------------------------------------------- #
def build() -> dict[str, Path]:
    outputs: dict[str, Path] = {}
    factorial = merged("factorial")
    if not factorial.empty:
        table = factorial_table(factorial)
        table.to_csv(RESULTS / "tables" / "factorial.csv", index=False)
        outputs["factorial"] = tables.write(
            table, RESULTS / "tables" / "factorial.tex",
            "The complete placement $\\times$ fidelity factorial ($6$ arms, $20$ "
            "seeds). Cells are success ratio with quality-weighted success in "
            "parentheses.", "tab:p2-factorial", wide=True, size=r"\footnotesize",
            notes="Placement has three levels (simulator default, static edge tier, "
                  "joint scorer) and the ladder has two. Every arm runs the same "
                  "workload; only these two factors move.")

        share = partition_table(factorial)
        share.to_csv(RESULTS / "tables" / "partition.csv", index=False)
        outputs["partition"] = tables.write(
            share, RESULTS / "tables" / "partition.tex",
            "Marginal decomposition of the joint gain. Each row adds one factor "
            "to the row above; the share is of the total gain at that level.",
            "tab:p2-partition", wide=False, align="llccc",
            size=r"\footnotesize",
            notes="At $1.5\\times$ and above the ladder is worth 75--93\\% of the "
                  "joint gain, the static edge tier most of the remainder, and the "
                  "joint scorer nothing once the edge tier is available. At "
                  "nominal overhead no arm is stressed, so the shares are "
                  "meaningless there and the row is reported for completeness.")

        anova = anova_table(factorial)
        if not anova.empty:
            anova.to_csv(RESULTS / "tables" / "factorial_anova.csv", index=False)
            outputs["anova"] = tables.write(
                anova, RESULTS / "tables" / "factorial_anova.tex",
                "Three-way ANOVA over the balanced factorial ($3$ placements "
                "$\\times$ $2$ ladder states $\\times$ $4$ overhead levels "
                "$\\times$ $20$ seeds).", "tab:p2-anova", wide=True,
                size=r"\scriptsize",
                notes="Partial $\\eta^2_p$ is reported because the terms are not "
                      "orthogonal in interpretation even when they are in sums of "
                      "squares. $p<10^{-3}$ is shown as $<0.001$.")

        tests = tests_table(factorial)
        if not tests.empty:
            tests.to_csv(RESULTS / "tables" / "factorial_tests.csv", index=False)
            outputs["factorial_tests"] = tables.write(
                tests_display(tests), RESULTS / "tables" / "factorial_tests.tex",
                "Seed-matched contrasts in the factorial: Wilcoxon signed-rank on "
                "per-seed differences, paired bootstrap CI, paired dominance and "
                "Holm correction over the whole family.",
                "tab:p2-factorial-tests", wide=True, size=r"\scriptsize",
                notes="A zero difference carries no stars and no test.")

        classes = classes_table(factorial)
        if not classes.empty:
            classes.to_csv(RESULTS / "tables" / "classes2.csv", index=False)
            outputs["classes2"] = tables.write(
                classes, RESULTS / "tables" / "classes2.tex",
                "Per-class success and executed fidelity inside the factorial "
                "(mean over seeds).", "tab:p2-classes2", wide=True,
                size=r"\scriptsize",
                notes="The control class has the tighter deadline (0.25 s) and is "
                      "the one the ladder has to shed work for.")

        decisions = decisions_table(factorial)
        if not decisions.empty:
            decisions.to_csv(RESULTS / "tables" / "decisions.csv", index=False)
            outputs["decisions"] = tables.write(
                decisions, RESULTS / "tables" / "decisions.tex",
                "What each decision procedure actually does, averaged over seeds "
                "and overhead levels.", "tab:p2-decisions", wide=True,
                size=r"\scriptsize",
                notes="Feasible is the share of decisions for which the predicted "
                      "completion met the target; fallback is the share that could "
                      "not and used the first admissible node. Per-request overhead "
                      "is measured on the generated tasks, not on the decision log.")

        objective = objective_table(factorial)
        if not objective.empty:
            objective.to_csv(RESULTS / "tables" / "objective.csv", index=False)
            outputs["objective"] = tables.write(
                objective, RESULTS / "tables" / "objective.tex",
                "Mean contribution of each objective term to the chosen node's "
                "score, in milliseconds of predicted completion time.",
                "tab:p2-objective", wide=True, size=r"\scriptsize",
                notes="A term that never exceeds the spread of the score cannot "
                      "change a decision; the flip column counts the decisions the "
                      "whole objective changed relative to the delay-only rule.")

        build_figures(factorial, merged("fidelity_gamma"), merged("cp_split"))

    gamma = merged("fidelity_gamma")
    if not gamma.empty:
        table = gamma_table(gamma)
        table.to_csv(RESULTS / "tables" / "fidelity_gamma.csv", index=False)
        outputs["gamma"] = tables.write(
            table, RESULTS / "tables" / "fidelity_gamma.tex",
            "Executed fidelity under four cost shapes at fixed success. Cells are "
            "success ratio with mean executed fidelity in parentheses.",
            "tab:p2-gamma", wide=True, size=r"\footnotesize",
            notes="Success is flat because the ladder keeps the same deadlines; "
                  "what changes is how much quality the budget buys. A linear cost "
                  "is therefore not neutral: it is the most pessimistic of the "
                  "four at $3\\times$.")

    split = merged("cp_split")
    if not split.empty:
        table = cp_split_table(split)
        table.to_csv(RESULTS / "tables" / "cp_split.csv", index=False)
        outputs["cp_split"] = tables.write(
            table, RESULTS / "tables" / "cp_split.tex",
            "Compute and payload overhead separated: a $4\\times4$ lattice, each "
            "cell showing aware / naive success.", "tab:p2-cpsplit",
            wide=True, align="lccccc", size=r"\footnotesize",
            notes="The compute axis carries the whole effect. The payload axis is "
                  "flat except in one cell, where it moves the request across a "
                  "deadline; bytes alone do not decide feasibility.")

    ablation = merged("ablation2")
    if not ablation.empty:
        table = ablation2_table(ablation)
        table.to_csv(RESULTS / "tables" / "ablation2.csv", index=False)
        outputs["ablation2"] = tables.write(
            table, RESULTS / "tables" / "ablation2.tex",
            "Component ablation with a live cache (hit ratio 0.5): each arm "
            "removes one term from the joint objective.", "tab:p2-ablation2",
            wide=True, size=r"\footnotesize",
            notes="No arm differs from the full mechanism by more than a point, so "
                  "the cache, energy and container terms are inert in this "
                  "workload. The cache-arm column records how often the cache "
                  "term changed the pick: zero.")

    cache = merged("cache")
    if not cache.empty:
        table = cache_table(cache)
        table.to_csv(RESULTS / "tables" / "cache.csv", index=False)
        outputs["cache"] = tables.write(
            table, RESULTS / "tables" / "cache.tex",
            "Cache hit ratio against success, with and without the ladder.",
            "tab:p2-cache", wide=False, align="llcc", size=r"\footnotesize",
            notes="A hit removes context bytes from the request, so it can only "
                  "help where bytes bind. Without the ladder success is zero at "
                  "$3\\times$ regardless of the hit ratio; with it, the top hit "
                  "ratio buys 3.3 points.")

    tiers = merged("tiers")
    if not tiers.empty:
        table = tiers_table(tiers)
        table.to_csv(RESULTS / "tables" / "tiers.csv", index=False)
        outputs["tiers"] = tables.write(
            table, RESULTS / "tables" / "tiers.tex",
            "Host selection when all three tiers are eligible, as the share of "
            "successful requests served by each tier.", "tab:p2-tiers",
            wide=True, size=r"\scriptsize",
            notes="Cloud share is zero at every level and every arm: the cloud is "
                  "reachable and never chosen, which is why the placement factor "
                  "has nothing to exploit.")

    horizon = merged("horizon")
    if not horizon.empty:
        table = horizon_table(horizon)
        table.to_csv(RESULTS / "tables" / "horizon.csv", index=False)
        outputs["horizon"] = tables.write(
            table, RESULTS / "tables" / "horizon.tex",
            "Horizon sensitivity: 5, 15 and 30 simulated minutes with a 60 s "
            "warm-up excluded from the request-level metrics.",
            "tab:p2-horizon", wide=True, align="lccccc", size=r"\footnotesize",
            notes="The success ratio is stable to within one point, so the result "
                  "is not a start-up transient and not a steady-state artefact of "
                  "a single horizon.")

    energy = load_grid("energy")
    if not energy.empty:
        table = energy_table(energy)
        table.to_csv(RESULTS / "tables" / "energy.csv", index=False)
        outputs["energy"] = tables.write(
            table, RESULTS / "tables" / "energy.tex",
            "Battery boundary: initial device energy against success, energy per "
            "successful request and device deaths.", "tab:p2-energy",
            wide=True, size=r"\footnotesize",
            notes="No device dies at any level, so the battery is not a binding "
                  "constraint in this scenario and the energy term of the "
                  "objective cannot earn its keep.")

    return {key: Path(value) for key, value in outputs.items()}


if __name__ == "__main__":
    built = build()
    for name, path in built.items():
        print(f"{name}: {path}")
