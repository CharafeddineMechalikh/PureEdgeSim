# Review record — P2

Manuscript: *Shedding Work, Not Orchestrating Better: A Component Ablation of
Overhead-Aware Scheduling for Agentic Edge Services*

---

## Round 1 — internal review of the first draft

**R2.1.** The overhead-aware orchestrator was compared only against the
simulator default, so the reported gain was attributable to whichever component
the default lacked.
*Resolution.* Added a static edge-only baseline and four ablation arms.

**R2.2.** The "edge" baseline used a different application file, so it differed
from the treatment in workload as well as placement.
*Resolution.* Found while preparing Round 2; the profile switch was removed so
that every arm runs the same agentic workload and differs only in orchestrator.

**R2.3.** The mitigation grids were run at nominal overhead, where the
mechanisms cannot engage, and produced three flat figures.
*Resolution.* Re-run with an overhead factor of {1, 3} and rebuilt as ablations.

---

## Round 2 — external review (reject, resubmit)

The referee's central objection: the crossing-point claim rests on the naive
baseline, and the strong static baseline is never evaluated under stress; the
only mechanism shown to matter is fidelity reduction, which has no cost model.

**R2.4 (fatal).** Static baseline never tested under stress.
*Resolution.* Added `edge` to the multiplier grid (40 new runs) and built a
dedicated ablation grid (80 runs) with `aware_nofid` (ladder off) and
`aware_flat` (cache and energy terms zeroed). Result: edge-only with the ladder
is identical to the joint orchestrator at every level (0.990 / 0.784 / 0.498 /
0.400); removing the ladder collapses success to the naive level (0.571 at
1.5x, 0.000 at 2x); removing cache and energy changes nothing. The paper was
rewritten around this ablation, which is now its thesis.

**R2.5.** Fidelity is free, so the stressed-regime gains are unbought.
*Resolution.* Added a quality-weighted success metric (mean of success x
fidelity) and a mean-fidelity column. At 2x the weighted value is 0.249 against
a raw 0.498; at 3x it is 0.100 against 0.400, with mean fidelity 0.540. The
paper reports both everywhere and recommends the weighted metric as primary.

**R2.6.** The envelope is advertised as 4x4 but only two overhead levels were
run.
*Resolution.* Extended the rate grid to four overhead levels (160 new runs);
Table VI is now a genuine 4 x 4 envelope.

**R2.7.** Mann-Whitney is used on paired seeds; p < 0.001 is questioned;
Cliff's delta is reported for zero differences.
*Resolution.* Added `stats.compare_paired` (Wilcoxon signed-rank on per-seed
differences, paired bootstrap CI, paired dominance). The table now separates
overhead level, contrast and metric, suppresses stars on zero differences, and
labels dominance correctly. The Holm family is stated (24 tests).

**R2.8.** Table architecture is unstated; task counts disagree (2,100 vs 2,175
vs "20+15 req/min").
*Resolution.* Architecture is now a column in Table III; the authoritative
count is 2,100 from the artifact and the "20+15" caption was removed. The
workload table lists the two classes actually used.

**R2.9.** Figures show a single x tick and unreadable categories.
*Resolution.* Root cause was a numeric locator installed after categorical
labels; fixed centrally in `research_common/figures.py`. All figures regenerated.

**R2.10.** Beta has no value or units; the energy argument is unclear; the
fallback is undefined for an empty set.
*Resolution.* Beta = 0.02 s, with the 24 ms container-transfer calculation that
motivates it; energy is stated to accrue over predicted busy time including
queueing; the algorithm initialises the fallback explicitly.

**R2.11.** Novelty is thin.
*Resolution.* Reframed as a component-ablation and measurement study with a
negative result; the title no longer claims a better orchestrator.

---

## Round 3 — proof and typography

- Build: 8 pages, 0 errors, 0 undefined citations/references, 0 missing files.
- Three overfull boxes found and fixed: the six-column multiplier table (moved
  to `table*`), the ablation table (shortened arm labels), and the paired-test
  table (restructured into overhead / contrast / metric columns).
