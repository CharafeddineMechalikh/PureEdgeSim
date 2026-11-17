# Cross-simulator adapters (P1-CBS v1.0)

Each adapter consumes `crosssim/traces/trace_s*.csv`, produced by
`crosssim/common_trace.py`, and writes one row per job to
`crosssim/results/common_<platform>/jobs_df1p0.csv` using the schema in
`crosssim/definitions.md` plus `platform`, `scenario`, `arm`, `workflow_id`.

`crosssim/aggregate_common.py` reads those files and emits
`tables/common_level_b.tex` and `common_level_c.tex`.

## YAFS 3.1 — implemented

```powershell
python crosssim/adapters/yafs/run_common.py --grid --seeds 5 `
    --out crosssim/results/common_yafs
python crosssim/aggregate_common.py
```

* 3 architectures × 4 temporal settings × 5 seeds = 60 runs.
* The `tick` is injected as a SimPy process on YAFS's own environment; this is
  the closest equivalent of PureEdgeSim's `update_interval` and is documented as
  a **custom** capability in `CAPABILITIES.md`.
* The shared trace is aggregated per workflow into one job for the
  `independent` arm. The `workflow` arm (each service as a separate YAFS
  message along the application DAG) is the next adapter extension.

## EdgeCloudSim — not implemented (next)

Evidence already gathered:

* vendored at commit `83969dd`; shipped tutorial runs (25,290 / 25,367 tasks).
* `BasicEdgeOrchestrator.getDeviceToOffload(Task)` and `getVmToOffload(Task,int)`
  are synchronous per-task functions; there is **no global periodic loop**, so
  Level B (temporal calibration) cannot be run natively. This is a capability
  finding, not a reason to omit the platform.
* `crosssim/ecs/src/` is empty; the previously referenced `crosssim.CrossSimApp`
  class does not exist in the repository. A custom scenario must be written.

Required work:

1. Write `CustomOrchestrator` implementing the reference placement policy and
   recording release / decision / dispatch / execution timestamps.
2. Write the config set (edge devices, data centres, cloud, applications,
   network) from `crosssim/scenario.json` and the shared trace.
3. Run Level A (scenario equivalence) and Level C (architecture / algorithm),
   and report Level B as `unavailable` with the source evidence above.

## iFogSim2 — build verified, adapter not written

Evidence already gathered:

* vendored at commit `5f68d39`; 445 Java sources; prebuilt classes in `build/`;
  dependency jars in `jars/` (including `jars/commons-math3-3.5/commons-math3-3.5.jar`).
* Compiling a shipped example succeeds (command below).
* Native support confirmed by source: `AppEdge.java` (DAG edges),
  `MicroserviceFogDevice.java`, `MobilityController.java`, `NetworkTopology.java`,
  `PowerModel*.java`, `ModulePlacement*.java` (one-shot, **not** periodic).
* `topologies/routerTopology` shows the JSON topology schema (nodes with
  `mips`, `ram`, `upBw`, `downBw`, `level`; links with `latency`).

**Partial implementation done in this session:** `adapters/ifogsim2/` builds and
replays exact trace arrivals: `TraceDistribution` reads `trace_s*.csv` and
aggregates one arrival per workflow, `LoggingSensor` removes iFogSim2's default
50 s transmission delay, and the application graph (3 tuple types, one `svc`
module, 3 actuator types) is built from the specification. The adapter emits
tuples at the correct shared-trace times.

**Remaining blocker:** tuples are not being forwarded from the `svc` module to
the actuators in this minimal topology (no `cbs-act` events), so end-to-end
delay and success cannot yet be collected. This is a placement/mapping issue in
iFogSim2's `ModulePlacementEdgewards` with a hand-built topology. No iFogSim2
result is reported until it is resolved.

```powershell
$cp = "build;jars\commons-math3-3.5\commons-math3-3.5.jar;" +
      ((Get-ChildItem jars -Filter *.jar | ForEach-Object { "jars\$($_.Name)" }) -join ";")
javac -cp $cp -d build src\org\fog\test\perfeval\MicroservicesAppSample1.java
```

* Native support confirmed by source: `AppEdge.java` (DAG edges),
  `MicroserviceFogDevice.java`, `MobilityController.java`, `NetworkTopology.java`,
  `PowerModel*.java`, `ModulePlacement*.java` (one-shot, **not** periodic).

Required work:

1. Write an adapter example that builds the 5-service application graph from
   `application_graph.json`, the 40-device / 3-edge / 1-cloud topology, and
   releases the shared trace arrivals.
2. Implement a reference `ModulePlacement` / routing policy.
3. Run Level A and Level C; report Level B as `unavailable` (one-shot placement).

## Non-fabrication rule

No result is reported for a platform that has not actually executed the common
scenario. Blank cells in `common_level_b.tex` are blanks, not zeros.
