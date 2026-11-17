# Revision map — P2

One row per item of the major-revision report. Every number below is produced by
`scenario/analysis.py` plus `scenario/revision_analysis.py` and is quoted in
`paper/main.tex`. Nothing is entered by hand.

| Item | What was asked | Artifact | Headline number |
|---|---|---|---|
| 1 | Cross placement and fidelity in a factorial instead of ablating sequentially | `tables/factorial.tex`, `factorial_anova.tex`, `factorial_tests.tex`; 480 runs, 20 seeds | At 2x: 0.000 (all no-ladder arms), 0.375 (ladder only), 0.496 (edge+ladder), 0.496 (joint). Ladder partial η²=0.997 vs placement 0.793; placement×ladder η²=0.736. |
| 2 | Separate compute overhead from payload overhead | `tables/cp_split.tex`, `figures/f10_cp_split.pdf`; 480 runs, 4×4 lattice | Compute 1x→3x: aware 0.990→0.400, naive 0.977→0.000. Payload 1x→3x at fixed compute: no change except one cell at 1.5x. |
| 3 | Replace the linear fidelity cost with a nonlinear family | `tables/fidelity_gamma.tex`, `figures/f9_gamma.pdf`; 120 runs | Success flat at 0.784 / 0.498 / 0.40 across all four laws; executed fidelity at 3x spans 0.462–0.681. |
| 3.5 | Decompose the objective and show which term can move a decision | `tables/objective.tex` | Delay 129–330 ms, spread 115–172 ms, container 20 ms, energy 0.15 ms (0.06% of score); objective flips = 0 at every level. |
| 3.6 | Report the decision procedure: feasibility, fallback, overhead | `tables/decisions.tex` | ~710 decisions/run at 2–3x; 96% feasible, 4% fallback; 5.8 kMI and 375 KB per request. |
| 4 | Re-run the cache/component ablation where a cache exists | `tables/ablation2.tex`, `cache.tex`; 420 runs | All five arms tie at 0.851 / 0.662 / 0.400–0.408. Cache term changes 0.000 picks; top hit ratio buys +0.033 at 3x only with the ladder. |
| 5 | Reconcile the workload and task counts | `WORKLOAD.md` | 2,100 requests/run = 24 generating devices × 17.5/min × 5 min; 1,200 RAG + 900 control; 2,175 belongs to a superseded file. |
| 6 | Use the right statistics and show the nulls are powered | `tables/factorial_anova.tex`, `factorial_tests.tex`, `policy_tests.tex`, `POWER.md` | 3-way balanced ANOVA; 40 + 24 Holm-corrected seed-matched contrasts; paired SD 0.005 → MDE 0.003 at 2x. |
| 7 | Show where work actually goes when all tiers are eligible | `tables/tiers.tex`; 120 runs | Cloud share 0.000 in all 12 cells; placement gain is a mist→edge shift. |
| 8 | Resolve both classes inside the factorial | `tables/classes2.tex` | At 2x: RAG 0.500 at fidelity 0.750; control 0.491 at 0.742. At 3x: 0.396 / 0.409. |
| 9 | Test horizon and warm-up sensitivity | `tables/horizon.tex`; 180 runs | 5/15/30 min → 0.740 / 0.737 / 0.740, request count 2,100 → 12,600. |
| 10 | Justify the seed count | `POWER.md` | 20 seeds resolve 0.003 at 2x; 7 seeds suffice for a 1-point effect; the exact zero is a true null. |
| 11 | Check the battery boundary | `tables/energy.tex`; 60 runs | Zero device deaths at 20/50/100 Wh; success 0.977 naive / 0.990 joint at every level. |
| 12 | Ground the LLM overhead model in a serving simulator | `GenAIInferenceSim/results/fidelity_ablation/{summary.csv,FINDINGS.md}`; 20 cells × 5 seeds | In Docket (weak edge, fast cloud): joint+ladder 1.000→0.972; joint without ladder 0.887→0.167; ladder alone ≈0. Generated-token ratio 0.90→0.42. |

## Totals

* PureEdgeSim: **2,620 runs** across 13 grids.
* Docket: **100 simulator executions** (20 cells × 5 seeds) plus 1 smoke run.
* All tables loaded with `\input`; every figure produced by the artifact pipeline.
* Diagrams are Mermaid (`.mmd` → `.png`), not TikZ.
* `python scenario/analysis.py` writes `acceptance.json`: **24/24** criteria pass.