- Two "Missing $" errors traced to an unescaped `aware_nofid` in a table note.
- Diagram converted from TikZ to Mermaid PNG (`paper/diagrams/overhead_loop.png`).
- Known gaps, stated in threats: fidelity has no accuracy cost in the simulator;
  the overhead model is linear and unfitted; placement may be under-stressed
  because the edge tier wins at every level.


---

## Round 4 — closure of the external review

**M1 strong baseline and ablations.** Done. `edge` added to the multiplier grid
(4 levels x 10 seeds) and a five-arm ablation built, including the
`naive+fidelity` arm the referee asked for. The gain partitions as:

| Arm at 2x | Success |
|---|---|
| naive | 0.000 |
| naive + fidelity | 0.375 |
| edge + fidelity | 0.498 |
| aware, flat (no cache/energy) | 0.498 |
| aware | 0.498 |
| aware, no fidelity | 0.000 |

So the ladder supplies 75% of the gain, the static edge tier 25%, and the joint
scorer nothing. The paper's claim was corrected accordingly (it previously said
the ladder alone captured everything).

**M2 priced fidelity.** Done via utility-weighted success and a per-class table:
0.498 -> 0.249 at 2x, 0.400 -> 0.100 at 3x; per class, the hard class is the
binding one at 1.5x (0.495 success, 0.744 fidelity) while RAG is served at
0.995 fidelity, and at 2x both are near 0.5 success and 0.75 fidelity. A
measured quality-accuracy curve remains out of scope (needs a testbed).

**M3 overhead calibration.** Partially done. The c-versus-m decomposition is
explained, the linear surrogate and its bias directions are stated. No
trace-grounded calibration and no prefill/decode split.

**M4 internal consistency.** Done. Counts reconciled to 2,100; envelope is a
true 4x4; architecture column added; paired Wilcoxon replaces Mann-Whitney and
the Holm family is stated (24 tests); zero differences carry no stars; energy
is reported in mWh per success; traffic per request added (0.96-2.68 MB,
identical across policies).

**M5 DAG instantiation.** Not done; stated as a limitation. The simulator
supports subtask graphs but the overhead model in this paper is a scalar
inflation, and a DAG arm would be a separate study.

**M6 novelty.** Done by reframing as a component-ablation/measurement paper with
a negative result.

**Minor items.** Figure ticks, beta value and units, energy-argument semantics,
algorithm fallback, warm-up statement, resolution bound, table formatting and
the battery null result are all addressed. Network metrics are normalised per
request. The 40x WAN amplification is now reported as two orders of magnitude
with the per-request traffic showing the bytes are identical.

**Out of scope, declared.** Measured fidelity-accuracy curve (testbed) and DAG
instantiation (separate model).

---

## Round 5 — full closure of the 12-section major revision

`results/REVISION_MAP.md` contains the item-by-item map. Summary:

- **M1 / factorial.** Complete 3×2 placement × fidelity design, 480 runs, 20
  seeds; ANOVA placement η²=0.793, ladder η²=0.997, interaction η²=0.736. Gain
  partitions 76/24/0 at 2×.
- **M2 / nonlinear cost.** Four cost laws C(f)=0.05+0.95f^γ; success unchanged,
  executed quality 0.462–0.681 at 3×.
- **M3 / compute vs payload.** Independent 4×4 lattice; compute carries the
  effect, payload is flat except one cell.
- **M4 / cache and components.** Live-cache ablation (hit ratio 0.5, 300 runs):
  no arm differs; cache term changes 0.000 picks; objective decomposition shows
  energy = 0.06% of score.
- **M5 / workload.** `WORKLOAD.md` reconciles 2,100 = 24 generators × 17.5/min ×
  5 min and retires the superseded 2,175.
- **M6 / statistics.** Balanced ANOVA, 40 + 24 Holm-corrected seed-matched
  contrasts, exact zero differences untested, `POWER.md` gives MDE 0.003.
