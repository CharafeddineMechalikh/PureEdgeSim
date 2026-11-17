# Research Plan — Overhead-Aware Agentic Edge Intelligence: Joint Optimization of LLM/RAG Reasoning, Scheduling, and Network Resources under Latency and Energy Budgets

- **Folder:** `02_overhead_aware_agentic_edge/`
- **Paper draft:** `paper/main.tex`
- **Scenario:** `scenario/run_scenario.py` + `scenario/settings/`
- **Status:** pilot scenario implemented and executed (PureEdgeSim Python, 30 devices, 5 simulated minutes, seed 42)
- **One-line goal:** make agentic (LLM/RAG) overhead a first-class, jointly optimized cost in edge scheduling instead of assuming reasoning is free.

---

## 1. Objectives

| ID | Objective | Why it matters |
|---|---|---|
| O1 | Formalize **agentic overhead**: effective compute `w̃_i = w_i + o_i`, payload `d̃_i = d_i + δ_i`, plus control/verification signaling. | Agentic pipelines spend real CPU, bytes, and time on retrieval, prompting, tool use, and verification. |
| O2 | Formulate **joint placement + fidelity + resource allocation** under latency and energy budgets. | Placement alone cannot absorb reasoning overhead. |
| O3 | Design an **overhead-aware orchestrator** that trades model fidelity, retrieval caching, tier choice, and batching. | The contribution must beat naive placement under the same overhead. |
| O4 | Derive a **feasibility envelope**: maximum sustainable load as a function of overhead level. | Practitioners need to know when agentic edge is viable. |
| O5 | Provide reproducible **agentic workload profiles** and an evaluation harness. | Enables comparable follow-up work. |

---

## 2. Scenario mapping (what is simulated)

| Element | Implementation |
|---|---|
| Devices | 30 devices, 5 simulated minutes, seed 42 |
| Infrastructure | Same for both cases; `EDGE_AND_CLOUD` and `ALL`; `TRADE_OFF` |
| Baseline workload | Edge inference (20/min, 4,000 MI, 500 KB container, 200 KB request) + control update (15/min, 1,200 MI) |
| Agentic workload | Inflated to proxy LLM/RAG overhead: inference 18,000 MI, 2,500 KB container, 1,200 KB request; control reasoning 6,000 MI, 900 KB container |
| Pilot result | Baseline: 2,078–2,089/2,100 successes, 0.07–0.09 s, ≈11.7 Wh. Agentic: 2,009–2,083/2,100, 0.32–0.36 s, 14.3–15.1 Wh, network 13–24 s |

**Interpretation:** the agentic profile keeps headline success high at this load but inflates delay ~4×, network time up to ~7.5×, and energy ~20–28% — exactly the hidden cost the paper targets.

---

## 3. Expected results

| ID | Hypothesis / expected result | Evidence already available |
|---|---|---|
| E1 | Agentic overhead raises delay **2–5×**, network **3–8×**, energy **+10–30%**. | Pilot: 4.3–4.5× delay, up to 7.5× network, +20–28% energy (edge+cloud). |
| E2 | Overhead-aware scheduling recovers **≥50%** of the success loss and **≥30%** of delay inflation vs naive placement at equal overhead. | Not yet implemented. |
| E3 | For each overhead multiplier there is a **maximum arrival rate** above which deadline satisfaction falls below 95%. | Requires rate sweep. |
| E4 | Caching and reduced fidelity shift the envelope by **20–40%**. | Requires cache-hit/fidelity sweeps. |
| E5 | Gains are largest when edge capacity is constrained and the WAN is the bottleneck. | Implied by pilot network inflation. |

---

## 4. Implementation checklist

### Done
- [x] Baseline and agentic application profiles (`applications_baseline.xml`, `applications_agentic.xml`).
- [x] Two-architecture comparison under a fixed seed.
- [x] Aggregated `feasibility_summary.json` and console reporting.
- [x] Project README.

### To do — model and algorithm
- [ ] Explicit overhead parameters (compute multiplier, payload multiplier, control-message count) exposed in settings/generator.
- [ ] RAG-style **service graph**: retrieve → reason → verify → act, with subtask dependencies.
- [ ] `OverheadAwareOrchestrator`: budget-aware placement + fidelity selection + cache-aware routing.
- [ ] Optional batching / parallel tool-call model.
- [ ] Energy-budget constraint for battery-powered devices.

