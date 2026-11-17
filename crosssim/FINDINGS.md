# Does the temporal-resolution artefact generalise beyond PureEdgeSim?

Short answer: **yes, on YAFS 3.1, quantitatively and with the same mechanism.**
The platform that cannot test it is EdgeCloudSim, for a reason that is itself
part of the answer: it has no periodic global orchestration loop.

All numbers below are read from `results/replication.csv`, which is produced by
`aggregate.py` from the per-task CSVs in `results/yafs/`. Three seeds per cell,
5,850 task records per cell (the scenario's 15 devices over 300 simulated
seconds).

## The finding under test

PureEdgeSim reports an apparent architectural benefit at its default 1 s control
interval: `ALL` (mist+edge+cloud) succeeds on 0.660 of requests and `EDGE_ONLY`
on 0.121, an effect of +53.9 points. The claim is that this is the temporal
resolution of the placement loop: an offloaded request waits a whole tick before
execution starts, and the workload contains 0.2 s and 0.8 s deadlines. Refining
the interval to 50 ms collapses the effect to -0.4 points; scaling all deadlines
by 16 collapses it to +1.1 points.

## YAFS 3.1 (commit 98425c10109a2098cd07eb2a3d6b48c2110d4baa)

| Temporal setting | All tiers | Edge only | Effect (pp) | Decision latency (s) |
|---|---|---|---|---|
| event-driven (no tick) | 1.000 | 1.000 | **0.0** | 0.040 |
| 50 ms | 1.000 | 1.000 | **0.0** | 0.090 |
| 200 ms | 1.000 | 0.469 | +53.1 | 0.240 |
| 1 s | 0.698 | 0.117 | **+58.2** | 1.040 |
| 1 s, deadlines ×16 | 1.000 | 1.000 | **0.0** | 1.040 |

The shape is the same as in PureEdgeSim, including the intermediate point: at a
200 ms tick, equal to the tightest deadline, the effect is still +53.1 points,
because a request that waits one full tick misses by its entire budget. The
critical value is not "interval equals deadline" but "interval below deadline".
Scaling the deadlines by 16 while keeping the 1 s tick removes the effect
entirely, exactly as in the reference platform (+1.1 points there, 0.0 here).

The magnitude is also close: +58.2 points in YAFS against +53.9 in PureEdgeSim.
That agreement is not calibration — the two models differ in how a task is
charged to a core, in the network stack and in the workload generator — but the
two numbers are the same phenomenon measured twice.

**Mechanism, not correlation.** `decision_latency_s` is the release-to-execution
start time averaged over every request. It is 1.040 s at a 1 s tick, 0.240 s at
200 ms, 0.090 s at 50 ms and 0.040 s when the placement is made at release.
The success ratio moves with this quantity and with nothing else that was varied.

## EdgeCloudSim (commit 83969dd15ad1b09bd6ca1b5d0bc84ae4670cd844)

EdgeCloudSim builds and runs in this environment: its shipped tutorial
completed 25,290 of 25,367 augmented-reality tasks in 30 simulated minutes
(`ecs/out_smoke_log.txt`). It could not be used for the replication, and the
reason is semantic rather than practical:

* placement is a per-task function, `getDeviceToOffload(Task)` and
  `getVmToOffload(Task, int)`
  (`src/edu/boun/edgecloudsim/edge_orchestrator/BasicEdgeOrchestrator.java:89,115`),
  called when the task is generated;
* there is no global control-loop period to vary, so there is no parameter that
  could produce the artefact.

That is a useful negative result for the paper: the artefact is not a property
of "edge simulators" in general but of simulators that schedule *globally and
periodically*. EdgeCloudSim's event-driven placement cannot exhibit it, and a
study that used EdgeCloudSim would never have seen it. We did not port the
three-class scenario onto EdgeCloudSim, so we make no claim about its success
ratios.

## What this does and does not establish

**Establishes.** The temporal-resolution artefact reproduces on an independent
discrete-event platform (SimPy-based, different resource model, different
network model, different placement API) with the same sign, the same monotone
shape, the same intermediate behaviour at interval = deadline, and the same
disappearance under deadline scaling.

**Does not establish.** That the absolute success ratios transfer. YAFS serves
1.000 at 50 ms where PureEdgeSim serves 0.987, and the two differ in queueing
and in how many replicas exist per node. The claim is about the *effect of the
clock*, not about the level of service.

**Does not establish.** That every simulator has this failure mode. EdgeCloudSim
does not have the parameter at all.
