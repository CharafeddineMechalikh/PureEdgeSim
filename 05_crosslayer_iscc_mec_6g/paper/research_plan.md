# Research Plan — Cross-Layer ISCC-MEC Co-Design for 6G Edge Networks: Unifying Semantic Communication, Reconfigurable Antennas, and Service-Level Resource Control

- **Folder:** `05_crosslayer_iscc_mec_6g/`
- **Paper draft:** `paper/main.tex`
- **Scenario:** `scenario/run_scenario.py` + `scenario/settings/`
- **Status:** pilot scenario implemented and executed (PureEdgeSim Python, 30 devices, 5 simulated minutes, seed 42)
- **One-line goal:** evaluate 6G ISCC services as dependency-aware pipelines, so sensing, communication, and computation decisions are optimized together rather than in isolation.

---

## 1. Objectives

| ID | Objective | Why it matters |
|---|---|---|
| O1 | Formalize an **ISCC service as a DAG** with sensing, communication/encoding, compute/inference, and actuation stages, dependencies, and deadlines. | Layer-isolated optimization misses cross-layer coupling. |
| O2 | Design a **dependency-aware, cross-layer placement/scheduling policy** (stage co-location vs split, data-aware routing). | The pipeline structure, not just the tier, drives performance. |
| O3 | Quantify **cross-layer gains** vs layer-agnostic placement. | The central empirical claim. |
| O4 | Analyze **sensitivity** to bandwidth, semantic compression ratio, pipeline depth, and PHY/antenna proxies. | Determines when co-design matters. |
| O5 | Provide a reproducible **service-graph scenario** with stage-level and request-level metrics. | Enables follow-up work on ISCC-MEC. |

---

## 2. Scenario mapping (what is simulated)

| Element | Implementation |
|---|---|
| Devices | 30 devices, 5 simulated minutes, seed 42 |
| ISCC pipeline | `sense` (800 MI, 1,200 KB request, 400 KB result) → `encode_comm` (1,200 MI) → `edge_infer` (3,500 MI) → `actuate` (500 MI), 0.8 s end-to-end deadline, 8 requests/min |
| Background | Telemetry (4/min, 4,000 MI) |
| Matrix | `MIST_AND_EDGE`, `EDGE_AND_CLOUD`, `ALL`; `TRADE_OFF` |
| Pilot result | MIST_AND_EDGE: 2,440/2,440 tasks, 760/760 requests, 0.1915 s e2e, 0 dependency failures. EDGE_AND_CLOUD: 2,396/2,440, 749/760, 33 dependency failures. ALL: 2,416/2,440, 754/760, 18 dependency failures |

**Interpretation:** dependency-aware local/mist placement gives perfect request success here, while cloud/edge splitting introduces dependency failures — a concrete cross-layer effect worth optimizing.

---

## 3. Expected results

| ID | Hypothesis / expected result | Evidence already available |
|---|---|---|
| E1 | Dependency-aware placement improves request success or makespan by **≥10%** vs layer-agnostic placement. | Pilot suggests large differences; controlled comparison pending. |
| E2 | Co-locating dependent stages reduces transfer delay; excessive splitting raises dependency failures. | EDGE_AND_CLOUD: 33 dependency failures vs 0 in MIST_AND_EDGE. |
| E3 | Semantic compression reduces e2e delay and bytes, with an accuracy/fidelity trade-off. | Requires compression parameter. |
| E4 | Gains grow with **pipeline depth** and payload size. | Requires depth sweep. |
| E5 | Cross-layer scheduling reduces **energy per successful request** by 5–20%. | Requires energy comparison. |

---

## 4. Implementation checklist

### Done
- [x] Four-stage ISCC service graph with dependencies.
- [x] Dependency-failure and request-level metrics.
- [x] Three-architecture pilot and aggregated JSON.
- [x] Project README.

### To do — model and algorithm
- [ ] `ISCCAwareOrchestrator`: stage placement, data-aware routing, optional co-location constraints.
- [ ] Semantic compression ratio parameter (payload multiplier + fidelity proxy).
- [ ] PHY proxy knobs: bandwidth, latency, antenna-gain multiplier.
- [ ] Pipeline-depth variants (2, 4, 6 stages).
- [ ] Stage-level accuracy/quality proxy.

