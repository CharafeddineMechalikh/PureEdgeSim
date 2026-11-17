# Data-and-claims audit + table/figure mapping

Produced **before** any manuscript text rewrite, as requested. Source of truth:
`01_sim2deploy_sagin_uav_benchmark/results/runs/*/raw_runs.csv`,
`results/tables/*.csv`, and `crosssim/results/common_summary.csv`. Scripts:
`audit_claims.py`, `audit_stats.py`, `audit_inventory.py` (temporary tooling, see
`docs/` for the committed versions).

---

## 1. Data-and-claims audit

### 1.1 Verified numeric claims

| Claim in manuscript | Recomputed value | Source subset | Verdict |
|---|---|---|---|
| 3,215 runs | 3,215 | sum of 9 grids (240+120+120+135+90+100+100+630+1680) | **match** |
| 0.660 full architecture | 0.6604 | scale grid, 40 devices, TRADE_OFF, default tick | **match** |
| 0.121 edge-only | 0.1206 | same subset | **match** (rounding) |
| 0.539 architecture effect | 0.5397 | 0.6604 − 0.1206, same subset | **match** |
| 0.415 at 200 ms | 0.4149 | clock grid, ALL − EDGE_ONLY | **match** |
| −0.004 at 50 ms | −0.0041 | clock grid, ALL − EDGE_ONLY | **match** |
| 0.011 with deadlines ×16 | 0.0109 | deadline grid, ALL − EDGE_ONLY | **match** |
| Class-mix edge-only values 0.030/0.068/0.121/0.186/0.444 | 0.0299/0.0678/0.1206/0.1860/0.4444 | mix grid, m1–m5 | **match** |
| 0.648 pooled ALL mean | 0.6485 | scale grid, all devices/algorithms | **match** |
| 0.528 pooled architecture-effect comparison | see label audit below | pooled over scale grid | **needs explicit label** |
| 0.540 in deadline/paired context | 0.5397 recomputed | canonical subset | **rounding of 0.5397** |
| Resolvable difference 0.019 | 0.018572 | 2.8 × s_hat(0.020975) / √10 | **match** |

**Conclusion:** the numeric values themselves are reproducible. The problem is
**labelling**, not arithmetic: 0.539, 0.540, 0.528 and 0.648 refer to four
different populations/averaging methods and must be labelled as such wherever
they appear (abstract, results, discussion, conclusion). Do not unify them.

### 1.2 Statistical defects found

1. **p-values printed as `0.000`** in `factor_effects.tex` (8 cells) and
   `hetero_interaction.tex` (7 cells). Must become `p < 0.001` or the actual
   value. Numerical audit confirms these are genuine p-values underflowing the
   3-decimal formatter, not typos in the analysis.
2. **ANOVA assumption scrutiny required.** Partial η² values up to 0.994 with
   tiny residual variance come from seed-level aggregates; the model treats
   runs as the experimental unit, which is correct, but the manuscript must
   state this and report the residual degrees of freedom.
3. **Table XIII note** is correct (`2.8 × s_hat / √n` with `s_hat = 0.020975`,
   `n = 10`), but the paper should show the calculation once in the Methods
   section rather than only in a table footnote.
4. **Table VI (`Started`)** must state that the mean lag is conditional on
   execution having started; tasks that never started are excluded.
5. **Matched seeds** must make the paired (Wilcoxon) comparison primary;
   Mann–Whitney is supplementary and must be labelled as independent-arm.

### 1.3 Claims-versus-evidence audit

