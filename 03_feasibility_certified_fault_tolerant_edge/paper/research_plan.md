# Research Plan — Feasibility-Certified and Fault-Tolerant Edge Orchestration for Mission-Critical Services under Mobility and Non-Stationary Workloads

- **Folder:** `03_feasibility_certified_fault_tolerant_edge/`
- **Paper draft:** `paper/main.tex`
- **Scenario:** `scenario/run_scenario.py` + `scenario/settings/`
- **Status:** pilot scenario implemented and executed (PureEdgeSim Python, 50 devices, 6 simulated minutes, seed 7)
- **One-line goal:** decide at runtime whether a placement can meet its deadline, and keep the service alive when it cannot.

---

## 1. Objectives

| ID | Objective | Why it matters |
|---|---|---|
| O1 | Define a **runtime feasibility certificate**: an estimate of completion time with uncertainty, and an admission rule `P(T ≤ D) ≥ 1 − ε`. | Average-case optimization hides deadline violations. |
| O2 | Design **fault-tolerant orchestration**: primary/backup placement, retries, migration, replication, and checkpointing under mobility and failures. | Mission-critical services cannot rely on best-effort placement. |
| O3 | Characterize **infeasible regions** and failure propagation under mobility, device death, and non-stationary load. | Operators need to know when to reject, replicate, or degrade. |
| O4 | Quantify **certification overhead vs benefit** and analyze false admissions/rejections. | A certificate is only useful if its cost is acceptable. |
| O5 | Provide a reproducible **failure-injection and burst-load scenario**. | Enables comparable research on resilience. |

---

## 2. Scenario mapping (what is simulated)

| Element | Implementation |
|---|---|
| Devices | 50 mobile/battery devices, 6 simulated minutes, seed 7 |
| Workloads | Emergency control (18/min, **0.05 s**, 1,500 MI), V2X safety stream (12/min, **0.12 s**, 5,000 MI), telemetry analytics (8/min, 1.0 s, 9,000 MI) |
| Matrix | `EDGE_ONLY`, `EDGE_AND_CLOUD`, `ALL` × `TRADE_OFF`, `ROUND_ROBIN` |
| Reported failures | not executed (no resource/long wait), delay, mobility, dead device, dependency |
| Pilot result | EDGE_ONLY 1,812/2,892; EDGE_AND_CLOUD 1,783–1,794; ALL 1,578–1,735; delay failures 1,080–1,314; no mobility/dead/dependency failures in this configuration |

**Interpretation:** the pilot already shows that *adding tiers is not automatically better*: `ALL` reduced success under tight deadlines because mist/backhaul latency increased delay failures. This is exactly the infeasibility phenomenon a certificate must expose.

---

## 3. Expected results

| ID | Hypothesis / expected result | Evidence already available |
|---|---|---|
| E1 | Naive tier expansion can **reduce** success under tight deadlines. | Pilot: ALL 1,578–1,735 vs EDGE_ONLY 1,812. |
| E2 | Feasibility certification reduces deadline violations by **≥30%** at **≤10%** energy overhead. | Not yet implemented. |
| E3 | Tuned certificate achieves **false admission ≤5%** and **false rejection ≤10%**. | Requires estimator + margin sweep. |
| E4 | Fault tolerance recovers **≥80%** of tasks affected by injected failures. | Requires failure injection + recovery policy. |
| E5 | Non-stationary bursts create **transient infeasibility**; admission control prevents thrashing. | Requires burst profile. |
| E6 | p95 recovery latency stays within **2× the critical deadline**. | Requires instrumentation. |

---

## 4. Implementation checklist

### Done
- [x] Strict-deadline mission-critical workload set.
- [x] Failure-cause reporting (delay/mobility/dead/dependency).
- [x] Architecture × algorithm sweep and aggregated JSON.
- [x] Pilot execution and README.

### To do — model and algorithm
- [ ] Completion-time estimator (queueing + transmission + compute + mobility risk).
- [ ] `FeasibilityCertifiedOrchestrator` with safety margin and admission rule.
- [ ] `FaultTolerantOrchestrator`: primary/backup, retry, migration, optional replication.
- [ ] Recovery policy for device death, link loss, and battery depletion.
- [ ] Service classes with different criticality and rejection semantics.