### To do — experiments
- [ ] Compression sweep (e.g., 1.0, 0.5, 0.2 payload ratio) with fidelity proxy.
- [ ] Bandwidth/latency sensitivity grid.
- [ ] Pipeline-depth sweep.
- [ ] Multi-seed runs with confidence intervals.
- [ ] Comparison against layer-agnostic baseline.

### To do — paper
- [ ] ISCC DAG formulation with joint constraints.
- [ ] Cross-layer scheduling algorithm and complexity.
- [ ] Stage-level delay/energy decomposition.
- [ ] Threats to validity: PHY/semantic abstraction vs real codecs.

---

## 5. Metrics to use

| Metric | Definition / formula | Notes |
|---|---|---|
| Request success ratio | `successful requests / generated requests` | Primary service metric |
| E2E request makespan | mean and p95 end-to-end delay | Primary latency metric |
| Task success ratio | `successful tasks / generated tasks` | Stage-level view |
| Dependency failure rate | `failed(dependency) / generated tasks` | Pipeline robustness |
| Stage delay breakdown | delay per stage (sense/encode/infer/actuate) | Where time goes |
| Bytes per request | transferred bytes / request (amplification/reduction) | Communication cost |
| Cross-layer gain | `(baseline − ISCC-aware) / baseline` for makespan or success | Treatment effect |
| Energy per successful request | `energy / successful requests` | Efficiency |
| Semantic fidelity proxy | accuracy/quality vs compression ratio | If implemented |
| Tier utilization | resource usage per tier | Placement effect |

**Primary outcomes:** request success, e2e makespan, dependency failures, energy per request.
**Secondary outcomes:** stage delay breakdown, bytes per request, tier utilization.

---

## 6. Acceptance criteria

| Criterion | Target | Verification |
|---|---|---|
| Service-graph execution | Scenario runs and reports request-level metrics | Rerun |
| ISCC-aware gain | ≥10% improvement in success or makespan vs layer-agnostic | ≥10 seeds, CI |
| Dependency failures | ≥50% reduction vs layer-agnostic | Controlled comparison |
| Sensitivity coverage | ≥3 compression and ≥3 bandwidth settings | Grid results |
| Stage accounting | Delay/energy decomposed per stage | Instrumented metrics |
| Assumption transparency | PHY/semantic proxies documented; no overclaim | Paper review |
| Artifact | Graph, script, seeds, outputs, README | Bundle check |

---

## 7. Potential figures

| Fig. | Type | Content / axes | Message |
|---|---|---|---|
| F1 | DAG diagram | sense → encode/comm → edge_infer → actuate with data sizes | Pipeline abstraction |
| F2 | Stacked bars | Stage-level delay per architecture | Where time is spent |
| F3 | CDF / line plot | Request makespan CDF; success vs pipeline depth | Pipeline behavior |
| F4 | Dual-axis line plot | Cross-layer gain vs compression ratio; fidelity on second axis | Compression trade-off |
| F5 | Bar plot | Dependency failure rate per policy | Robustness |
| F6 | Bar plot | Energy per successful request | Efficiency |
| F7 | Heatmap | Makespan over bandwidth × compression | Sensitivity map |
| T1 | Table | Stage parameters and deadlines | Reproducibility |
| T2 | Table | Policy comparison with CIs | Main result |

---

## 8. Risks and threats to validity

- Physical layer (beamforming, RIS/fluid antennas, channel models) is **abstracted**; antenna gains are proxy knobs at best.
- Semantic communication is represented by payload scaling, not a real semantic codec or distortion metric.
- The accuracy/fidelity proxy must be defined carefully; otherwise compression gains are meaningless.
- The DAG is small (four stages); deeper pipelines may expose different bottlenecks.
- PureEdgeSim's service-graph semantics (shared application placement, dependency failures) constrain how policies can be expressed.

## 9. Reproduction

```powershell
$env:PYTHONPATH = "_deps_pureedgesim_python/src"
python 05_crosslayer_iscc_mec_6g/scenario/run_scenario.py
```

Outputs: `scenario/output/<timestamp>/` (CSV, log, `run.json`, settings snapshot) and `scenario/output/feasibility_summary.json`.