| Claim area | Current evidence | Allowed wording |
|---|---|---|
| Dependency-aware workflows | application graph + trace generated; **workflow arm not executed** | "scenario specification"; RQ3 **not** fully answered by dependency evidence |
| Cross-simulator (four platforms) | PureEdgeSim (reference + port), YAFS (common trace), EdgeCloudSim (common trace, event-driven only), iFogSim2 (not executed) | "capability-aware cross-simulator study on the platforms executed"; never "four-simulator benchmark" |
| PureEdgeSim Java | same framework, different port | "cross-port audit", never "independent simulator" |
| YAFS common trace vs independent replication | two different experiments with 5 and 3 seeds respectively | keep separate; do not merge |
| EdgeCloudSim | no global periodic parameter | "contrasting engine", no temporal sweep claimed |
| iFogSim2 | build verified; adapter partially implemented; forwarding defect documented at commit `5f68d394…` | capability/implementation discussion only |
| Cloud-only deployment | illustrative figure, not executed | label as illustrative, not experimental baseline |
| Scale as capacity result | utilisation low at all scales | "fleet-size sensitivity under this workload", not general scalability |

### 1.4 Terminology/scope audit

The paper must be reframed from *multi-tier edge orchestration* to
**edge-computing simulation scenarios**. Deployment configurations (including
multi-tier ones) are experimental factors. Mist/edge/cloud remain valid when
describing the actual simulated infrastructure. Title: **Option A**.

---

## 2. Scientific corrections required (numbered)

C1. Reframe title/abstract/introduction/conclusion to edge-computing simulation;
    demote "multi-tier" to a deployment factor.
C2. Label every architecture-effect value with its exact population, subset and
    averaging method (0.539 / 0.540 / 0.528 / 0.648).
C3. Replace all `p = 0.000` with `p < 0.001` (or exact value); state ANOVA unit,
    df, and residual variance.
C4. State that Table VI lag is conditional on execution start.
C5. Make paired seed-level analysis primary; label Mann–Whitney as
    independent-arm supplementary.
C6. Mark dependency-aware workflow arm as specified-but-unexecuted; do not claim
    RQ3 dependency validation.
C7. Mark iFogSim2 as not experimentally validated; keep blank cells blank.
C8. Distinguish YAFS common-trace experiment from the independent 3-seed
    replication.
C9. Reword scale result as workload-specific fleet-size sensitivity.
C10. Keep energy-per-generated and energy-per-success separate; define the
     energy model's boundaries.
C11. Justify the 0.2/0.8/60 s deadlines or add a second application scenario;
     otherwise scope external validity accordingly.
C12. Reconcile prose "UAV/field unit" with Table I's "class A/B" device labels.

---

## 3. Table-by-table consolidation plan

Current: 28 main-paper tables (25 `\input` + 3 inline). Target: **11 main**.

| Current | Action | New main table / figure |
|---|---|---|
| I Infrastructure | keep (expand: A/B = UAV/field unit, RAM, MIPS semantics) | T1 Infrastructure specification |
| II Workload classes | keep (separate from service table) | T2 Workload characteristics |
| III Grids | keep, redesign as master design matrix with run/gen/completed counts | T3 Experimental design |
| IV Scale outcomes | convert | **F4** multi-panel performance |
| V Class success | convert | **F4** panel (c) |
| VI Delay budget | convert | **F6** panel (a) |
| VII Clock | convert | **F5** panel (a) |
| VIII Deadline | convert | **F5** panel (b) |
| IX Mix | convert | **F5** panel (c) |
| X Latency decomposition | convert | **F6** panel (a) |
| XI Calibration ratio | convert | **F6** panel (b) |
| XII Decision cost | convert | **F7** or **F10** cost panel |
| XIII Algorithm ranking | convert | **F7** heatmap |
| XIV Rank agreement | convert | **F7** panel (c) |
| XV Factorial ANOVA | keep compact (factor, df, F, p, η²) | T4 Statistical model summary |
| XVI Placement distribution | convert | **F7** stacked bars |
| XVII Architecture effect by profile | convert | **F7** interaction heatmap (+ sign flip) |
| XVIII Placement by profile | convert | merged into **F7** |
| XIX Algorithm winners | convert | merged into **F7** |
| XX Hetero ANOVA | keep compact OR supplement with interaction plot | T5 (compact) / **F7** |
| XXI Failure causes | convert | **F8** panel (a) |
| XXII Architecture tests | convert | **F8** panel (b) dot-and-whisker |
| XXIII Paired contrasts | convert | **F8** panel (b), separate estimand |
| XXIV Mobility | convert | **F9** panel (a) |
| XXV Workload realism | convert | **F9** panel (b) |
| XXVI Trace by class | convert | **F9** panel (c) |
| XXVII Cross-port | keep compact | T6 Cross-port audit |
| XXVIII Common-trace cross-sim | convert + keep numeric support | **F10** + T7 (supporting) |
| XXIX Independent YAFS replication | keep separate in T7 or supplement | T7 / supplement (not merged) |

