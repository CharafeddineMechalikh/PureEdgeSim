# Simulator capability matrix (source-verified, Phase 1)

Status key: **yes** = implemented and experimentally exercised in this artifact;
**native** = supported by the platform's own API (source location given), not
yet exercised here; **custom** = implemented by us because the platform does not
provide it; **unavailable** = no native mechanism and no adapter implemented.

This matrix replaces the earlier API-only discussion. Every `native` /
`unavailable` entry cites a file in the vendored source at the commit recorded
in `REPRO.md`. Entries are about *capability*, not about quality of modelling.

| Capability | PureEdgeSim (Python 6.1.0) | PureEdgeSim (Java 6.1) | iFogSim2 (`5f68d39`) | EdgeCloudSim (`83969dd`) | YAFS 3.1 (`98425c1`) |
|---|---|---|---|---|---|
| Execution engine | hybrid event loop + global periodic orchestrator | same framework, different port | CloudSim-based DES (`src/org/fog/cloudsim/`) | CloudSim-based DES (`src/edu/boun/edgecloudsim/core/SimManager.java`) | SimPy DES (`src/yafs/core.py:1148`) |
| Mist/peer devices | yes (`edge_devices.xml`, mist tier) | yes (same framework) | native (`MicroserviceFogDevice.java`, `FogDevice.java`) | native (`edge_client/MobileDevice`) | custom (one DES module per device core; `PORTING_NOTES.md`) |
| Edge data centres | yes (3 DCs) | yes | native (`FogDevice.java`) | native (`edge_server/`) | native topology node |
| Cloud | yes (1 DC) | yes | native (`app/cloud/` examples) | native (`cloud_server/`) | native topology node |
| Application dependencies | custom (this revision: `SPEC.md` §3) | custom (same model) | native (`AppEdge.java`, `AppDAG`-style edge lists) | partial (task-level; no DAG scheduler in core) | partial (`Application` modules, no DAG scheduler) |
| Mobility | yes (8 models) | yes (same framework) | native (`MobilityController.java`, `RandomMobilityGenerator.java`) | native (`mobility/`) | unavailable (static topology; `PORTING_NOTES.md`) |
| Periodic global orchestration | yes (`update_interval`, default 1 s) | yes (same parameter) | unavailable — placement is one-shot at deployment (`ModulePlacement*.java`) | unavailable — `getDeviceToOffload(Task)` at generation (`BasicEdgeOrchestrator.java:89,115`) | custom — driver adds a SimPy tick (`yafs_impl/run_yafs.py`) |
| Event-triggered placement | unavailable in core | unavailable in core | native (module mapping at deploy, event-driven task forwarding) | native (per task, synchronous) | native (`Selection.get_path`, `yafs/selection.py`) |
| Network modelling | yes (WAN/MAN/Wi-Fi/Ethernet) | yes | native (`NetworkTopology.java`) | native (`network/`) | native (`yafs/core.py:292`) |
| Energy modelling | yes (per node + per link) | yes | native (`PowerModel*.java`, `FogLinearPowerModel.java`) | native (`power/`, `edge_server/`) | unavailable (no energy module) |
| Trace-driven workload | yes (4 arms in P1) | yes (same generators) | custom (adapter consumes `traces/*.csv`) | custom (adapter consumes `traces/*.csv`) | custom (driver consumes `traces/*.csv`) |
| Common reference algorithms | 5 implemented + 2 native | same | custom (routing policies to be added) | custom (orchestrator subclass) | custom (`Selection` subclass) |

## Capability-based claims this matrix supports

* **O1 (calibration):** PureEdgeSim Python and Java, YAFS (custom tick),
  and — pending Phase 3 — iFogSim2 and EdgeCloudSim via event-driven
  controls/explicit tick injection.
* **O2 (cross-simulator):** supported only after the common scenario has been
  executed on all four engines. An API inspection (EdgeCloudSim) or a
  related-work mention (iFogSim2) is **not** experimental validation.
* **O3 (application/infrastructure):** supported by the dependency-aware arm
  in PureEdgeSim and, where the native platform permits DAGs, by the iFogSim2
  adapter; otherwise recorded as a documented limitation.

## Verification status

| Platform | Source verified | Reference scenario executed | Common trace consumed | Phase-3 equivalence passed |
|---|---|---|---|---|
| PureEdgeSim Python | yes | yes (3,215 runs) | no (native generator) | pending |
| PureEdgeSim Java | yes | yes (parity grids) | no | pending |
| YAFS 3.1 | yes | yes (replication grid) | yes | pending |
| EdgeCloudSim | yes | tutorial only | no | pending |
| iFogSim2 | yes | no | no | pending |

No cross-simulator conclusion is drawn from a row that is not `yes` in the
last column.