- **M7 / host selection.** All tiers eligible: cloud share 0.000 in all cells.
- **M8 / classes.** Per-class table inside the factorial for both classes.
- **M9 / horizon.** 5/15/30 min, spread 0.3 points.
- **M10 / seeds.** 20 seeds in the null-bearing grids, justified by power.
- **M11 / energy.** No deaths at 20/50/100 Wh; battery null.
- **M12 / Docket LLM realism.** Example 14 in the authors' serving model, which
  shares PureEdgeSim's event engine (so it is a cost-model check, not a second
  simulator),
  20 cells × 5 seeds; it reverses the placement null in the weak-edge regime and
  therefore bounds the claim instead of pretending to replicate it.

Manuscript retitled to *When Does Joint Placement--Fidelity Optimization Matter?
A Factorial Study of Agentic Edge Computing*. Build audit: 15 pages, no errors,
no undefined references. Acceptance: 24/24.

---

## Round 3 — TMC pre-submission review (Phase 1 applied)

**Scientific correctness first, as the reviewer required:**

1. **Fidelity-selection inconsistency resolved.** The paper previously printed
   a joint minimization over $(v,f)$ which, as the reviewer showed, would always
   choose the lowest feasible fidelity. The implementation is a two-stage
   lexicographic policy: select the highest fidelity for which some host meets
   the deadline (Eq.~\ref{eq:fidelity-selection}), then rank hosts by
   Eq.~\ref{eq:host-selection}. $f$ (selected level), $C(f)$ (executed cost
   fraction) and $q(f)$ (retained quality, not modelled) are now separate;
   $C(f)$ enters the feasibility test, which explains the cost-curve result.
   Algorithm 1 now states the implemented procedure verbatim.
2. **Numerical reconciliation.** `scenario/consistency_check.py` verifies every
   headline against the raw CSVs. The 20-seed factorial ($0.496$ at $2\times$)
   is authoritative; the 10-seed scaling grid ($0.498$) is labelled wherever it
   appears. A dedicated authoritative-results table was added.
3. **Statistics.** Two-sided power formula stated explicitly
   ($2.80\times0.005/\sqrt{20}=0.0031$); TOST equivalence with a stated 1-point
   margin added for the exact zero; partial $\eta^2$ separated from the marginal
   decomposition.
4. **Compute/payload.** The claim was weakened to "dominant in most cells, with
   payload effects near boundaries" and a 250-run refined $5\times5$ lattice
   (`cp_fine`) was executed; it shows threshold effects at $c=1.5$ and $c=1.7$.
5. **Energy/payload calculations.** The battery-dependent mist-energy
   accumulator in the vendored simulator was a real defect; it was fixed
   (`simulator_patches/energy_accounting.txt`), the 60-run `energy` grid was
   re-run, and energy is now identical across initial-charge settings.
6. **Network model.** The communication term is defined as propagation-only in
   the orchestrator's prediction. A 180-run bandwidth-sharing grid maps the
   boundary (all policies fail at the tested overheads), and a 90-run co-tenant
   grid shows that contention reverses the placement ranking.
7. **RQs, title, terminology and overclaims** revised per the reviewer; RQ1--RQ5
   are now answered explicitly in the conclusion.

Phase 2 and 3 of the review, all executed: capacity-ratio sweep (400 runs),
workflow DAGs including a 2--5 service mixed-chain arm (120 runs), refined
compute/payload lattice (250 runs), objective-weight sensitivity (100 runs),
fidelity-ladder granularity (480 runs), quality-curve sensitivity over six
monotone families (post hoc), and a full six-arm, twenty-seed Docket check
(480 requests). Total artifact: 4,240 PureEdgeSim runs plus the Docket grid.

Still genuinely external: a *measured* LLM quality--computation curve (no
quality evaluator or model deployment exists in either artifact; we bound the
conclusion over a family instead) and validation on real edge/cloud hardware
with a production trace. Docket is the authors' simulator on the same engine,
so it is reported as a serving-model robustness check, not an independent
replication.