### To do — experiments
- [ ] Overhead-multiplier sweep `{1×, 2×, 4×, 8×}`.
- [ ] Arrival-rate sweep to locate the feasibility boundary.
- [ ] Cache-hit-ratio sweep `{0, 0.5, 0.9}`.
- [ ] Fidelity levels (small/medium/large model proxies).
- [ ] ≥10 seeds with confidence intervals.

### To do — paper
- [ ] Formal problem statement and complexity discussion.
- [ ] Algorithm description + pseudocode.
- [ ] Feasibility-envelope section and break-even analysis.
- [ ] Threats to validity: proxy overhead vs measured LLM traces.

---

## 5. Metrics to use

| Metric | Definition / formula | Notes |
|---|---|---|
| Overhead ratio (delay) | `(T_agentic − T_baseline) / T_baseline` | Per architecture |
| Overhead ratio (energy) | `(E_agentic − E_baseline) / E_baseline` | Per successful task preferred |
| Network amplification | `net_agentic / net_baseline` | WAN and total |
| Deadline satisfaction | `successes / generated` | Primary service metric |
| Budget violation rate | fraction of tasks exceeding latency or energy budget | Requires budget definitions |
| Energy per successful task | `energy / successes` | Efficiency |
| Success per Wh | `successes / energy` | Inverse view |
| Max sustainable arrival rate | largest rate with deadline satisfaction ≥95% | Envelope metric |
| Cost per agentic decision | ms and Wh per decision | Control-plane cost |
| Pareto hypervolume | delay vs energy front | If multi-objective policy is added |

**Primary outcomes:** deadline satisfaction, p95 delay, energy per successful task, feasibility boundary.
**Secondary outcomes:** network amplification, tier shift, cost per decision.

---

## 6. Acceptance criteria

| Criterion | Target | Verification |
|---|---|---|
| Scenario reproducibility | Baseline and agentic numbers reproduce with fixed seed | Rerun + compare CSV |
| Policy gain | ≥10% success improvement **or** ≥20% delay reduction vs naive at equal overhead | ≥10 seeds, CI |
| No budget regression | Budget violation rate not worse than naive | Constraint check |
| Envelope coverage | ≥3 overhead levels × ≥4 arrival rates | Results grid |
| Sensitivity | Cache-hit and fidelity effects quantified | Sweep plots |
| Assumption transparency | Overhead model documented; proxy vs real LLM gap discussed | Paper review |
| Artifact | Profiles, script, seeds, outputs, README | Bundle check |

---

## 7. Potential figures

| Fig. | Type | Content / axes | Message |
|---|---|---|---|
| F1 | Pipeline diagram | Agentic loop (retrieve → reason → verify → act) with compute/byte overhead annotations | Where overhead comes from |
| F2 | Multi-line plot | Delay, energy, network vs overhead multiplier | Cost scaling |
| F3 | Feasibility envelope | Success ratio vs arrival rate; shaded feasible region | When agentic edge works |
| F4 | Stacked bars | Tier placement baseline vs agentic | How placement shifts |
| F5 | Bar plot | Energy per successful task | Efficiency loss |
| F6 | Pareto scatter | Delay vs energy for naive vs overhead-aware | Policy benefit |
| F7 | Line plot | Gain vs cache-hit ratio | Mitigation potential |
| T1 | Table | Overhead parameters and workload profiles | Reproducibility |
| T2 | Table | Policy comparison with CIs | Main result |

---

## 8. Risks and threats to validity

- **Overhead is a proxy**: task length and payload inflation, not measured token-level LLM latency.
- RAG retrieval quality/accuracy is not modeled; only its cost is.
- No real model-serving stack (GPU memory, batching, KV-cache) is simulated.
- Results depend on the chosen inflation factors; sensitivity analysis is mandatory.
- PureEdgeSim reports aggregate rows; p95 requires custom instrumentation.

## 9. Reproduction

```powershell
$env:PYTHONPATH = "_deps_pureedgesim_python/src"
python 02_overhead_aware_agentic_edge/scenario/run_scenario.py
```

Outputs: `scenario/output/baseline/`, `scenario/output/agentic_overhead/`, and `scenario/output/feasibility_summary.json`.
