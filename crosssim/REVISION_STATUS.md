# P1 major-revision status (Phase 1 complete, Phase 2 gated)

The reviewer's supervisor requires the application and infrastructure model to
be **agreed before** large cross-simulator experiments begin. This file records
what is now available for that agreement and what remains.

## Phase 1 — experimental model (completed)

| Reviewer requirement | Artifact | Status |
|---|---|---|
| Application graph with services, dependencies, sizes, MI, deadlines, rates, instances, placement constraints | `crosssim/application_graph.json`, `crosssim/SPEC.md` §3, diagram `diagrams/application_graph.png` | **done** |
| Verify dependencies are actually simulated | `crosssim/common_trace.py` emits `predecessors` per service and asserts per-class MI sums; two workload arms (`independent`, `workflow`) defined in `SPEC.md` §3 | **specified**; execution pending |
| Infrastructure topology figure | `crosssim/diagrams/infrastructure.png` | **done** |
| Numeric infrastructure specs | `crosssim/scenario.json` (fleet, edge, cloud, links) + `SPEC.md` §5 | **done** |
| Three deployment configurations illustrated | `crosssim/diagrams/deployments.png` (+ optional cloud-only reference) | **done** |
| Simulator capability matrix from implementation | `crosssim/CAPABILITIES.md`, `.csv` pending | **done (source-verified)** |
| Simulator-independent benchmark specification | `crosssim/SPEC.md` v1.0 | **done** |
| Common workload trace generated outside the simulators | `crosssim/common_trace.py`, `crosssim/traces/trace_s1..s5.csv` | **done** |
| Cross-simulator framework figure | `crosssim/diagrams/framework.png` | **done** |
| Calibration workflow figure | `crosssim/diagrams/calibration.png` | **done** |
| Orchestrator scope figure | `crosssim/diagrams/orchestrator.png` | **done** |
| Research questions RQ1–RQ5 | `crosssim/SPEC.md` §2 | **done** |
| Experimental unit and statistics plan | `crosssim/SPEC.md` §8 | **done** |
| Common metric definitions | `crosssim/definitions.md` + `SPEC.md` §7 | **done** |

## Phase 2 — simulator adapters (in progress)

| Platform | Vendored commit | Adapter | Status |
|---|---|---|---|
| PureEdgeSim Python | 6.1.0 port | reference implementation | exists; must be switched to the common trace and workflow arm |
| PureEdgeSim Java | 6.1 | cross-port parity | exists; same adaptation needed |
| YAFS 3.1 | `98425c1` | `adapters/yafs/run_common.py` | **done**: consumes the shared trace; 3 architectures × 4 temporal settings × 5 seeds = 60 runs complete |
| EdgeCloudSim | `83969dd` | `ecs/` (tutorial smoke only) | **must be implemented**; no common-scenario run yet |
| iFogSim2 | `5f68d39` | none yet | **build verified** (`javac` against `build/` + `jars/`); adapter not written |

### YAFS Level-B result (common trace, 5 seeds)

| Architecture | 1 s | 200 ms | 50 ms | event-driven |
|---|---|---|---|---|
| ALL | 0.868 | 0.999 | 0.999 | 0.999 |
| EDGE_AND_CLOUD | 0.392 | 0.859 | 1.000 | 1.000 |
| EDGE_ONLY | 0.392 | 0.859 | 1.000 | 1.000 |

The architecture effect at 1 s is **+47.6 pp** and vanishes at 50 ms, reproducing
the temporal artefact under the *shared* trace (not a simulator-native
generator). Tables: `crosssim/tables/common_level_b.tex`,
`common_level_c.tex`; data: `crosssim/results/common_summary.csv`.


## Phase 3 — equivalence checks and core experiments (not started)

* Same arrivals, task attributes and DAG edges verified in each adapter.
* Level B temporal calibration on all four engines.
* Level C architecture/algorithm/infrastructure comparisons.
* Simulator × interval, × architecture, × workload, × infrastructure interactions.
* Cross-port audit kept explicitly separate from cross-simulator validation.

## Phase 4 — manuscript restructure (not started)

* Title/abstract rewritten only after results exist.
* ~9 grouped figures, 10–12 main tables; detail to supplementary material.
* Discussion organised around temporal artefacts, application characteristics,
  infrastructure/deployment, simulator differences, practical recommendations.
* Conclusion answering RQ1–RQ5 with explicit generalisation boundaries.

## Honest statement

The current manuscript's results remain a PureEdgeSim-centred study plus a YAFS
replication and an EdgeCloudSim API finding. Until Phases 2–3 are complete, the
paper must **not** claim a four-simulator benchmark, and the title/abstract must
not imply one. No fabricating of simulator results will be done to close this
gap.
