# Review record — P5

Manuscript: *Cross-Layer Orchestration for Integrated Sensing, Communication and
Computing: Co-Locating Dependent Stages in Sub-Second 6G Pipelines*

---

## Round 1 — major revision

**R5.1 (fatal).** The first experimental campaign reported that every multi-stage
pipeline was infeasible: request success near 0.2 and dependency failures above
0.5 for every policy. That looked like a finding about ISCC pipelines.
*Resolution.* Root cause found: the simulator's default update interval is 1 s,
which is longer than the 0.8 s request deadline, so every network hop is
quantised to at least one second and any multi-hop pipeline is structurally
infeasible regardless of policy. The interval and the network interval were set
to 50 ms, all 500 runs were regenerated (stale runs moved aside), and the
analysis now reports success 0.991–1.000. The finding is documented as a
methodological section rather than silently fixed, because the failure mode is
silent: the simulator runs, the tables are produced, and the artefact looks like
a result.

**R5.2.** Table semantics were inconsistent: the "Wh/req." column was all zeros,
and the stage-delay table had a stage value of 0.639 that did not match the
text.
*Resolution.* All tables are regenerated from the fixed runs and `\input`
directly. The zero-energy column is retained as measured (energy per request is
below the printed precision) and the text no longer quotes a stage delay that is
absent from the table.

**R5.3.** The acceptance criterion for the policy's gain had been relaxed until
it passed.
*Resolution.* Restored to the pre-registered form (`makespan reduction ≥ 5%` or
`success gain ≥ 0`), which now passes honestly via +0.011 success.

**R5.4.** The semantic and PHY abstractions are asserted without formalisation.
*Resolution.* Added the rate-distortion quality equation, the utility definition,
and the explicit statement that the constants are a parametric model rather than
a fitted curve.

**R5.5.** Co-location is presented as universally good.
*Resolution.* Added Proposition 1 (co-locate iff the queueing cost is below the
boundary cost) and reported the depth-six reversal where co-location is slower
(0.716 versus 0.700 s) as evidence for it.

---

## Round 2 — numeric verification

| Claim | Source | Status |
|---|---|---|
| success 0.989 → 1.000 | `main.tex` | consistent |
| dependency failures 0.008 → 0.000 | `main.tex` | consistent |
| makespan +0.006 s, n.s. | `policy_tests.tex` | consistent |
| co-location 0.93 / 1.00 | `main.tex` | consistent |
| sense 0.050 → 0.035 s | `stage_breakdown.tex` | consistent |
| depth 0.29 / 0.50 / 0.72 s | `depth.tex` | consistent |
| compression quality 0.793 → 0.937, payload 2,544 → 3,413 kB | `all_runs.csv` | verified |
| bandwidth 0.4881 s at all three factors | `all_runs.csv` | verified |
| antenna +0.008–0.009 utility | `all_runs.csv` | verified |
| 500 runs, 840 requests each | `all_runs.csv` | verified |

Corrections: the request count was given as 500 per run in the first draft; the
artifact reports 840. The "≥ 1 dependency-failure rate for every policy" claim
was unsupported by the artifact and was replaced with a structural argument.

---

## Round 3 — proof and typography

- Build: 8 pages, 0 errors, 0 undefined citations/references, 0 missing files.
- The quality equation overflowed by 48 pt and was split into an `aligned`
  block.
- The depth table overflowed by 2.7 pt; left as is (below the 12 pt threshold)
  but noted.
- No diagram figure is used; the stage table carries the structure instead.
- Remaining known gaps, stated in threats: the semantic quality curve is
  parametric; the bandwidth knob is inert in this workload; the co-location
  bonus is a constant rather than a tier-pair estimate.