### To do — experiments
- [ ] Failure injection hooks (kill device at time t, drop link, drain battery).
- [ ] Failure-intensity sweep (0%, 5%, 10%, 20% affected devices).
- [ ] Burst/surge arrival profile (non-stationary load).
- [ ] Safety-margin sweep for precision/recall trade-off.
- [ ] ≥10 seeds with confidence intervals.

### To do — paper
- [ ] Formal certificate definition, assumptions, and proof sketch/bounds.
- [ ] Algorithm pseudocode and complexity.
- [ ] Precision/recall and overhead analysis.
- [ ] Threats to validity: model-based estimates vs runtime measurements.

---

## 5. Metrics to use

| Metric | Definition / formula | Notes |
|---|---|---|
| Deadline violation rate | `failed(delay) / generated` | Primary safety metric |
| Certificate precision | `admitted-and-on-time / all admitted` | False-admission complement |
| Certificate recall | `admitted-feasible / all feasible` | False-rejection complement |
| False admission rate | `admitted-but-missed / admitted` | Target ≤5% |
| False rejection rate | `rejected-but-would-meet / feasible` | Target ≤10% |
| Success ratio under failures | `successes / generated` per failure intensity | Resilience |
| Recovery ratio | `recovered tasks / affected tasks` | Target ≥80% |
| Recovery latency | time from failure to service restoration (mean, p95) | Deadline-relative |
| Rejection rate | `rejected / generated` | Admission cost |
| Certification overhead | extra energy/delay per task vs uncertified | Target ≤10% energy |
| Availability per class | fraction of time the class meets its SLA | Service view |

**Primary outcomes:** deadline violation rate, certificate precision/recall, recovery ratio, overhead.
**Secondary outcomes:** rejection rate, recovery latency, availability.

---

## 6. Acceptance criteria

| Criterion | Target | Verification |
|---|---|---|
| Certificate precision | ≥95% | Admitted-task audit |
| False rejection | ≤10% | Feasible-task audit |
| Violation reduction | ≥30% vs uncertified | Controlled comparison |
| Overhead | ≤10% energy and ≤15% delay | Instrumented runs |
| Failure resilience | ≤20% success degradation at 20% device failures | Failure sweep |
| Recovery | ≥80% of affected tasks recovered within deadline | Injection runs |
| Recovery latency | p95 ≤ 2× critical deadline | Distribution plot |
| Statistical support | ≥10 seeds × ≥3 failure intensities | CIs |
| Formal grounding | Certificate assumptions and bounds stated | Paper review |

---

## 7. Potential figures

| Fig. | Type | Content / axes | Message |
|---|---|---|---|
| F1 | Loop diagram | Admit → place → monitor → recover | Certification and fault-tolerance workflow |
| F2 | Line plot | Deadline violation rate vs load, certified vs naive; feasibility boundary | Safety improvement |
| F3 | Precision–recall curve | False admission vs false rejection as safety margin varies | Operating-point selection |
| F4 | Bar/line plot | Success ratio vs failure intensity | Resilience |
| F5 | CDF | Recovery latency | Tail behavior |
| F6 | Scatter | Energy overhead vs violations avoided | Cost–benefit |
| F7 | Stacked bars | Failure causes (delay/mobility/dead/dependency) | Where losses occur |
| T1 | Table | Certificate parameters and service classes | Reproducibility |
| T2 | Table | Failure scenarios and outcomes | Main result |

---

## 8. Risks and threats to validity

- Failure injection is **simulation-level**; no real hypervisor, kernel, or network stack is involved.
- Certificate quality depends on the accuracy of the completion-time estimator.
- The pilot shows no mobility/dead-device failures, so failure mechanisms must be deliberately injected.
- Mobility traces are synthetic; real vehicular/UAV traces may change the infeasibility boundary.
- Admission control may reject tasks that a more informed policy could have served; precision/recall reporting is essential.

## 9. Reproduction

```powershell
$env:PYTHONPATH = "_deps_pureedgesim_python/src"
python 03_feasibility_certified_fault_tolerant_edge/scenario/run_scenario.py
```

Outputs: `scenario/output/<timestamp>/` (CSV, log, `run.json`, settings snapshot) and `scenario/output/feasibility_summary.json`.
