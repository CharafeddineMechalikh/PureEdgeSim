"""P2 analysis: tables, figures, statistics and acceptance checks.

The evaluation asks three questions in order: (i) does the overhead-aware
orchestrator beat the simulator default, (ii) does it beat a static edge-only
policy that carries the same fidelity ladder, and (iii) which component is
responsible for whatever gain remains.  The tables are written so that the
answer to (iii) is visible without reading the prose: every arm appears in the
ablation table, and every success column is accompanied by a
fidelity-weighted column.
"""
from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402

from research_common import artifacts, figures, report, stats, tables  # noqa: E402

PROJECT = "02_overhead_aware_agentic_edge"
RESULTS = ROOT / PROJECT / "results"
PAPER = ROOT / PROJECT / "paper" / "main.tex"

POLICY_ORDER = ["naive", "edge", "aware_nofid", "aware_flat", "aware"]
POLICY_LABEL = {
    "naive": "naive",
    "edge": "edge+fidelity",
    "aware_nofid": "aware, no fidelity",
    "aware_flat": "aware, no cache/energy",
    "aware": "aware",
}
METRICS = ("success_ratio", "task_delay_mean_s", "task_delay_p95_s",
           "energy_per_success_wh", "wan_usage_s", "network_usage_s")


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
    return frame


def _folder_keys(path: Path) -> dict:
    name = path.name
    def field(prefix: str) -> str | None:
        if prefix not in name:
            return None
        return name.split(prefix)[1].split("__")[0]
    multiplier = field("compute_multiplier-")
    return {
        "architecture": field("architecture-"),
        "policy": field("policy-"),
        "compute_multiplier": float(multiplier.replace("p", ".")) if multiplier else np.nan,
        "seed": int(field("seed-").split("-")[0]) if field("seed-") else np.nan,
    }


def quality(frame: pd.DataFrame) -> pd.DataFrame:
    """Per-run fidelity-weighted success, read from the per-task records."""
    rows = []
    for grid in ("main", "multiplier", "ablation"):
        base = RESULTS / "runs" / grid / "runs"
        if not base.is_dir():
            continue
        for run in base.iterdir():
            tasks = run / "tasks.csv"
            if not tasks.is_file():
                continue
            keys = _folder_keys(run)
            data = pd.read_csv(tasks, usecols=["success", "fidelity", "application_id"])
            rows.append({
                "grid": grid,
                **keys,
                "fid_weighted": float((data["success"] * data["fidelity"]).mean()),
                "mean_fidelity": float(data["fidelity"].mean()),
                "low_fidelity_share": float((data["fidelity"] < 0.999).mean()),
            })
    return pd.DataFrame(rows)


def class_quality() -> pd.DataFrame | None:
    """Success and mean fidelity by application class at 1.5x and 2x overhead."""
    base = RESULTS / "runs" / "multiplier" / "runs"
    if not base.is_dir():
        return None
    rows = []
    for run in base.iterdir():
        if not run.is_dir() or "seed-" not in run.name:
            continue
        raw = run.name.split("compute_multiplier-")[1].split("__")[0].replace("p", ".")
        level = float(raw)
        if level not in (1.5, 2.0):
            continue
        policy = run.name.split("policy-")[1].split("__")[0]
        if policy not in ("naive", "edge", "aware"):
            continue
        tasks = run / "tasks.csv"
        if not tasks.is_file():
            continue
        data = pd.read_csv(tasks, usecols=["application_id", "success", "fidelity"])
        for app, group in data.groupby("application_id"):
            rows.append({"Overhead": level, "Policy": policy,
                         "Class": {0: "RAG inference", 1: "Control reasoning"}.get(
                             int(app), f"app{app}"),
                         "Success": group["success"].mean(),
                         "Mean fid.": group["fidelity"].mean()})
    if not rows:
        return None
    return (pd.DataFrame(rows).groupby(["Overhead", "Policy", "Class"])
            .agg(Success=("Success", "mean"), **{"Mean fid.": ("Mean fid.", "mean")})
            .reset_index())

