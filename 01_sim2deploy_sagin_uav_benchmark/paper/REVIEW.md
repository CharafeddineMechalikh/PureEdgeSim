# Review record — P1

Manuscript: *The Architecture Effect Is a Class Mix: A Reproducible Benchmark and
Cross-Port Audit for Multi-Tier Edge Orchestration*

Three rounds. Round 1 is an external-style adversarial review of the first full
draft. Round 2 checks every numeric claim against the artifact. Round 3 is a
proof/typography pass on the compiled PDF.

---

## Round 1 — major revision

**R1.1 (fatal).** The headline "architecture dominates scale" is an artefact.
The reviewer showed that the edge-only success ratio (0.121) equals the share of
the aggregation class, and that the full-architecture ratio equals the sum of
the aggregation share and the sensing share weighted by its success.
*Resolution.* Confirmed against the per-task records and adopted as the paper's
central finding. Added `results/tables/class_breakdown.tex` (class × architecture)
and rewrote the abstract, introduction, results and conclusion around it. The
old causal claim ("adding tiers makes planning possible") is withdrawn.

**R1.2.** Architecture and scale are not comparable axes; the deployment is not
capacity-saturated, so "scale does not matter" is unsupported.
*Resolution.* Removed "order of magnitude"; the paper now says "between eight and
fifteen times" and states explicitly that the scale result is about a
fixed-capacity deployment. An infrastructure-scaled arm was not run and is named
as the natural continuation.

**R1.3.** Most of the protocol is documented simulator guidance.
*Resolution.* Out of scope by author instruction (the repository is the author's
own). No change.

**R1.4.** The WAN divergence may be a version skew.
*Resolution.* Both implementations are at 6.1 (Python 6.1.0, Java
`pureedgesim-6.1.jar`). The version is now stated in the parity table caption and
in the text; the divergence is therefore residual, not skew.

**R1.5.** "Workload parity passes" is arithmetic, not evidence.
*Resolution.* The paper now says so: 9.95 req/min × 40 devices × 5 min = 1,990
for any seed. The distributional test is reported as inconclusive (minimum
attainable p at 5+5 is 0.0079), not as a pass.

**R1.6.** Statistical test/pairing mismatch, zero-variance control, pseudo-replication.
*Resolution.* Methods section now states that Mann–Whitney is applied to arms
treated as independent, that Cliff's δ against a zero-variance control is
trivially ±1 and is not interpreted as an effect size, and that seeds are
comparable across architectures but not across fleet sizes.

**R1.7.** The trace comparison confounds arrival rate with deadline mapping.
*Resolution.* The mapping is now stated as an equation (length, request bytes,
result bytes, deadline) and the two confounds are separated in the text. The
trace arm is shown to offer 781 requests at 2.6 s⁻¹ with 2–5 s deadlines, versus
1,990 at 6.6 s⁻¹ with 0.2/0.8/60 s. A rate-matched arm was not run and is
declared.

**R1.8.** The trace inversion (E+C below edge-only) is unremarked.
*Resolution.* Added a dedicated paragraph: with 2–5 s deadlines the edge tier
suffices and the WAN is a strictly slower option, so the extra tier hurts. This
is now the paper's clearest example of a result a pooled metric cannot express.

**R1.9.** Numerical errors: WAN "monotone in fleet size"; 4.7 pp mixed with
12.9 pp; energy spread; "order of magnitude".
*Resolution.* All corrected. The WAN paragraph now reports the non-monotonicity
(9.6 → 14.4 → 13.6 s; round-robin peaks at 40 devices), the per-seed and
per-arm gaps are labelled separately, and energy per generated task is stated to
be within one percent across architectures.

**R1.10.** Presentation: missing tick labels, broken legend, unlabelled axes.
*Resolution.* Root cause was `_style_axis` installing a numeric locator after
the categorical labels were set, which discarded all but the first label.
`research_common/figures.py` now sets categorical ticks after styling and
rotates long labels; the stacked-bar legend is placed below the axis. Added
`failure_causes.tex` so the decomposition has numbers, not only a figure.

