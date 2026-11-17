# Review record — P4

Manuscript: *Cost-Aware Adaptive Security for Federated Agentic Edge Computing:
Trading Cryptographic Level against Deadline*

---

## Round 1 — major revision

**R4.1.** The first draft claimed adaptive security "wins", but the data show
static L3 and L4 achieve higher protected utility (0.931 and 0.970 versus
0.874). A reviewer would reject this as cherry-picking the baseline.
*Resolution.* The claim was replaced with a Pareto statement. Adaptive dominates
L1 and L2 on both delay and utility, is not dominated by L3/L4 (which are slower
and more secure), and sits on the frontier. The paper says explicitly that it is
third on utility and why.

**R4.2.** The cryptographic parameters are asserted with no provenance.
*Resolution.* The parameter table was rebuilt with a per-row citation into the
shared bibliography (FIPS 203/204, CKKS, Secure Aggregation, plus the two 2026
systems papers). Values are unchanged; only the sourcing and layout changed.

**R4.3.** The load and model-size sweeps are described in the wrong direction.
*Resolution.* Recomputed and rewritten. Under load, L4 peaks at 2× (0.951) then
collapses to 0.729 at 4× while L2 holds 0.830; under model size, L4 falls to
0.043 at 4× and even the unprotected policy outperforms it (0.519). The paper
now reports this inversion and states it as a limitation of the fixed risk
weight rather than tuning the weight after the fact.

**R4.4.** The privacy leakage claim ("grows with threat and model size") was
attached to a figure that only varies threat.
*Resolution.* The text now attributes the threat dependence to the figure and
the size dependence to the size sweep, which is where it is measured.

**R4.5.** No formal statement supports the trade-off.
*Resolution.* Added Proposition 1 (profile monotonicity: attack probability
non-increasing and delay non-decreasing in the profile index) with proof.

---

## Round 2 — numeric verification

| Claim | Source | Status |
|---|---|---|
| adaptive 1.000 / 0.234 / +3.7% / 0.126 / 0.874 | `main.tex` | consistent |
| L4 +107.9%, 2.06×, 0.019, 0.970 | `main.tex` | consistent |
| 69.6% of the attack reduction | (0.415−0.126)/0.415 | recomputed |
| 3.4% of the delay cost | 3.7/107.9 | recomputed |
| τ sweep 0.935→0.814 vs 0.848→0.367 vs 0.981→0.959 | `attack.tex` | consistent |
| load 0.830 / 0.736 / 0.729 / 0.584 at 4× | `all_runs.csv` | verified |
| size 0.519 / 0.439 / 0.350 / 0.043 at 4× | `all_runs.csv` | verified |
| 680 runs | `all_runs.csv` | 120+240+160+160 |

One error found: the delay-overhead narrative originally called L4's handshake
"22 ms" while the table lists 0.022 s — the same value, but the units were
inconsistent; both are now in seconds.

---

## Round 3 — proof and typography

- Build: 8 pages, 0 errors, 0 undefined citations/references, 0 missing files.
- The provenance table overflowed by 201 pt and did not span both columns
  correctly. It was rebuilt with short row labels and per-row citations, and now
  fits within `table*`.
- The TikZ decision figure collapsed (overlapping nodes, empty boxes) and was
  replaced by a Mermaid diagram rendered to PNG (`security_loop.png`).
- Remaining known gaps, all stated in threats: the simulator executes no
  cryptographic code (parameters are calibrated, not measured); the attack model
  is a proxy with no reduction; protected utility weights a missed deadline and
  an attack equally.