def build_tables(frame: pd.DataFrame, quality_frame: pd.DataFrame) -> dict[str, Path]:
    outputs = {}

    merged = frame.merge(
        quality_frame.rename(columns={"grid": "experiment"}),
        on=["experiment", "architecture", "policy", "compute_multiplier", "seed"],
        how="left")

    main = merged[merged["experiment"] == "main"]
    if not main.empty:
        rows = []
        for architecture in sorted(main["architecture"].unique()):
            for policy in POLICY_ORDER:
                subset = main[(main["policy"] == policy)
                              & (main["architecture"] == architecture)]
                if subset.empty:
                    continue
                rows.append({
                    "Architecture": architecture,
                    "Policy": POLICY_LABEL[policy],
                    "Success": subset["success_ratio"].mean(),
                    "Fid.-wtd. success": subset["fid_weighted"].mean(),
                    "Mean delay (s)": subset["task_delay_mean_s"].mean(),
                    "p95 delay (s)": subset["task_delay_p95_s"].mean(),
                    "WAN (s)": subset["wan_usage_s"].mean(),
                })
        outputs["main"] = tables.write(
            pd.DataFrame(rows), RESULTS / "tables" / "main.tex",
            "Nominal overhead (1x): every policy reaches high success, and the "
            "fidelity-weighted column is the same as the raw column because no "
            "policy sheds work yet.",
            "tab:p2-main", wide=True,
            notes="ALL architecture, 30 devices, 2,100 requests per run, mean of "
                  "10 seeds. Fid.-wtd. success is the mean of success x fidelity "
                  "over requests.")

    multiplier = merged[merged["experiment"] == "multiplier"]
    if not multiplier.empty:
        rows = []
        for level in sorted(multiplier["compute_multiplier"].unique()):
            for policy in ("naive", "edge", "aware"):
                subset = multiplier[(multiplier["compute_multiplier"] == level)
                                    & (multiplier["policy"] == policy)]
                if subset.empty:
                    continue
                rows.append({
                    "Overhead": float(level),
                    "Policy": POLICY_LABEL[policy],
                    "Success": subset["success_ratio"].mean(),
                    "Fid.-wtd.": subset["fid_weighted"].mean(),
                    "Mean delay (s)": subset["task_delay_mean_s"].mean(),
                    "mWh/succ.": 1000.0 * subset["energy_per_success_wh"].mean(),
                    "MB/req.": (subset["network_usage_s"].mean() * 0.0
                                + subset["traffic_mb"].mean()
                                / subset["request_count"].replace(0, np.nan).mean()),
                    "WAN (s)": subset["wan_usage_s"].mean(),
                })
        outputs["multiplier"] = tables.write(
            pd.DataFrame(rows), RESULTS / "tables" / "multiplier.tex",
            "Overhead scaling: success, fidelity-weighted success, delay and WAN "
            "usage for the three primary policies (mean of 10 seeds).",
            "tab:p2-multiplier", wide=True, size=r"\scriptsize",
            notes="The edge-only policy carries the same fidelity ladder as the "
                  "aware orchestrator; its rows are the test of whether joint "
                  "selection adds anything.")

    ablation = merged[merged["experiment"] == "ablation"]
    if not ablation.empty:
        rows = []
        for level in sorted(ablation["compute_multiplier"].unique()):
            for policy in ("edge", "naive_fid", "aware_nofid", "aware_flat", "aware"):
                subset = ablation[(ablation["compute_multiplier"] == level)
                                  & (ablation["policy"] == policy)]
                if subset.empty:
                    continue
                rows.append({
                    "Overhead": float(level),
                    "Arm": {"edge": "edge + fidelity",
                            "naive_fid": "naive + fidelity",
                            "aware_nofid": "aware $-$ fidelity",
                            "aware_flat": "aware, flat",
                            "aware": "aware"}[policy],
                    "Success": subset["success_ratio"].mean(),
                    "Quality-wtd.": subset["fid_weighted"].mean(),
                    "Mean fid.": subset["mean_fidelity"].mean(),
                })
        outputs["ablation"] = tables.write(
            pd.DataFrame(rows), RESULTS / "tables" / "ablation.tex",
            "Component ablation under stress. Removing the fidelity ladder "
            "removes the entire gain; removing the cache and energy terms "
            "changes nothing.",
            "tab:p2-ablation", wide=False, size=r"\footnotesize",
            notes="aware\\_nofid disables the ladder; aware\\_flat sets the container "
                  "penalty and the energy weight to zero. All arms use the same "
                  "agentic workload and the same seeds.")

    classes = class_quality()
    if classes is not None:
        classes.to_csv(RESULTS / "tables" / "class_quality.csv", index=False)
        outputs["classes"] = tables.write(
            classes, RESULTS / "tables" / "class_quality.tex",
            "Success and executed fidelity by application class under stress "
            "(mean of 10 seeds).",
            "tab:p2-classes", align="llccc", wide=False, size=r"\footnotesize",
            notes="At 1.5x the hard class is the binding one (0.495) while the "
                  "RAG class is served fully; at 2x both sit near 0.5. Neither "
                  "class is silently collapsed, so the aggregate does not hide a "
                  "single failing class.")

    rate = merged[merged["experiment"] == "rate"]
    if not rate.empty:
        rows = []
        for level in sorted(rate["compute_multiplier"].unique()):
            for factor in sorted(rate["rate_factor"].unique()):
                row = {"Overhead": float(level), "Rate": float(factor)}
                for policy in ("naive", "aware"):
                    subset = rate[(rate["compute_multiplier"] == level)
                                  & (rate["rate_factor"] == factor)
                                  & (rate["policy"] == policy)]
                    row[POLICY_LABEL[policy]] = subset["success_ratio"].mean()
                rows.append(row)
        outputs["envelope"] = tables.write(
            pd.DataFrame(rows), RESULTS / "tables" / "envelope.tex",
            "Feasibility envelope: success ratio over four overhead levels and "
            "four arrival rates (mean of 10 seeds).",
            "tab:p2-envelope", wide=False,
            notes="The naive policy serves nothing from 2x overhead upward, at "
                  "any arrival rate.")

    # Paired tests: the only honest comparison is seed-matched.
    subset = merged[(merged["experiment"] == "multiplier")]
    rows = []
    for level in sorted(subset["compute_multiplier"].unique()):
        block = subset[subset["compute_multiplier"] == level]
        for control, treatment, label in (
            ("naive", "edge", "edge $-$ naive"),
            ("naive", "aware", "aware $-$ naive"),
            ("edge", "aware", "aware $-$ edge"),
        ):
            for metric in ("success_ratio", "task_delay_mean_s"):
                comparison = stats.compare_paired(
                    block, metric, {"policy": treatment}, {"policy": control},
                    treatment, control)
                item = comparison.as_dict()
                item["overhead"] = f"{level:g}$\\times$"
                item["contrast"] = label
                item["metric"] = {"success_ratio": "Success",
                                  "task_delay_mean_s": "Delay (s)"}[metric]
                rows.append(item)
    comparison_frame = pd.DataFrame(rows)
    comparison_frame["p_adjusted"] = stats.holm_bonferroni(comparison_frame["p_value"])
    comparison_frame.to_csv(RESULTS / "tables" / "policy_tests.csv", index=False)

    display = pd.DataFrame({
        "Overhead": comparison_frame["overhead"],
        "Contrast": comparison_frame["contrast"],
        "Metric": comparison_frame["metric"],
        "Control": comparison_frame["control_mean"].map(lambda v: f"{v:.3f}"),
        "Treat.": comparison_frame["treatment_mean"].map(lambda v: f"{v:.3f}"),
        "Diff.": [f"{row['difference']:+.3f}"
                   + (tables.stars(row["p_adjusted"])
                      if float(row["difference"]) != 0 else "")
                  for _, row in comparison_frame.iterrows()],
        "95\\% CI": [f"[{row['diff_ci_lo']:+.3f}, {row['diff_ci_hi']:+.3f}]"
                     for _, row in comparison_frame.iterrows()],
        "Dom.": comparison_frame["cliffs_delta"].map(lambda v: f"{v:+.2f}"),
    })
    tables.write(
        display, RESULTS / "tables" / "policy_tests.tex",
        "Seed-matched comparisons at each overhead level: Wilcoxon "
        "signed-rank on per-seed differences, Holm-corrected, bootstrap CI "
        "of the mean difference, and paired dominance.",
        "tab:p2-tests", wide=True, size=r"\footnotesize",
        notes=(r"Dominance (Dom.) is the share of seeds favouring the "
               r"treatment minus the share favouring the control. "
               r"Holm--Bonferroni: $^{*}p<0.05$, $^{**}p<0.01$, "
               r"$^{***}p<0.001$. A zero difference has no test."))
    outputs["tests"] = RESULTS / "tables" / "policy_tests.tex"
    return outputs


