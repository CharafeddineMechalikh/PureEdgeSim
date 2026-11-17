# Defect report: iFogSim2 common-benchmark adapter

## Affected artefact

| Field | Value |
|---|---|
| Simulator | iFogSim2 ("The New Version") |
| Upstream | `https://github.com/Cloudslab/iFogSim2` |
| Vendored commit | **`5f68d3947e450d8d2b4af42670be819206be68c9`** |
| Commit date | 2025-10-03 21:55:02 +1000 |
| Commit subject | `Remove IDE files from repo and ignore` |
| Java | Java 23, javac 23 (Windows) |
| Adapter location | `crosssim/adapters/ifogsim2/` |
| Reported in paper | Section "Common-scenario cross-simulator validation" and Threats to Validity |

The repository has no semantic version tag on this commit; the commit hash above is
the authoritative version identifier.

## What works

The adapter compiles and replays the shared trace exactly:

* `TraceDistribution.java` reads `crosssim/traces/trace_s1.csv`, aggregates one
  arrival per `workflow_id` and returns the gaps between absolute arrival times.
  For seed 1 it yields 1,040 / 729 / 235 arrivals for C1 / C2 / C3.
* `LoggingSensor.java` overrides `startEntity()` to remove iFogSim2's default
  50 s `Config.TRANSMISSION_START_DELAY`; tuples are emitted at the shared-trace
  instants (observed: C2 at 0.028812 s, C1 at 0.617053 s, etc.).
* The application graph is built from `application_graph.json`: three sensor
  tuple types, one `svc` module, three actuators, one loop per class.

## Defect

**Result tuples never reach the actuators; end-to-end delays and success cannot
be collected.**

Observed symptoms:

1. `LoggingSensor.transmit()` is called at every trace arrival (verified with a
   counter printed at the first three emissions per class).
2. `LoggingActuator.processEvent()` receives no `FogEvents.TUPLE_ARRIVAL`
   events: a debug line at the top of that method produced no output in a full
   300 s run.
3. iFogSim2's own summary prints `APPLICATION LOOP DELAYS` as `null` for all
   three loops:
   ```
   [C1, svc, C1_ACT] ---> null
   [C2, svc, C2_ACT] ---> null
   [C3, svc, C3_ACT] ---> null
   ```
4. Consequently `crosssim/results/common_ifogsim2/results_s1.csv` is never
   created and no iFogSim2 row appears in `crosssim/tables/common_level_b.tex`.

## Where the failure occurs

From `src/org/fog/entities/FogDevice.java` at this commit:

* `processTupleArrival()` (lines 430–490) only executes a tuple when
  `appToModulesMap.get(appId)` contains `tuple.getDestModuleName()`. If the
  service module is not deployed on the device that receives the tuple, the
  tuple is forwarded up/down the topology; if no device holds the module it is
  eventually dropped.
* `sendTupleToActuator()` (lines 646–669) forwards a result tuple only if
  `tuple.getDestModuleName().equals(actuatorType)` for an actuator in
  `getAssociatedActuatorIds()`.

The sensors attach to the first level-1 fog device. Placement was attempted
with `ModulePlacementEdgewards` and with `ModulePlacementOnlyCloud`, including an
explicit `ModuleMapping.addModuleToDevice("svc", <gateway name>)`. None of these
made the `svc` module visible on the receiving device, so upstream tuples are
dropped and no result tuple is produced.

This is a **placement/topology integration defect in the adapter's use of the
platform at this commit**, not a claim that iFogSim2 itself is broken for its
shipped examples (the shipped `MicroservicesAppSample1` example runs to
completion). It is recorded as a defect because the common-benchmark adapter
cannot be completed at this version without deeper changes to iFogSim2's
module-placement internals.

## Reproduction

```powershell
cd "D:\Charef\doctorat\Work folder\PureEdgeSim"
$cp = "crosssim\vendor\iFogSim2\build;" +
      "crosssim\vendor\iFogSim2\jars\commons-math3-3.5\commons-math3-3.5.jar;" +
      ((Get-ChildItem crosssim\vendor\iFogSim2\jars -Filter *.jar |
        ForEach-Object { "crosssim\vendor\iFogSim2\jars\$($_.Name)" }) -join ";") +
      ";crosssim\adapters\ifogsim2\classes"
java -cp $cp crosssim.CommonMainApp `
  "$PWD\crosssim\traces\trace_s1.csv" `
  "$PWD\crosssim\adapters\ifogsim2\config\topology.json" `
  "$PWD\crosssim\results\common_ifogsim2\results_s1.csv" 300 1
```

Expected: one CSV row per workflow with end-to-end delay.
Observed: no CSV, three `null` loop delays.

## Consequence for the paper

No iFogSim2 result is reported. The manuscript states explicitly that iFogSim2
was built and source-verified but its adapter was not executed successfully,
and excludes it from Table `tab:common-level-b`. Filling that cell with an
assumed value would be fabrication.

## What would resolve it

Option A: subclass `FogDevice` and override `processTupleArrival()` so the `svc`
module is executed on the device that receives the sensor tuple, bypassing
`appToModulesMap` placement.

Option B: use iFogSim2's JSON topology and its shipped example scaffolding
(`CleanFromJson`/`MicroservicesController`) instead of the minimal hand-built
topology, then map the three workloads onto its existing modules.

Both are implementation work on this exact commit; neither changes the
reported results of PureEdgeSim, YAFS or EdgeCloudSim.
