# Research Plan — From Simulation to Deployment: A Reproducible Benchmark and Testbed for UAV- and SAGIN-Integrated Edge Computing

- **Folder:** `01_sim2deploy_sagin_uav_benchmark/`
- **Paper draft:** `paper/main.tex`
- **Scenario:** `scenario/run_scenario.py` + `scenario/settings/`
- **Status:** pilot scenario implemented and executed (PureEdgeSim Python, 40 devices, 5 simulated minutes, seed 42)
- **One-line goal:** turn UAV/SAGIN-MEC evaluation into a reproducible artifact that others can rerun, audit, and extend toward a testbed.

---

## 1. Objectives

| ID | Objective | Why it matters |
|---|---|---|
| O1 | Specify a **benchmark contract**: tier/entity model, mobility, workload mix, link and energy assumptions, scenario matrix, reporting schema, provenance. | 2026 UAV/SAGIN papers use bespoke simulators and assumptions, so results are not comparable. |
| O2 | Implement the benchmark on the **latest PureEdgeSim** (Python primary, Java parity) with one-command reproduction. | Open, maintained engine + identical settings format lowers adoption cost. |
| O3 | Quantify **sensitivity** of MEC outcomes to tier set, placement policy, mobility, and scale. | Separates algorithm gains from scenario artifacts. |
| O4 | Package a **reusable artifact**: settings, scripts, seeds, outputs, data dictionary, provenance. | Reproducibility is the contribution, not an afterthought. |
| O5 | Provide **simulation-to-testbed guidance**: which parameters must be calibrated and which metrics must be measured on hardware. | Bridges the gap between simulation claims and deployment feasibility. |

---

## 2. Scenario mapping (what is simulated)

| Element | Implementation |
|---|---|
| Area | 500 m × 500 m; device range 80 m; edge coverage 350 m |
| Devices (40) | 40% UAV (30k MIPS, 60 Wh, 12 m/s), 40% mobile IoT (12k MIPS, 20 Wh, 1.5 m/s), 20% fixed task-only sensors |
| Edge tier | 3 ground MEC data centers (16 cores, 80k MIPS) with MAN links |
| Cloud tier | remote 400-core / 100k MIPS data center over 50 Mbps, 120 ms WAN (satellite/backhaul proxy) |
| Workloads | UAV sensing/control (0.2 s, 2,000 MI), UAV video analytics (0.8 s, 12,000 MI), mission aggregation (60 s, 20,000 MI) |
| Network | realistic contention model, shared WAN |
| Matrix | `EDGE_ONLY`, `EDGE_AND_CLOUD`, `ALL` × `TRADE_OFF`, `ROUND_ROBIN`; 5 min; seed 42 |
| Pilot result | EDGE_ONLY/EDGE_AND_CLOUD 240/1990 successes; ALL 1,227–1,411/1990; WAN 0 s vs 9–67 s; energy ≈30.2–30.4 Wh |

**Interpretation:** the pilot already shows a ~5.9× success spread across architectures, confirming that tier and policy choices dominate outcomes under mobility and deadlines.

---

## 3. Expected results

| ID | Hypothesis / expected result | Evidence already available |
|---|---|---|
| E1 | Success ratio differs by **≥2×** across architectures under mobility and tight deadlines. | Pilot: 240 vs 1,411 successes (≈5.9×). |
| E2 | Cloud/backhaul helps some delay-tolerant tasks but adds WAN cost and can hurt deadline-tight tasks. | WAN usage is 0 when cloud is disabled and 9–67 s when enabled; delay failures remain high. |
| E3 | Same-seed runs are **exactly reproducible**; Java and Python agree on shared columns. | PureEdgeSim Python writes `run.json` + settings snapshot; parity not yet tested. |
| E4 | Runtime scales roughly linearly with devices × simulated duration; a 5-min, 40-device sweep finishes in minutes. | Pilot sweeps completed quickly on a laptop. |
| E5 | A third party can reproduce headline numbers **without editing code**. | Requires artifact bundle + data dictionary (pending). |

---

## 4. Implementation checklist

### Done
- [x] Five settings files for the UAV/SAGIN proxy (parameters, devices, datacenters, cloud, applications).
- [x] Three-tier topology (mist-capable devices, ground MEC, remote cloud).
- [x] Three workload classes with different deadlines and compute demand.
- [x] Seeded sweep script and aggregated `feasibility_summary.json`.
- [x] Pilot execution and project README.