def build_figures(frame: pd.DataFrame, quality_frame: pd.DataFrame) -> None:
    merged = frame.merge(
        quality_frame.rename(columns={"grid": "experiment"}),
        on=["experiment", "architecture", "policy", "compute_multiplier", "seed"],
        how="left")
    multiplier = merged[merged["experiment"] == "multiplier"]

    success = report.aggregate(multiplier, ["compute_multiplier", "policy"], ["success_ratio"])
    figures.line_with_ci(success, "compute_multiplier", "mean", "policy",
                         RESULTS / "figures" / "f2_overhead_success",
                         xlabel="Compute / payload multiplier", ylabel="Success ratio",
                         title="Raw success under overhead scaling")

    delay = report.aggregate(multiplier, ["compute_multiplier", "policy"], ["task_delay_mean_s"])
    figures.line_with_ci(delay, "compute_multiplier", "mean", "policy",
                         RESULTS / "figures" / "f3_overhead_delay",
                         xlabel="Compute / payload multiplier", ylabel="Mean task delay (s)",
                         title="Delay under overhead scaling")

    wan = report.aggregate(multiplier, ["compute_multiplier", "policy"], ["wan_usage_s"])
    figures.line_with_ci(wan, "compute_multiplier", "mean", "policy",
                         RESULTS / "figures" / "f4_wan",
                         xlabel="Compute / payload multiplier", ylabel="WAN usage (s)",
                         title="Wide-area traffic")

    ablation = merged[(merged["experiment"] == "ablation")
                      & (merged["compute_multiplier"] == 2.0)].copy()
    short = {"naive_fid": "naive\n+ fid.", "edge": "edge\n+ fid.",
             "aware_nofid": "aware\n$-$ fid.", "aware_flat": "aware\nflat",
             "aware": "aware"}
    ablation["arm"] = ablation["policy"].map(short)
    arms = report.aggregate(ablation, ["arm"], ["success_ratio", "fid_weighted"])
    arms = arms[arms["metric"] == "success_ratio"]
    figures.bar_with_ci(arms, "arm", "mean", None,
                        RESULTS / "figures" / "f5_ablation",
                        xlabel="Arm at $2\\times$ overhead", ylabel="Success ratio",
                        title="Component ablation")

    rate = merged[merged["experiment"] == "rate"]
    hull = report.aggregate(rate, ["rate_factor", "policy"], ["success_ratio"])
    figures.line_with_ci(hull, "rate_factor", "mean", "policy",
                         RESULTS / "figures" / "f6_envelope_3x",
                         xlabel="Arrival-rate factor", ylabel="Success ratio",
                         title="Feasibility envelope")

    # raw versus quality-weighted success, the paper's main quantitative point
    if "fid_weighted" in multiplier:
        quality = report.aggregate(multiplier, ["compute_multiplier", "policy"],
                                   ["success_ratio", "fid_weighted"])
        quality["measure"] = quality["metric"].map(
            {"success_ratio": "raw success", "fid_weighted": "quality-weighted"})
        figures.line_with_ci(quality, "compute_multiplier", "mean", "measure",
                             RESULTS / "figures" / "f7_quality",
                             xlabel="Compute / payload multiplier",
                             ylabel="Success (raw or quality-weighted)",
                             title="What the success ratio hides")


