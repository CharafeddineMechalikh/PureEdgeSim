# Example 23: extending the simulator with custom models

## What it shows

Every default model in PureEdgeSim can be replaced without touching the
framework. This example implements no orchestrator and no new algorithm.
Instead it registers the four models that the other examples leave at their
defaults, so the scenario is assembled almost entirely from custom code:

| Extension point | Class |
| --- | --- |
| Computing nodes generator | `Example23TypeGuaranteeGenerator` |
| Task generator | `Example23BurstTaskGenerator` |
| Topology creator | `Example23FullMeshTopologyCreator` |
| Simulation manager | `Example23AuditSimulationManager` |

The four classes extend their defaults, override one method each, and call
`super` everywhere else. Registration is three lines plus one in `Example23`:

```java
Simulation sim = new Simulation();
sim.setCustomComputingNodesGenerator(Example23TypeGuaranteeGenerator.class);
sim.setCustomTaskGenerator(Example23BurstTaskGenerator.class);
sim.setCustomTopologyCreator(Example23FullMeshTopologyCreator.class);
sim.setCustomSimulationManager(Example23AuditSimulationManager.class);
sim.launchSimulation();
```

Together, the examples now exercise all eight setter-based extension points:

| Extension point | Demonstrated by |
| --- | --- |
| Mobility model | [Example 2](../Example2/README.md) |
| Computing node | [Example 4](../Example4/README.md), [Example 7](../Example7/README.md), [Example 21](../Example21/README.md), [Example 22](../Example22/README.md) |
| Edge orchestrator | [Example 8](../Example8/README.md) to [Example 22](../Example22/README.md) |
| Network model | [Example 7](../Example7/README.md), [Example 21](../Example21/README.md) |
| Task generator | this example |
| Topology creator | this example |
| Computing nodes generator | this example |
| Simulation manager | this example |
| Task class (`setCustomTaskClass`) | documented in [Customizing models](../../../docs/customizing-models.md), tested in the unit suite |

## The four hooks

### 1. Computing nodes generator: never drop a declared type

The default allocation divides the fleet by the declared percentages with the
largest remainder method. It is fair to the shares, but a share smaller than one
device rounds to zero. With 20 devices, the 2 percent wearable type of this
scenario earns 0.4 of a device and disappears from the fleet.

The custom generator keeps the default allocation and then reserves one device
for every positive share that received none, taking it from the type with the
most devices:

```java
@Override
protected int[] allocateInstances(List<Double> percentages) {
    int[] allocation = super.allocateInstances(percentages);
    for (int type = 0; type < allocation.length; type++) {
        if (percentages.get(type) <= 0 || allocation[type] > 0)
            continue;
        int donor = largestAllocation(allocation);
        if (donor < 0)
            break;
        allocation[donor]--;
        allocation[type] = 1;
    }
    return allocation;
}
```

In the run, the 39/29/30/2 shares become 8/6/6/0 after rounding and 7/6/6/1 after
the reservation. The fleet keeps the 20 devices requested by the scenario.

### 2. Task generator: synchronize the bursts

The default generator spreads each minute's tasks over the first fifteen seconds
of that minute, with a random offset per device. The custom generator starts
every minute at the same instant for every device:

```java
@Override
protected void generateTasksForDevice(ComputingNode dev, int app) {
    Application application = SimulationParameters.applicationList.get(app);
    if (application.hasRandomArrival()) {
        super.generateTasksForDevice(dev, app);
        return;
    }
    for (int minute = 0; minute < (int) simulationTime; minute++)
        insert(minute * 60, app, dev);
}
```

This models a fleet that reports on the minute mark, such as meter readings or
scheduled uploads. All devices then contend for the network and the servers at
the same instant. Applications that declare an arrival pattern keep the default
per-gap generation.

### 3. Topology creator: complete the backbone

The topology file declares a ring: each edge data center connects to its two
neighbors. The custom creator keeps those links and adds a MAN link in both
directions between every pair that is not directly connected:

```java
@Override
public void generateTopologyGraph() {
    super.generateTopologyGraph();
    int added = connectAllPairs(infrastructureTopology, simulationManager,
            computingNodesGenerator.getEdgeOnlyList());
    infrastructureTopology.savePathsToMap(computingNodesGenerator.getEdgeAndCloudList());
}
```