Main tables: **T1–T7 (7)** plus service specification table (if workflow arm is
ever executed) and a compact algorithm-cost summary = **≈10**. All detailed
matrices to supplementary material.

---

## 4. Figure-by-figure redesign plan

Current: 17 figure environments. Target: **10** principal figures.

| New | Content | Replaces |
|---|---|---|
| F1 Application model | service DAG, data sizes, both workload arms (one marked unexecuted) | current application_graph.png |
| F2 Infrastructure + deployments | topology, capacities, eligibility matrix, cloud-only marked illustrative | infrastructure.png + deployments.png |
| F3 Experimental framework | spec → adapters → equivalence → metrics; status legend | framework.png + pipeline.png |
| F4 Main performance and scale | 4 panels: success vs devices, p95 vs devices, class × config, effect CI | f1, f2, class results |
| F5 Temporal/deadline/mix sensitivity | 3 panels with log x-axis for interval | f10, f11, f12 |
| F6 Calibration and latency | component plot + ρ curve; remove duplicate workflow figure | f11_temporal_fidelity + calibration.png |
| F7 Algorithms and infrastructure | heatmap + rank stability + placement stacks + sign-flip interaction | f13, f16, host/hetero tables |
| F8 Failures, energy, network | stacked failures + effect dot plots + energy denominator panels | f3, f4, f4b, f5, f8 |
| F9 Workload and mobility robustness | 4 trace arms + class panel + mobility models | f7 trace, mobility, f8 sensitivity |
| F10 Cross-simulator and cross-port | heatmap: platform × config × interval; blank = unavailable; separate common-trace vs independent; cross-port deltas | replication.tex, common_level_b, parity |

Remove/merge: `orchestrator.png` (fold key detail into F3 caption), duplicate
calibration workflow (F6), pipeline.png (folded into F3), standalone
failure/energy/network figures (folded into F8).

All figures: consistent colours, 95% CI for means, sample sizes in captions,
explicit mean/median/p95 labels, no 3D, no decorative grids.

---

## 5. Numerical/statistical audit report

See §1.1–1.2. Summary: **all headline numbers reproduce**; the defects are
presentation (p = 0.000, missing subset labels, SD/CI confusion) and
claim–evidence mismatches (§1.3), not fabricated data.

---

## 6. Experiments still required

1. Dependency-aware workflow arm in PureEdgeSim: workflow completion,
   dependency violations, parent-child transfer delay/volume, placement
   distribution of dependent services.
2. iFogSim2 actuator forwarding at commit `5f68d394…` (upstream defect
   documented in `crosssim/adapters/ifogsim2/BUG_REPORT.md`).
3. A second application scenario (industrial/smart-city) for external validity.
4. Resource-contention experiment: multiple applications, higher rates,
   constrained CPU/memory, measurable network contention.
5. Cloud-only deployment if it is to be an experimental baseline.

Until (1) and (2) are executed, RQ3's dependency part and four-platform
validation remain explicitly open.

---

## 7. Manuscript rewrite (separate step)

This document intentionally contains **no rewritten manuscript text**. The next
action after your approval is: apply C1–C12, implement the table/figure mapping
above, regenerate all figures from the audited CSVs, then rewrite the paper.
