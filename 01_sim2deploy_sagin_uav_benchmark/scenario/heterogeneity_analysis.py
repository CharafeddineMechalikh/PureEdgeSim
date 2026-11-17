"""Does the architecture effect survive when the tiers actually differ?

The crossed design varies which tiers are *eligible* while holding their
capacity fixed, so at a 50 ms control interval the architecture effect is
negative: adding a cloud tier can only add a queue.  That is a statement about
a homogeneous fleet, not about edge computing.

This module crosses the architecture and algorithm factors with four
infrastructure profiles (``heterogeneity.py``) so the two claims can be
separated:

* at the simulator's default 1 s interval the effect is positive under *every*
  profile, including the one where the cloud is slow and distant -- the
  signature of a clock artefact rather than a property of the topology;
* at 50 ms the sign of the effect is predicted by the profile: strongly
  positive when the edge is weak and the cloud is fast (adaptive placement has
  something to win), negative when the edge is already the best host.

The algorithms are carried along because the review asks whether a ranking
measured at one temporal resolution survives a change of infrastructure.
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from research_common import figures, stats, tables  # noqa: E402

RESULTS = ROOT / "01_sim2deploy_sagin_uav_benchmark" / "results"
PROFILES = ("uniform", "edge_weak", "edge_strong", "split_task")
PROFILE_NOTE = {
    "uniform": "ample edge, ample cloud",
    "edge_weak": "weak edge, fast powerful cloud",
    "edge_strong": "powerful edge, slow distant cloud",
    "split_task": "light work favours edge, heavy work favours cloud",
}
ALGORITHMS = ("random", "round_robin", "trade_off", "minmin", "leastloaded", "mct", "qos")


def load() -> pd.DataFrame:
    frame = pd.read_csv(RESULTS / "runs" / "hetero" / "raw_runs.csv")
    frame["policy"] = frame["policy"].astype(str)
    return frame


def effect_table(frame: pd.DataFrame) -> pd.DataFrame:
    """ALL minus EDGE_ONLY success, per profile and interval, with a seed CI."""
    wide = (frame.groupby(["infra", "architecture", "update_interval", "seed"])
            ["success_ratio"].mean().unstack("architecture"))
    rows = []
    # After unstack the index is (infra, interval, seed): levels 0 and 1 are the
    # cell, level 2 is the replication.  Grouping on level 2 by mistake silently
    # turns each seed into a "profile" -- which is exactly what the first
    # version of this function did.
    for (infra, interval), block in wide.groupby(level=[0, 1]):
        block = block.droplevel([0, 1])
        if not {"ALL", "EDGE_ONLY"} <= set(block.columns):
            continue
        difference = (block["ALL"] - block["EDGE_ONLY"]).dropna()
        if difference.empty:
            continue
        low, high = stats.bootstrap_ci(difference.to_numpy(dtype=float), np.mean)
        rows.append({
            "Profile": infra,
            "What it means": PROFILE_NOTE[infra],
            "Interval (s)": float(interval),
            "All tiers": float(block["ALL"].mean()),
            "Edge only": float(block["EDGE_ONLY"].mean()),
            "Effect (pp)": 100.0 * float(difference.mean()),
            "95% CI (pp)": f"[{100 * low:+.1f}, {100 * high:+.1f}]",
            "Seeds": int(difference.size),
            "Sign": "benefit" if difference.mean() > 0 else "penalty",
        })
    order = {name: index for index, name in enumerate(PROFILES)}
    table = pd.DataFrame(rows)
    return (table.sort_values(["Profile", "Interval (s)"],
                              key=lambda s: s.map(order) if s.name == "Profile" else s)
            .reset_index(drop=True))


def ranking_table(frame: pd.DataFrame) -> pd.DataFrame:
    """Best algorithm per (profile, interval): does the winner move?"""
    means = (frame[frame["architecture"] == "ALL"]
             .pivot_table(index="policy", columns=["infra", "update_interval"],
                          values="success_ratio", aggfunc="mean"))
    rows = []
    for infra in PROFILES:
        for interval in (1.0, 0.05):
            column = means[(infra, interval)].dropna()
            if column.empty:
                continue
            ordered = column.sort_values(ascending=False)
            rows.append({
                "Profile": infra,
                "Interval (s)": interval,
                "Winner": ordered.index[0],
                "Winner success": float(ordered.iloc[0]),
                "Runner-up": ordered.index[1] if len(ordered) > 1 else "",
                "Gap to runner-up (pp)": 100.0 * float(ordered.iloc[0] - ordered.iloc[1])
                if len(ordered) > 1 else float("nan"),
                "Worst": ordered.index[-1],
                "Worst success": float(ordered.iloc[-1]),
                "Spread (pp)": 100.0 * float(ordered.iloc[0] - ordered.iloc[-1]),
            })
    return pd.DataFrame(rows)


def interaction_table(frame: pd.DataFrame) -> pd.DataFrame:
    """Factorial ANOVA of success over profile x interval x architecture."""
    table = stats.factorial_anova(frame, "success_ratio",
                                 ["infra", "architecture", "update_interval"])
    return table


def affinity_table(frame: pd.DataFrame) -> pd.DataFrame:
    """Where work lands, per profile, to explain the sign of the effect."""
    rows = []
    columns = {"Tasks successfully executed (Mist)": "Mist",
               "Tasks successfully executed (Edge)": "Edge",
               "Tasks successfully executed (Cloud)": "Cloud"}
    subset = frame[(frame["architecture"] == "ALL") & (frame["policy"] == "trade_off")]
    for (infra, interval), block in subset.groupby(["infra", "update_interval"]):
        generated = block["Number of generated tasks"].replace(0, np.nan)
        row = {"Profile": infra, "Interval (s)": float(interval)}
        for column, label in columns.items():
            row[f"{label} share"] = float((block[column] / generated).mean())
        row["Success"] = float(block["success_ratio"].mean())
        rows.append(row)
    return pd.DataFrame(rows)


def build() -> dict:
    frame = load()
    outputs: dict[str, object] = {}

    effect = effect_table(frame)
    effect.to_csv(RESULTS / "tables" / "heterogeneity.csv", index=False)
    outputs["effect_frame"] = effect
    outputs["effect"] = tables.write(
        effect, RESULTS / "tables" / "heterogeneity.tex",
        "Architecture effect (all tiers minus edge only) under four "
        "infrastructure profiles and two control intervals, 40 devices, "
        "seven algorithms, 10 seeds.",
        "tab:p1-heterogeneity", wide=True, size=r"\scriptsize",
        notes="A positive effect at the 1 s default with a negative effect at "
              "50 ms under the same profile is the definition of a clock "
              "artefact: the topology did not change between the two rows. "
              "Confidence intervals resample seeds.")

    ranking = ranking_table(frame)
    ranking.to_csv(RESULTS / "tables" / "hetero_ranking.csv", index=False)
    outputs["ranking_frame"] = ranking
    outputs["ranking"] = tables.write(
        ranking, RESULTS / "tables" / "hetero_ranking.tex",
        "Which algorithm wins depends on both the control interval and the "
        "infrastructure profile (ALL architecture, 40 devices, 10 seeds).",
        "tab:p1-hetero-ranking", align="lllcccccc", wide=True, size=r"\scriptsize",
        notes="Spread is the distance between the best and worst algorithm in "
              "the same cell; it measures how much the choice is worth there.")

    interaction = interaction_table(frame)
    interaction.to_csv(RESULTS / "tables" / "hetero_interaction.csv", index=False)
    outputs["interaction_frame"] = interaction
    display = interaction.rename(columns={"term": "Term", "sum_sq": "Sum of squares",
                                          "mean_sq": "Mean square",
                                          "partial_eta_sq": "Partial eta sq"})
    display["p_value"] = display["p_value"].map(
        lambda value: "$<0.001$" if value < 1e-3 else f"{value:.3f}")
    outputs["interaction"] = tables.write(
        display,
        RESULTS / "tables" / "hetero_interaction.tex",
        "Factorial analysis of the success ratio over the fully crossed "
        "infrastructure x architecture x interval design (1680 runs, balanced).",
        "tab:p1-hetero-interaction", align="lcccccc", wide=True, size=r"\footnotesize",
        notes="The profile x interval interaction is the formal statement that "
              "the temporal configuration changes the sign of the architectural "
              "effect; it cannot be read off the main effects.")

    affinity = affinity_table(frame)
    affinity.to_csv(RESULTS / "tables" / "hetero_affinity.csv", index=False)
    outputs["affinity_frame"] = affinity
    outputs["affinity"] = tables.write(
        affinity, RESULTS / "tables" / "hetero_affinity.tex",
        "Where trade-off placement sends work under each profile, as a share of "
        "generated tasks (ALL architecture, 10 seeds).",
        "tab:p1-hetero-affinity", align="llcccc", wide=True, size=r"\footnotesize",
        notes="The cloud share is what an adaptive orchestrator can exploit; "
              "under the weak-edge profile it is both reachable and fast, which "
              "is why the effect is positive there at 50 ms.")

    if not effect.empty:
        plot = effect.copy()
        plot["cell"] = plot["Profile"] + " @ " + plot["Interval (s)"].map("{:g}s".format)
        figures.bar_with_ci(plot, "cell", "Effect (pp)", None,
                            RESULTS / "figures" / "f16_heterogeneity",
                            ci_lo=None, ci_hi=None,
                            xlabel="Profile @ control interval",
                            ylabel="Architecture effect (pp)",
                            title="The architecture effect changes sign with the clock")
        outputs["fig"] = RESULTS / "figures" / "f16_heterogeneity.pdf"
    return outputs


if __name__ == "__main__":
    built = build()
    print(f"[P1 heterogeneity] {len(built)} exhibits")