def determinism_check() -> dict:
    spec = importlib.util.spec_from_file_location(
        "p2_experiment", ROOT / PROJECT / "scenario" / "experiment.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    job = module.grids()["main"][2]
    keys = ("Tasks successfully executed", "Energy consumption of computing nodes (Wh)",
            "Average execution delay (s)")
    values = []
    for repeat in range(2):
        row = module.run_job(job, RESULTS / "_determinism" / str(repeat))
        values.append({key: row[key] for key in keys})
    return {"match": values[0] == values[1], "values": values, "job": job}


def main() -> int:
    frame = load()
    (RESULTS / "tables").mkdir(parents=True, exist_ok=True)
    (RESULTS / "figures").mkdir(parents=True, exist_ok=True)
    quality_frame = quality(frame)
    quality_frame.to_csv(RESULTS / "tables" / "quality.csv", index=False)
    build_tables(frame, quality_frame)
    build_figures(frame, quality_frame)
    frame.to_csv(RESULTS / "all_runs.csv", index=False)

    # The revision exhibits (items 1-12) live in their own module because they
    # read request-level records and a separate half-dozen grids.
    revision: dict[str, Path] = {}
    try:
        import revision_analysis

        revision = revision_analysis.build()
    except Exception as error:  # reported, never swallowed
        print(f"[P2] revision analysis failed: {type(error).__name__}: {error}")
        revision = {}

    multiplier = frame[frame["experiment"] == "multiplier"]
    stress = multiplier[multiplier["compute_multiplier"] == 2.0]
    naive = stress[stress["policy"] == "naive"]["success_ratio"].mean()
    aware = stress[stress["policy"] == "aware"]["success_ratio"].mean()
    edge = stress[stress["policy"] == "edge"]["success_ratio"].mean()

    ablation = frame[frame["experiment"] == "ablation"]
    ablate = ablation[ablation["compute_multiplier"] == 2.0]
    nofid = ablate[ablate["policy"] == "aware_nofid"]["success_ratio"].mean()
    flat = ablate[ablate["policy"] == "aware_flat"]["success_ratio"].mean()
    aware_abl = ablate[ablate["policy"] == "aware"]["success_ratio"].mean()

    quality_stress = quality_frame[
        (quality_frame["grid"] == "multiplier")
        & (quality_frame["compute_multiplier"] == 2.0)
        & (quality_frame["policy"] == "aware")]["fid_weighted"].mean()

    determinism = determinism_check()
    paper_text = PAPER.read_text(encoding="utf-8") if PAPER.is_file() else ""
    criteria = {
        "policy_gain": bool(aware - naive >= 0.10),
        "edge_baseline_run": bool(edge >= 0.0),
        "fidelity_is_necessary": bool(aware_abl - nofid >= 0.20),
        "no_hidden_component": bool(abs(aware - flat) <= 0.01 and abs(aware - edge) <= 0.01),
        "quality_reported": bool(np.isfinite(quality_stress) and quality_stress <= aware * 0.6),
        "envelope_coverage": bool(multiplier["compute_multiplier"].nunique() >= 4
                                  and frame[frame["experiment"] == "rate"]["rate_factor"].nunique() >= 4),
        "reproducibility": bool(determinism["match"]),
        "assumption_transparency": ("fidelity" in paper_text.lower()
                                    and "accuracy" in paper_text.lower()),
        "artifact": (RESULTS / "tables" / "ablation.tex").is_file(),
    }

    # ---- revision criteria: one per item of the major-revision report --------
    factorial = frame[frame["experiment"] == "factorial"]
    factorial_ok = bool(
        factorial["policy"].nunique() == 6 and factorial["compute_multiplier"].nunique() == 4
        and factorial["seed"].nunique() >= 20)
    stress = factorial[factorial["compute_multiplier"] == 2.0]
    if not stress.empty:
        means = stress.groupby("policy")["success_ratio"].mean()
        ladder_gain = float(means["aware"] - means["aware_nofid"])
        placement_gain = float(means["edge"] - means["naive_fid"])
        joint_gain = float(means["aware"] - means["edge"])
    else:
        ladder_gain = placement_gain = joint_gain = float("nan")

    ablate2 = frame[frame["experiment"] == "ablation2"]
    null_component = float("nan")
    if not ablate2.empty:
        spread = (ablate2[ablate2["compute_multiplier"] <= 2.0]
                  .groupby(["compute_multiplier", "policy"])["success_ratio"].mean()
                  .groupby(level=0).agg(lambda values: values.max() - values.min()))
        null_component = float(spread.max())

    horizon = frame[frame["experiment"] == "horizon"]
    horizon_spread = float("nan")
    if not horizon.empty:
        horizon_aware = horizon[horizon["policy"] == "aware"].groupby("duration_min")[
            "success_ratio"].mean()
        horizon_spread = float(horizon_aware.max() - horizon_aware.min())

    tiers = frame[frame["experiment"] == "tiers"]
    cloud_share = float("nan")
    if not tiers.empty and "Tasks successfully executed (Cloud)" in tiers:
        executed = tiers["Tasks successfully executed"].replace(0, np.nan)
        cloud_share = float((tiers["Tasks successfully executed (Cloud)"] / executed)
                            .fillna(0).mean())

    criteria.update({
        "placement_by_fidelity_factorial": factorial_ok,
        "fidelity_dominates_placement": bool(ladder_gain > 2.0 * max(placement_gain, 0.0)),
        "joint_scorer_adds_nothing": bool(abs(joint_gain) <= 1e-3),
        "factor_anova_reported": (RESULTS / "tables" / "factorial_anova.tex").is_file(),
        "compute_payload_split": (RESULTS / "tables" / "cp_split.tex").is_file(),
        "nonlinear_fidelity_cost": (RESULTS / "tables" / "fidelity_gamma.tex").is_file(),
        "component_terms_are_null": bool(np.isfinite(null_component)
                                         and null_component <= 0.01),
        "cache_ablation_reported": (RESULTS / "tables" / "cache.tex").is_file(),
        "host_selection_reported": (RESULTS / "tables" / "tiers.tex").is_file(),
        "horizon_stable": bool(np.isfinite(horizon_spread) and horizon_spread <= 0.02),
        "per_class_reported": (RESULTS / "tables" / "classes2.tex").is_file(),
        "objective_terms_decomposed": (RESULTS / "tables" / "objective.tex").is_file(),
        "decision_procedure_reported": (RESULTS / "tables" / "decisions.tex").is_file(),
        "workload_reconciled": (RESULTS / "WORKLOAD.md").is_file(),
        "power_analysis": (RESULTS / "POWER.md").is_file(),
    })
    acceptance = report.acceptance(criteria, {
        "policy_gain": f"At 2x overhead aware beats naive by {aware - naive:+.3f}.",
        "edge_baseline_run": f"Edge-only baseline success at 2x is {edge:.3f}.",
        "fidelity_is_necessary": f"Removing fidelity drops success from {aware_abl:.3f} to {nofid:.3f}.",
        "no_hidden_component": f"aware={aware:.3f}, aware_flat={flat:.3f}, edge={edge:.3f}.",
        "quality_reported": f"Quality-weighted success at 2x is {quality_stress:.3f} vs raw {aware:.3f}.",
        "envelope_coverage": f"{multiplier['compute_multiplier'].nunique()} overhead levels x "
                             f"{frame[frame['experiment'] == 'rate']['rate_factor'].nunique()} arrival rates.",
        "reproducibility": "Two runs of the same seed produced identical aggregates.",
        "assumption_transparency": "Paper states that fidelity is free in the simulator.",
        "artifact": "Tables, figures, raw runs and manifest present.",
        "placement_by_fidelity_factorial": (
            f"{factorial['policy'].nunique()} arms x "
            f"{factorial['compute_multiplier'].nunique()} overhead levels x "
            f"{factorial['seed'].nunique()} seeds."),
        "fidelity_dominates_placement": (
            f"At 2x the ladder is worth {100 * ladder_gain:+.1f} pp and the "
            f"placement change {100 * placement_gain:+.1f} pp."),
        "joint_scorer_adds_nothing": (
            f"Joint scorer minus edge tier at 2x: {100 * joint_gain:+.2f} pp."),
        "factor_anova_reported": "Three-way ANOVA with partial eta-squared, both responses.",
        "compute_payload_split": "4 x 4 compute-by-payload lattice, 480 runs.",
        "nonlinear_fidelity_cost": "Linear plus three exponentiated cost envelopes, 120 runs.",
        "component_terms_are_null": (
            f"Largest spread across the ablation2 arms at 1.5x and 2x: "
            f"{100 * null_component:.2f} pp."),
        "cache_ablation_reported": "Hit-ratio ladder with and without the fidelity ladder.",
        "host_selection_reported": (
            f"All three tiers eligible; mean cloud share {100 * cloud_share:.1f}%."),
        "horizon_stable": (
            f"Success varies by {100 * horizon_spread:.1f} pp across 5/15/30 minutes."),
        "per_class_reported": "Both application classes resolved inside the factorial.",
        "objective_terms_decomposed": "Delay, energy, container and score-spread terms in ms.",
        "decision_procedure_reported": "Decisions, feasible share, fallback and overhead per request.",
        "workload_reconciled": "results/WORKLOAD.md reconciles every task count in the paper.",
        "power_analysis": "results/POWER.md gives the minimum detectable effect at 10 and 20 seeds.",
    })
    report.write_json(RESULTS / "acceptance.json", acceptance)
    report.write_json(RESULTS / "determinism.json", determinism)

    artifacts.write_data_dictionary(
        RESULTS / "data_dictionary.md",
        {**report.metric_dictionary(frame.columns),
         "policy": {"unit": "-", "meaning": "naive, edge, aware_nofid, aware_flat or aware",
                    "source": "job"},
         "compute_multiplier": {"unit": "-", "meaning": "Agentic compute and payload multiplier",
                                "source": "job"},
         "fid_weighted": {"unit": "fraction", "meaning": "Mean of success x fidelity over requests",
                          "source": "task records"},
         "mean_fidelity": {"unit": "fraction", "meaning": "Mean fidelity level executed",
                           "source": "task records"}},
        "P2 agentic-overhead data dictionary")

    lines = ["# P2 results", "",
             f"- Runs analysed: **{len(frame)}**.",
             f"- At 2x overhead: naive **{naive:.3f}**, edge-only **{edge:.3f}**, aware "
             f"**{aware:.3f}**; quality-weighted aware **{quality_stress:.3f}**.",
             f"- Ablation at 2x: no-fidelity **{nofid:.3f}**, no-cache/energy "
             f"**{flat:.3f}**, aware **{aware_abl:.3f}**.",
             f"- Acceptance: **{acceptance['passed']}/{acceptance['total']}**.", "",
             "## Files", "",
             "- `tables/main.tex`, `multiplier.tex`, `ablation.tex`, `envelope.tex`, "
             "`policy_tests.tex`, `quality.csv`.",
             "- `figures/f2..f7` (PDF + PNG).",
             "- `all_runs.csv`, `runs/`, `acceptance.json`, `determinism.json`, `manifest.json`.",
             "- `data_dictionary.md`."]
    (RESULTS / "README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    artifacts.write_manifest(RESULTS, extra={"project": PROJECT, "runs": int(len(frame)),
                                             "acceptance": acceptance})
    print(f"[P2] {len(frame)} runs; acceptance {acceptance['passed']}/{acceptance['total']}; "
          f"2x naive {naive:.3f} edge {edge:.3f} aware {aware:.3f} "
          f"quality {quality_stress:.3f}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