The ring needs two hops between opposite data centers, with 10 milliseconds of
MAN latency per hop. The full mesh needs one hop. The cached shortest paths are
recomputed after the links are added, which is the rule the customizing guide
states for any topology change.

### 4. Simulation manager: audit the failures

The default manager reports the aggregate success rate. The custom manager
records every task outcome as it happens and groups the failures by reason:

```java
@Override
public boolean taskFailed(Task task, int phase) {
    boolean failed = super.taskFailed(task, phase);
    if (failed)
        recordFailure(task);
    return failed;
}

@Override
public void processEvent(Event ev) {
    super.processEvent(ev);
    if (ev.getTag() == RESULT_RETURN_FINISHED && ev.getData() instanceof Task)
        recordSuccess((Task) ev.getData());
}
```

The audit is printed when the engine notifies its end listeners, so it appears
after the standard report. A task that was never sent because its device died is
counted apart from the tasks that failed during execution.

## The scenario

`settings/` holds 20 devices with four types on a 2000 by 2000 meters map: 39
percent smartphones on cellular, 29 percent tablets and 30 percent laptops on
Wi-Fi, and 2 percent wearables. Four edge data centers form a ring, and the
architecture is `ALL` with the built-in `TRADE_OFF` policy, so the custom models
are the only thing that differs from a stock scenario. The run lasts five
minutes with seed 83.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example23"
```

## What to look for

The console shows one line per hook before the standard report:

```text
Example23TypeGuaranteeGenerator - Device type 4 declares 2.0 % of the fleet, a share that rounds to zero: one device reserved from type 1
Example23FullMeshTopologyCreator - Added 4 directed MAN links to complete the full mesh of 4 edge data centers
```

and one line after it:

```text
Example23AuditSimulationManager - audit: 1248 succeeded, 202 failed, 0 not generated (FAILED_DUE_TO_LATENCY 202)
```

The audit matches the standard report: 1248 of 1450 tasks succeeded, 86.07
percent, and every failure is a deadline miss. The reservation line proves the
wearable type is present; without it the fleet would contain 20 devices of the
other three types only.

## Measured run

| Metric | Stock models | This example |
| --- | --- | --- |
| Generated tasks | 1450 | 1450 |
| Tasks successfully executed | 79.24 % | 86.07 % |
| Failed due to latency | 301 | 202 |
| Average task execution time | 0.1406 s | 0.1946 s |
| Network usage | 54.42 s | 54.51 s |
| Transferred data | 1693.78 MB | 1653.48 MB |
| WAN usage | 17.40 s | 0.58 s |
| MAN usage | 0 s | 0 s |

Both runs read the same files. The comparison is context, not an ablation: the
reserved wearable replaces one smartphone, and synchronized bursts consume the
shared random stream differently, so the two fleets do not issue identical
tasks. The table shows that replacing four models changes the outcome
substantially while the scenario file stays the same.

MAN usage stays at zero in this run because every task was served by the data
center closest to its device, and a two-tier placement never crosses the
backbone. The full mesh pays off for policies that move work between data
centers; for the built-in `TRADE_OFF` it only shortens the paths and adds a
redundant route, which the unit tests verify on the graph itself.

## Tune it

- Reserve the rare type from a specific donor instead of the largest one, for
  example always from the most powerful type.
- Change the synchronization offset from `minute * 60` to spread the bursts,
  or align them on an hour boundary.
- Remove the `savePathsToMap` call after adding links and compare the routing
  delay: the network model uses the cached paths.
- Add a failure reason to the audit summary only when its count is positive, to
  shorten the line in large runs.

## Files

- `Example23.java`
- `Example23TypeGuaranteeGenerator.java`
- `Example23BurstTaskGenerator.java`
- `Example23FullMeshTopologyCreator.java`
- `Example23AuditSimulationManager.java`
- `examples/Example23/settings/`

## Where to go next

- [Customizing models](../../../docs/customizing-models.md) for the constructor and
  event-tag rules every custom model must follow
- [Architecture](../../../docs/architecture.md) for how the models are loaded and
  connected
- [Example 21](../Example21/README.md) and [Example 22](../Example22/README.md)
  for custom nodes and network models that change behavior, not only structure
