# Research Plan — Secure-by-Design Federated and Agentic MEC: Post-Quantum, Privacy-Preserving Coordination against Poisoning, Eavesdropping, and Inference Attacks

- **Folder:** `04_secure_federated_agentic_mec/`
- **Paper draft:** `paper/main.tex`
- **Scenario:** `scenario/run_scenario.py` + `scenario/settings/`
- **Status:** pilot scenario implemented and executed (PureEdgeSim Python, 35 devices, 5 simulated minutes, seed 42)
- **One-line goal:** treat security level as a jointly optimized design variable with placement and resources, instead of an unconditional add-on.

---

## 1. Objectives

| ID | Objective | Why it matters |
|---|---|---|
| O1 | Define a **threat model** for federated/agentic MEC: poisoning, eavesdropping/MITM, and inference/privacy leakage. | Security claims must be scoped to explicit adversaries. |
| O2 | Specify a **secure-by-design architecture**: PQC key exchange, signatures, HE/secure aggregation, authenticated coordination. | Provides the mechanisms whose cost is being optimized. |
| O3 | Formulate **joint security-level, placement, and resource optimization** under latency/energy budgets. | Strong security everywhere is often infeasible at the edge. |
| O4 | Quantify the **performance–security trade-off** and evaluate adaptive security selection. | Practitioners need operating points, not slogans. |
| O5 | Provide a reproducible **secure-overhead scenario** with parameter provenance. | Enables comparable follow-up work. |

---

## 2. Scenario mapping (what is simulated)

| Element | Implementation |
|---|---|
| Devices | 35 devices, 5 simulated minutes, seed 42 |
| Matrix | `EDGE_AND_CLOUD`, `ALL`; `TRADE_OFF` |
| Baseline workload | Federated update (10/min, 8,000 MI, 800 KB container) + agentic coordination (12/min, 5,000 MI, 600 KB) |
| Secure workload | Same applications with PQC/HE/signature/secure-aggregation overhead approximated by extra compute and bytes: 14,000 MI, 1,400 KB container, 1,100 KB request; 9,000 MI, 1,100 KB, 700 KB |
| Pilot result | Success essentially unchanged (1,472–1,478/1,490), delay 0.158→0.280 s (≈1.77×), energy 13.13→14.32 Wh (+9%), WAN usage changes with placement |

**Interpretation:** at this load, security overhead does not yet break success but nearly doubles delay and raises energy — the trade-off the paper must make explicit and optimize.

---

## 3. Expected results

| ID | Hypothesis / expected result | Evidence already available |
|---|---|---|
| E1 | Security overhead raises delay **50–100%**, energy **10–25%**, and bytes **1.5–3×** in the abstraction. | Pilot: +77% delay, +9% energy. |
| E2 | Adaptive security achieves **≥90%** of baseline performance while meeting the required protection level. | Not yet implemented. |
| E3 | Attack success is reduced by **≥50%** vs an undefended baseline. | Requires adversarial scenarios. |
| E4 | Overhead is dominated by crypto compute for small payloads and by bytes for large model updates. | Requires overhead breakdown. |
| E5 | WAN-heavy placements amplify secure overhead; edge-local aggregation is favorable. | Implied by pilot WAN/placement shifts. |

---

## 4. Implementation checklist

### Done
- [x] Baseline and secure workload profiles (`applications_baseline.xml`, `applications_secure.xml`).
- [x] Two-architecture comparison under a fixed seed.
- [x] Aggregated `feasibility_summary.json` and console reporting.
- [x] Project README.

### To do — model and algorithm
- [ ] Security-level matrix: none / signature-only / PQC / PQC+HE / secure aggregation, each with compute (MI), bytes, and latency parameters.
- [ ] Calibrate parameters from literature (Kyber, Dilithium, CSIDH, CKKS) and record sources.
- [ ] `SecurityAwareOrchestrator`: choose security level per task/context and place accordingly.
- [ ] Federated-round model with aggregation point selection.
- [ ] Optional adaptive policy based on threat level/context.