**R1.11.** The SAGIN/UAV title is not earned by the model.
*Resolution.* Retitled to "multi-tier edge orchestration"; the model is
described as multi-tier, and the absence of satellite backhaul and UAV energy is
in threats to validity.

---

## Round 2 — numeric verification

Every number in the prose was checked against `results/tables/*.tex`:

| Claim | Source | Status |
|---|---|---|
| edge-only 0.121, full 0.648 pooled | `scale.tex` | consistent |
| class shares 0.528 / 0.352 / 0.121 | `class_breakdown.tex` | consistent |
| recombination 0.121 + 0.528·0.779 + 0.352·0.374 = 0.664 | table | matches pooled 0.660 within seed noise; text says "matches" |
| required MIPS 10,000 / 15,000 / 333 | `applications.xml` + cores | arithmetic check |
| destination split 86.4% of 966 mist / 0 of 84 DC | seed-1 task CSV | verified, now labelled "first seed" |
| scale effect ≤ 0.065 | `scale.tex` | 0.691→0.626 |
| p95 +0.24 s | `scale.tex` | 1.068→1.303 |
| energy 0.024 vs 0.131 Wh/success | `architecture_tests.tex` | consistent |
| energy/generated within 1% | `all_runs.csv` | 0.01572/0.01582/0.01583 |
| trace 0.921 / 0.834 / 0.885 | `trace.tex` | consistent |
| parity 1,210 / 1,466; WAN 208 vs 11.3 | `parity.tex` | consistent |
| 4.7 pp mean gap, 12.9 pp single seed | `parity.json` | separated in text |

Two mismatches were found and fixed during this round: the "monotone WAN" claim
(now corrected) and the unlabelled mixing of the mean and single-seed parity
gaps.

---

## Round 3 — proof and typography

- Build: 9 pages, 0 errors, 0 undefined citations, 0 undefined references,
  0 missing files, 0 overfull boxes > 12 pt.
- Two overfull boxes found and fixed: the trace mapping equation (split into
  `aligned`) and the class table (set in `\footnotesize`).
- The `fig:p1-scale` label was lost in the rewrite and restored on the
  scale figure.
- Abstract is 246 words, within the IEEEtran guidance.
- Diagrams converted from TikZ to Mermaid → PNG (`paper/diagrams/pipeline.png`).
- Remaining known gap: the Java arm reports no traffic figure in the parity
  table, so that row is asymmetric. It is retained rather than fabricated, and
  the caption says the two ports use different RNG streams.


---

## Round 4 — second external review (major revision)

The second referee accepted the class-mix diagnosis but showed that the
*explanation* was wrong: the paper's per-core feasibility bound contradicts its
own data (video needs 15,000 MIPS/core but mist provides 13,750, yet video
succeeds), and the arithmetic mixed two estimands (0.539 vs 0.528).

**R4.1 (fatal).** The per-core bound is invalid.
*Resolution.* Measured the simulator's allocation instead of assuming it: a task
is charged the node's aggregate throughput (edge 80,000 MIPS, cloud 100,000),
not one core. The bound was deleted and replaced by a measured delay budget
(Table VIII). This exposed the real mechanism: infrastructure placements start
1.01 s (edge) to 2.12 s (cloud) after release because the orchestrator runs on
the 1 s control tick, so every sub-second deadline fails regardless of compute.

**R4.2.** Mixed estimands (0.539 vs 0.528 vs 0.648).
*Resolution.* One canonical contrast is defined once (40 devices, TRADE_OFF,
1 s: 0.660 vs 0.121 = 0.539) and used everywhere; the pooled 0.648 is named as
such where it appears. The abstract no longer uses 0.648.

**R4.3.** "Aggregation succeeds on 1.000 under every architecture" contradicts
the table (0.975 under ALL).
*Resolution.* Corrected; the recomposition now uses 0.975 and gives 0.661,
matching the pooled 0.660.