### To do — benchmark and experiments
- [ ] Java equivalent settings + parity run against the Python results.
- [ ] ≥10 seeds per configuration with 95% confidence intervals.
- [ ] Device-count sweep (20, 40, 60, 80) and duration sweep.
- [ ] Mobility variants: random-waypoint, Gauss–Markov, fixed waypoints, group mobility.
- [ ] Trace-replay workload option (recorded arrivals instead of synthetic rates).
- [ ] Link/energy sensitivity (WAN latency, WiFi rate, battery capacity).

### To do — artifact and paper
- [ ] Data dictionary for the 56 PureEdgeSim columns used in the paper.
- [ ] Artifact bundle (zip) with version tag and DOI (e.g., Zenodo).
- [ ] Paper sections: benchmark specification table, methodology, results, threats to validity.
- [ ] Reproduction script that regenerates every figure from raw CSV.

---

## 5. Metrics to use

| Metric | Definition / formula | Source |
|---|---|---|
| Task success ratio | `successes / generated` | CSV columns 7–8 |
| Deadline failure ratio | `failed(delay) / generated` | column 10 |
| Mobility failure ratio | `failed(mobility) / generated` | column 12 |
| Tier share | `successes(tier) / successes` for cloud/edge/mist | columns 16/18/20 |
| Average execution delay | mean task delay (s) | column 4 |
| p95 execution delay | 95th percentile (requires per-task logging or custom metric) | custom |
| Energy per successful task | `energy(Wh) / successes` | column 32 |
| WAN / network usage | seconds and MBytes | columns 21–24 |
| Reproducibility | CSV hash equality across reruns; max relative diff Java vs Python | artifact check |
| Wall-clock runtime | seconds per scenario / total sweep | harness timing |

**Primary outcomes:** success ratio, deadline-failure ratio, energy per successful task, WAN usage.
**Secondary outcomes:** tier share, p95 delay, runtime.

---

## 6. Acceptance criteria

| Criterion | Target | Verification |
|---|---|---|
| One-command reproduction | Clean checkout + one command, <10 min on a laptop | README test |
| Seed reproducibility | Identical metric rows for identical seed | CSV hash equality |
| Java–Python parity | Shared columns within 1e-6 relative (or exact) | parity script |
| Experimental breadth | ≥3 architectures × ≥2 algorithms × ≥2 scales | results table |
| Statistical support | ≥10 seeds; 95% CIs; ≥1 significant architecture difference | CI plot / test |
| Artifact completeness | Settings, script, seeds, outputs, data dictionary, provenance | bundle checklist |
| Claim traceability | Every paper claim maps to an artifact/figure | internal review |
| Threats to validity | Explicit section on abstraction and calibration limits | paper review |

---

## 7. Potential figures

| Fig. | Type | Content / axes | Message |
|---|---|---|---|
| F1 | Architecture diagram | UAV/IoT/sensor → ground MEC ↔ MAN ↔ cloud/backhaul | Benchmark scope and tier mapping |
| F2 | Line plot + CI | Success ratio vs device count, one line per architecture | Scalability and tier effect |
| F3 | Stacked bars | Failure causes (delay/mobility/dead/resource) per architecture | Where losses come from |
| F4 | Bar plot | Energy per successful task per architecture | Efficiency comparison |
| F5 | Bar or CDF | WAN/network usage per architecture | Backhaul cost of cloud offloading |
| F6 | Scatter | Java vs Python metric values with identity line | Cross-implementation reproducibility |
| F7 | Line plot | Wall-clock runtime vs devices × duration | Practical benchmark cost |
| T1 | Table | Benchmark parameters and workloads | Reproducibility contract |
| T2 | Table | Results with CIs | Main findings |

---

## 8. Risks and threats to validity

- Satellite constellation, orbital dynamics, and 5G/NTN PHY are **not modeled**; the cloud tier is only a latency/capacity proxy.
- Mobility is synthetic, not trace-driven; UAV energy is modeled at the device level, not aerodynamically.
- PureEdgeSim reports aggregate metrics; p95 and per-task distributions require custom instrumentation.
- Simulator-specific scheduling details can bias cross-simulator comparisons; Java–Python parity is a partial control.

## 9. Reproduction

```powershell
$env:PYTHONPATH = "_deps_pureedgesim_python/src"
python 01_sim2deploy_sagin_uav_benchmark/scenario/run_scenario.py
```

Outputs: `scenario/output/<timestamp>/Sequential_simulation.csv`, `Sequential_simulation.txt`, `run.json`, `settings/`, plus `scenario/output/feasibility_summary.json`.
