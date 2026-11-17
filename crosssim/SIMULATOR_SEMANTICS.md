# Simulator semantics relevant to temporal fidelity

The referee's objection was that a Java and a Python implementation of the same
simulator cannot establish generalisation, because they share modelling
assumptions and execution semantics. This table records the assumptions that
matter for the finding, with the source location that establishes each entry.

| Property | PureEdgeSim 6.1 | YAFS 3.1 (`98425c1`) | EdgeCloudSim (`83969dd`) | iFogSim2 |
|---|---|---|---|---|
| Execution model | Hybrid: SimJava-style event loop plus a periodic orchestrator | Pure discrete event (SimPy) | Pure discrete event (CloudSim) | Pure discrete event (CloudSim) |
| When placement is decided | Every `update_interval` seconds, globally, for all waiting tasks | When the driver calls its `Selection.get_path`, i.e. per message; our replication adds a periodic loop to match the semantics under test | Per task, at generation: `BasicEdgeOrchestrator.getDeviceToOffload` / `getVmToOffload` | At module placement (one-shot) and at application mapping |
| Time-resolution parameter | `update_interval`, default **1 s** | none built in; our driver's `tick` (0, 0.05, 0.2, 1 s) | none: no global loop | none: placement is not periodic |
| Task model | Poisson arrivals per device class, fixed size/deadline per class | Deterministic period per device class (`60/rate`), fixed size/deadline | Poisson arrivals, application-specific | Poisson arrivals per app |
| Deadline semantics | Checked at result delivery | Checked at result delivery (`success = delay <= deadline`) | Application-defined (`Task` max delay), checked at completion | Checked at loop completion |
| Resource model | Task charged the node's aggregate MIPS for its length | One DES server per core; service time = `instructions / IPT` (`core.py:443`) | One VM per task, CPU share of the VM | One VM per task, CPU share |
| Network model | Propagation + transmission per link type | Propagation + transmission; `bytes/(BW*1e6)` (`core.py:292-296`) | WLAN/WAN propagation + bandwidth, queueing in the network layer | Propagation + bandwidth with network topology |
| Seeding | Per-run seed threaded through generator and mobility | `random.seed(seed)` (driver), SimPy deterministic given seed | `Math.random`-derived long seed; seeded per run | Seeded per run |
| Custom placement policy | Subclass the orchestrator | Subclass `Selection` (route) and/or `Placement` (deploy) | Subclass `EdgeOrchestrator` | Subclass `Placement`/`ModuleMapping` |
| Energy model | Per-node CPU energy, network energy per link type | None | WLAN/WAN + datacenter energy | Per-device and network energy |

## Why the distinction matters

The finding requires a *global, periodic* placement decision. YAFS has none by
default, which is why the replication had to introduce a control loop over the
same logical parameter; the rest of the platform — resource accounting, network
stack, scheduling — remained YAFS's own. EdgeCloudSim and iFogSim2 place each
task or module at a semantically defined instant and expose no period to vary,
so they cannot reproduce the artefact even in principle. That is why the
generalisation claim in the paper is about discrete-event simulators with a
global orchestration period, not about simulators as a class.

## YAFS's own placement API

`Selection.get_path` is documented as "It can also be seen as an orchestration
algorithm" (`vendor/YAFS/src/yafs/selection.py:1-8`); `Placement` decides which
node receives a module (`vendor/YAFS/src/yafs/placement.py`). The replication
uses both interfaces unmodified and registers its periodic loop on the SimPy
environment exposed as `sim.env` (`vendor/YAFS/src/yafs/core.py:1148-1196`).

## EdgeCloudSim's placement API

`BasicEdgeOrchestrator.getDeviceToOffload(Task)` returns a device type and
`getVmToOffload(Task, int)` a VM
(`vendor/EdgeCloudSim/src/edu/boun/edgecloudsim/edge_orchestrator/BasicEdgeOrchestrator.java:89,115`).
Both are synchronous functions of the task, called at generation. There is no
scheduler state that advances on a period.

## Methods along the wrong axis

| Method | What varies | Why it is not enough |
|---|---|---|
| Different seeds | Randomness | Same clock, same aliasing |
| Different fleet size | Capacity | Shown in P1 to change success by 0.035–0.065 |
| Java vs Python port of the same simulator | Implementation | Shared modelling assumptions and same `update_interval` |
| Different simulator with event-driven placement | Platform | Cannot exhibit the artefact (EdgeCloudSim) |
| Different simulator with a configurable global period | Platform **and** semantics | This is the replication (YAFS) |