**R4.4.** The prescription is never validated.
*Resolution.* Added the clock sweep: 3 architectures x {1.0, 0.2, 0.05} s x 10
seeds = 90 runs. The architecture effect falls from 0.539 to 0.415 to -0.004,
and the lag falls from 1.01 to 0.21 to 0.06 s. The paper is now a validated
causal claim rather than a diagnosis.

**R4.5.** Infrastructure table absent; update interval unaddressed.
*Resolution.* Added Table II (tiers, cores, MIPS, links) and a calibration
section with the deadline/interval rule and three checks.

**R4.6.** p95 is survivorship-biased; energy boundary unclear.
*Resolution.* p95 is now explicitly labelled a survivorship statistic and read
only alongside success and the delay budget; the energy boundary (computing
nodes, network energy excluded) is stated, and energy per generated task is
plotted (Fig. 5b).

**R4.7.** Statistics: paired design analysed as unpaired; invariance by
failure-to-reject.
*Resolution.* Caveats stated (MW treats arms as independent; zero-variance
control makes delta trivial). The interval sweep replaces a failure-to-reject
invariance claim with a monotone dose-response, which is stronger evidence.

**R4.8.** Trace: more confounds than acknowledged.
*Resolution.* The four confounds (deadline mapping, class mix, size
distribution, offered load 2.6 vs 6.6 per second) are listed, and the paper says
no rate-matched or deadline-matched arm was run.

**R4.9.** References four years out of date; 13 entries.
*Resolution.* Added benchmarking-methodology and simulator-ecosystem references
(Hoefler and Belli; Sargent; iFogSim; EdgeCloudSim; YAFS; Splitwise; serverless
trace characterisation), bringing the bibliography to 20 entries.

**Deferred and stated as such.** Cache-off arm, mix sweep, cross-simulator
replication, and the analytical microbenchmark for the port divergence were not
run. Each is named in threats or discussion rather than implied to be covered.


---

## Round 5 — closure of the second review

Every item that can be closed by experiment or re-analysis was closed; three are
declared out of scope with reasons.

**M1 arithmetic.** Done. One canonical contrast (40 devices, TRADE_OFF, 1 s):
0.660 vs 0.121, effect 0.539. Aggregation under ALL corrected to 0.975;
recomposition gives 0.661 against a pooled 0.660. The pooled 0.648 is named as
pooled where it appears.

**M2 feasibility bound.** Done, by replacement. The per-core bound was wrong
(tasks receive aggregate node throughput). A measured delay budget
(Table VIII) shows the lag: 1.01 s on edge sites, 2.12 s in the cloud, 0.13-0.48 s
on mist; a sensing task computes in 0.025 s and still fails. Result-size column
added to Table I.

**M3 validate the prescription.** Done. Three sweeps:
- interval: 0.539 -> 0.415 -> -0.004 at 1 s / 0.2 s / 50 ms;
- deadline scaling: 0.540 at 1x, 0.406 at 3x, 0.011 at 16x, with edge-only at
  0.972 against 0.983;
- class mix: edge-only tracks the aggregation share exactly (0.030, 0.068,
  0.121, 0.186, 0.444 against shares 0.030, 0.068, 0.121, 0.186, 0.444).

**M4 survivorship, energy, censoring.** Done. p95 labelled a survivorship
statistic; energy per generated task plotted (Fig. 5b) and the boundary stated;
an Unresolved column added to the failure table (zero to three decimals here).

**M5 statistics.** Done for the canonical contrast: seed-matched Wilcoxon with
Holm correction and paired dominance (Table XII), alongside the pooled
Mann-Whitney table retained for comparability. Mobility remains reported as
means with confidence bands in the figure; a cell-by-cell interval table was not
added.