### To do — experiments
- [ ] Adversarial scenarios: poisoned-update fraction, eavesdropper presence, inference-attack proxy.
- [ ] Privacy leakage proxy (e.g., membership-inference success).
- [ ] Overhead breakdown (crypto compute vs network vs storage).
- [ ] Load sweep and model-size sweep.
- [ ] ≥10 seeds with confidence intervals.

### To do — paper
- [ ] Threat model and defender capabilities section.
- [ ] Protocol/architecture description with security levels.
- [ ] Optimization formulation and adaptive policy.
- [ ] Clear separation of **modeled** vs **assumed** security guarantees.

---

## 5. Metrics to use

| Metric | Definition / formula | Notes |
|---|---|---|
| Security overhead (delay) | `(T_secure − T_baseline) / T_baseline` | Per configuration |
| Security overhead (energy) | `(E_secure − E_baseline) / E_baseline` | Per successful task preferred |
| Byte amplification | `bytes_secure / bytes_baseline` | Network cost |
| Protected-exchange ratio | protected exchanges / total exchanges | Coverage |
| Attack success rate | poisoning impact, eavesdrop exposure, inference accuracy | Per attack model |
| Privacy leakage proxy | inference-attack success or leakage score | Requires proxy definition |
| Cost per protected update | energy and delay per protected exchange | Efficiency |
| Secure aggregation throughput | updates/second | Scalability |
| Utility per cost | utility / (delay × energy) or similar | Composite |
| Key-exchange / verification latency | seconds per handshake/signature | Crypto cost |

**Primary outcomes:** delay/energy/byte overhead, attack success reduction, protection coverage.
**Secondary outcomes:** throughput, cost per protected update, privacy leakage proxy.

---

## 6. Acceptance criteria

| Criterion | Target | Verification |
|---|---|---|
| Security levels runnable | All defined levels execute and report comparable metrics | Test matrix |
| Adaptive performance | ≥90% of baseline throughput/latency at required protection | Controlled comparison |
| Attack reduction | ≥50% lower attack success vs undefended | Adversarial runs |
| Parameter credibility | Overhead within ±25% of cited literature | Source table |
| Provenance | Every crypto parameter traceable to a citation | Artifact review |
| Statistical support | ≥10 seeds | CIs |
| Honest claims | No claim of cryptographic security from simulation alone | Paper review |

---

## 7. Potential figures

| Fig. | Type | Content / axes | Message |
|---|---|---|---|
| F1 | Architecture + threat diagram | PQC channels, signatures, secure aggregation, adversaries | Security scope |
| F2 | Grouped bars | Delay, energy, bytes vs security level | Cost of protection |
| F3 | Pareto scatter | Utility vs protection level | Operating points |
| F4 | Line plot | Attack success vs defense strength (poisoning fraction, inference) | Defense effectiveness |
| F5 | Time/load plot | Adaptive vs static security | Adaptivity benefit |
| F6 | Stacked bars | Overhead breakdown (compute/network/crypto) | Where cost comes from |
| T1 | Table | Crypto parameters with sources | Credibility |
| T2 | Table | Threat → defense mapping | Scope |

---

## 8. Risks and threats to validity

- Cryptography is **not executed**; overhead is parametric. Simulation cannot establish cryptographic security.
- Attack models are coarse proxies (poisoning fraction, inference success), not full adversarial ML.
- Federated learning convergence/accuracy is not modeled; only coordination cost is.
- Parameter calibration from literature introduces version/hardware dependence.
- Adaptive security may create a false sense of safety if threat detection is assumed perfect.

## 9. Reproduction

```powershell
$env:PYTHONPATH = "_deps_pureedgesim_python/src"
python 04_secure_federated_agentic_mec/scenario/run_scenario.py
```

Outputs: `scenario/output/baseline/`, `scenario/output/secure_overhead/`, and `scenario/output/feasibility_summary.json`.