**M6 port audit.** Partially done. Provenance and version parity (6.1/6.1)
stated; structural parity is now a defined statistic (total-variation distance
between outcome vectors) with a threshold of 0.05 declared before the
comparison; observed 0.129. Not done: replaying an identical mobility trace in
both ports, because the Java arm emits only the aggregate row and would need a
per-task exporter; and the analytical route microbenchmark.

**M7 external validity.** Done by scoping: the title and threats section state
that this is multi-tier edge orchestration, not SAGIN, and cross-simulator
replication is named as future work.

**M8 scale mechanism.** Done. Per-tier utilisation reported (cloud 0.01%, edge
0.16-0.26%, mist 1.48-2.24%): the deployment is not CPU-bound at any scale, so
the decline is not a capacity-saturation story. Duplicate sentence removed.

**M9 trace confounds.** Partially done. Four confounds listed; the trace arm is
now decomposed by class (Table XIII: 0.82-0.93 for every class under every
architecture), which confirms no class carries the aggregate. Not done:
rate-matched arm, multiple trace windows, and sensitivity of the token->MI
mapping.

**M10 references.** Done to 24 entries, adding iFogSim2, MLPerf, TailBench,
Glasserman-Yao, Sargent, Hoefler-Belli, Hoefler's simulator-ecosystem peers.
The uncited "12%/30%" strawman was removed.

**Out of scope, declared.** A cache-off arm needs a manager override that the
benchmark harness does not expose; cross-simulator replication needs a second
simulator; the analytical port microbenchmark needs per-task Java records.

---

## Round 4 — reconstruction plan closed

| Referee item | Artifact | Headline |
|---|---|---|
| Separate algorithm, architecture and simulator effects | 3,215-run crossed grid; `tables/algorithm_ranking.tex`, `factor_effects.tex` | Algorithm × architecture × interval interaction partial η²=0.917, F(24,567)=260.3. |
| At least three simulators | `crosssim/FINDINGS.md`, `SIMULATOR_SEMANTICS.md`, `tables/replication.tex` | YAFS 3.1 reproduces +58.2 pp at 1 s, +53.1 at 200 ms, 0.0 at 50 ms and 0.0 with deadlines ×16; EdgeCloudSim has no global period and cannot exhibit the artefact; iFogSim2 likewise by API inspection. |
| At least three algorithm families | Seven algorithms across three families (random, round-robin, trade-off, min-min, least-loaded, MCT, Q-learning), 630 runs | Rankings invert: Kendall τ=0.103 between 1 s and 50 ms; winner changes from round-robin to earliest-finish. |
| Ranking stability | `tables/ranking_stability.tex` | Four cross-interval comparisons; τ=0.72 between 1 s and 200 ms, collapsing only when the tick crosses the deadlines. |
| Negative-lag fix | Release→decision and decision→dispatch are 0 s (`tables/latency_decomposition.tex`) | The entire wait is in dispatch→execution (1.01 s MCT, 2.12 s least-loaded, 5.07 s min-min/video); moving it to the decision stage was a measurement error. |
| Measured calibration instead of D/10 | `tables/temporal_fidelity.tex`, `figures/f11_temporal_fidelity.pdf` | Success is stable for ρ=L_decision/D ≤ 0.5 and collapses for ρ>1; decision cost 0.150–0.518 ms, 0.6–3.0% of wall clock. |
| Real architecture effect under heterogeneity | `tables/heterogeneity.tex`, `hetero_interaction.tex`, 1,680 runs | At 50 ms: +65.7 pp with weak edge/fast cloud, −3.6 to −19.6 otherwise; at 1 s uniformly +28 to +34. The clock both manufactures an effect and understates a real one. |
| Trace confounds | `tables/trace.tex`, four arms, 120 runs | Load irrelevant (+2.7 pp rate-matched), deadline distribution decisive (−36.1 pp deadline-matched); architecture effect changes sign between native trace and deadline-matched arm. |

Manuscript retitled to *The Architecture Effect Was the Clock*. Build audit:
18 pages, no errors, no undefined references or citations. Acceptance: 17/17.
