---
description: "Write a PureEdgeSim workload orchestrator: the decision contract, candidate list, complete examples, registration, state, testing, and design advice."
---
# Writing orchestrators

The orchestrator is the policy of the simulator. It decides where each task runs. Everything else (task generation, networking, execution, energy) is mechanism, and the orchestrator is the part most researchers replace.

This guide explains the contract, the information available to a decision, how to write and test an orchestrator, and the mistakes that are easy to make. It assumes that you have read [Task lifecycle](task-lifecycle.md) and [Models](models.md).

## What the orchestrator is called for

An orchestrator is a `SimEntity` created once per simulation. When a task reaches the orchestration stage, `DefaultSimulationManager` calls `edgeOrchestrator.orchestrate(task)`. That method delegates to your decision function:

```java
protected abstract int findComputingNode(String[] architectureLayers, Task task);
```

Return the index of the chosen node in `nodeList`, or `-1` when no node is acceptable. The base class then:

1. Validates that the selected node is not a sensor.
2. Stores the node as the task's offloading destination.
3. Records the node as the generating device's application placement.
4. Logs the decision when deep logging is enabled.

A `-1` return is not an exception. The task is marked as failed with `NO_OFFLOADING_DESTINATIONS` and counted under tasks that failed for lack of resources. Returning a sensor index throws an `IllegalArgumentException`.

## The two calls you must understand

### `findComputingNode`

This is called only when the device does not already have an application placement. In the default flow, the first task of a device triggers a decision, and the chosen destination is reused for the following tasks. The placement survives until:

- any task of that device fails (`setFailed` clears it),
- the device moves far enough that its device-to-device destination is out of range,
- your code clears it with `task.getEdgeDevice().setApplicationPlaced(false)`.

Your decision function can therefore be called once per device for the whole simulation, or many times if failures are frequent. Do not assume it is called for every task.

Placement is a simplification. If your algorithm needs a per-task decision, clear the placement at the end of the decision, or override `orchestrate` in your class. Clearing it too aggressively has a cost: each task then pays the request transfer to the orchestrator again.

### Placing the subtasks of a service graph

When an application declares a service graph, one request is a set of subtasks, and a partition plan usually sends different subtasks to different nodes. The placement cache works against that: the first subtask would store its destination and every later subtask of the same device would reuse it. Clear the placement after each decision in the orchestrator:

```java
@Override
public void orchestrate(Task task) {
    super.orchestrate(task);
    task.getEdgeDevice().setApplicationPlaced(false);
}
```

`findComputingNode` can then read `task.getSubtaskName()` and `task.getApplicationID()` to look up the partition or the node chosen for that subtask, and fall back to `tradeOff` when the named node is not eligible for this request. Two examples implement the pattern on the same video analytics graph:

- `Example9` (`examples.Example9PartitioningOrchestrator`) cuts each connected component into balanced partitions with a graph-theory heuristic, then maps the partitions to mist, edge, and cloud.
- `Example10` (`examples.Example10GeneticOrchestrator`) searches the node of every subtask with a genetic algorithm, whose fitness estimates the pipeline makespan, the result transfers, and the load imbalance.

Both print the plan they computed before the simulation starts, so the two strategies can be compared on the same scenario.

### Static and dynamic partition plans

The task graph always comes from `applications.xml`, so partitioning never decomposes a task. It only decides how the declared subtasks are grouped and where each group runs, and that decision can be taken at two moments.

**Static.** Build the plan once, in the orchestrator constructor, from `SimulationParameters.applicationList`, then look it up by application identifier and subtask name in `findComputingNode`. `Example9` and `Example10` work this way. A static plan is cheap, reproducible, and easy to print before the run, but it cannot see a device that died, a node whose queue filled up, or a destination that moved out of range.

**Dynamic.** Recompute the grouping or the destination per request, either inside `findComputingNode` or before calling `super` in `orchestrate`, from the live candidate list and the task attributes:

```java
@Override
protected int findComputingNode(String[] architecture, Task task) {
    int selected = -1;
    double lowestLoad = Double.MAX_VALUE;
    for (int index = 0; index < nodeList.size(); index++) {
        ComputingNode node = nodeList.get(index);
        if (!offloadingIsPossible(task, node, architectureLayers))
            continue;
        // CPU utilization in [0, 1]; each queued task weighs one full CPU.
        double load = node.getCurrentCpuUtilization() / 100.0 + node.getTasksQueue().size();
        if (load < lowestLoad) {
            lowestLoad = load;
            selected = index;
        }
    }
    return selected;
}
```

A dynamic plan reacts to death, mobility, and queue growth, and pays a decision per subtask. A common middle ground keeps the partition membership static and chooses the node of each partition dynamically among the eligible nodes, which bounds the recomputation while still following the state of the run. `Example24DynamicRepartitionOrchestrator` is a complete dynamic plan: it assigns a tier to every subtask, refreshes the assignment every 30 seconds, replans immediately when a planned tier loses all of its nodes, and counts the subtasks that moved. Its README runs the same scenario with a frozen plan and compares request success and makespan.

Whichever moment you choose, clear the application placement after every decision as shown above. Otherwise the first subtask stores its destination and later subtasks of the same device never reach `findComputingNode`.

### `resultsReturned`

This callback fires after the task's results have reached the generating device. It is also called for failures that occur after the task has left the orchestrator (phases 2 and 3), not for failures before that point. Use it to update learned state:

```java
@Override
public void resultsReturned(Task task) {
    if (task.getFailureReason() == null) {
        totalDelay += task.getTotalDelay();
        completedTasks++;
    }
}
```

`getTotalDelay()` is network time plus waiting time plus CPU time for that task. `getWatingTime()` is spelled that way in the API and returns the queueing delay at the destination. `getActualCpuTime()` returns the execution time.

## The candidate list

`nodeList` is fixed for the whole simulation and depends on the architecture name of the current scenario:

| Architecture | Contents |
| --- | --- |
| `CLOUD_ONLY` | Cloud nodes |
| `EDGE_ONLY` | Edge data centers |
| `MIST_ONLY` | Edge devices, sensors excluded |
| `EDGE_AND_CLOUD` | Cloud nodes and edge data centers |
| `MIST_AND_CLOUD` | Cloud nodes and edge devices, sensors excluded |
| `MIST_AND_EDGE` | Edge devices and edge data centers, sensors excluded |
| `ALL` | Cloud nodes, edge data centers, and edge devices, sensors excluded |

The list is built in `initialize()`, which the `Orchestrator` constructor calls. `algorithmName`, `architectureName`, and `architectureLayers` are also set there from the current scenario.

Use the provided eligibility helper rather than reimplementing the rules:

```java
offloadingIsPossible(task, node, architectureLayers)
```

It enforces the layer membership, the one-hop attachment rule for edge data centers, the range rule for Mist nodes, and liveness. If you bypass it, you can produce physically impossible decisions, such as offloading to an edge data center the device is not connected to.

Cloud nodes are always considered reachable. Edge data centers are eligible when they are the current attachment of the task's device or of its orchestrator. Mist devices are eligible when they are within `edge_devices_range` of the device or its orchestrator, are alive, and are not sensors.

## A first complete orchestrator

The smallest useful custom orchestrator routes every task to the cloud:

```java
package examples;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

public class CloudOnlyOrchestrator extends Orchestrator {

    public CloudOnlyOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
    }

    @Override
    protected int findComputingNode(String[] architectureLayers, Task task) {
        for (int i = 0; i < nodeList.size(); i++) {
            ComputingNode node = nodeList.get(i);
            if (node.getType() == SimulationParameters.TYPES.CLOUD
                    && offloadingIsPossible(task, node, architectureLayers)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void resultsReturned(Task task) {
        // No state to update.
    }
}
```

When the scenario architecture is `MIST_ONLY` or `EDGE_ONLY`, the candidate list contains no cloud node, so every task returns `-1` and fails. That is correct behavior for an orchestrator that only understands cloud offloading, and it is why the default scenario, which sweeps all architectures, will show a high failure count for those iterations.

## A more realistic orchestrator

The default TRADE_OFF algorithm ignores queue state, battery, and network conditions. The following orchestrator uses the current queue length, an execution-time estimate, the layer, and the battery level:

```java
package examples;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

public class LoadAndBatteryAwareOrchestrator extends Orchestrator {

    protected static final double MIN_BATTERY_PERCENT = 20;

    public LoadAndBatteryAwareOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
    }

    @Override
    protected int findComputingNode(String[] architectureLayers, Task task) {
        int selected = -1;
        double bestScore = Double.MAX_VALUE;

        for (int i = 0; i < nodeList.size(); i++) {
            ComputingNode node = nodeList.get(i);
            if (!offloadingIsPossible(task, node, architectureLayers))
                continue;

            // Do not drain a battery-powered Mist device below the reserve.
            if (node.getType() == SimulationParameters.TYPES.EDGE_DEVICE
                    && node.getEnergyModel().isBatteryPowered()
                    && node.getEnergyModel().getBatteryLevelPercentage() < MIN_BATTERY_PERCENT)
                continue;

            // The destination needs RAM for the container.
            if (node.getAvailableRam() <= task.getContainerSizeInMBytes())
                continue;

            double executionTime = task.getLength() / node.getMipsPerCore();
            double estimatedQueueTime = node.getTasksQueue().size() * executionTime;

            double layerPenalty = 1.0;
            if (node.getType() == SimulationParameters.TYPES.EDGE_DEVICE)
                layerPenalty = 1.2;
            else if (node.getType() == SimulationParameters.TYPES.CLOUD)
                layerPenalty = 1.8;

            double score = (executionTime + estimatedQueueTime) * layerPenalty;
            if (score < bestScore) {
                bestScore = score;
                selected = i;
            }
        }
        return selected;
    }

    @Override
    public void resultsReturned(Task task) {
        // Hook for online learning or statistics.
    }
}
```

This is still a heuristic, not a latency predictor. It sees queue length and capacity, but not the transfer time, the WAN utilization, or the energy that the transfer will consume. A network-aware version can read:

- `node.getCurrentLink(ComputingNode.LinkOrientation.UP_LINK)` for the device attachment link,
- `simulationManager.getDataCentersManager().getTopology().getDelay(a, b)` for the shortest-path latency between two nodes,
- `simulationManager.getNetworkModel().getWanUpUtilization()` when one shared WAN link is enabled. This call throws if `use_one_shared_wan_link=false`.
- `node.getCurrentCpuUtilization()`, `node.getAvgCpuUtilization()`, `node.isIdle()`, `node.getAvailableStorage()`.

Be careful with `getAvgCpuUtilization()`: it is a lifetime average, not the current load. For an instantaneous view, use `getCurrentCpuUtilization()` or the queue length.

## Registering the orchestrator

A custom orchestrator is selected before the simulation starts:

```java
Simulation simulation = new Simulation();
simulation.setCustomEdgeOrchestrator(LoadAndBatteryAwareOrchestrator.class);
simulation.launchSimulation();
```

The class is instantiated reflectively with a `(SimulationManager)` constructor. If the constructor is missing or has a different signature, the simulation fails during model loading.

The algorithm names in `simulation_parameters.properties` are passed to your class as `algorithmName`. You decide what they mean. Set an algorithm name that your class handles:

```properties
orchestration_algorithms=LOAD_AND_BATTERY
```

Then dispatch on it:

```java
@Override
protected int findComputingNode(String[] architectureLayers, Task task) {
    if ("LOAD_AND_BATTERY".equals(algorithmName))
        return loadAndBattery(architectureLayers, task);
    throw new IllegalArgumentException("Unknown algorithm: " + algorithmName);
}
```

Architecture names are not free-form. They are consumed by `Orchestrator.initialize()` and by the scenario parser, so keep the seven documented names (`ALL`, `CLOUD_ONLY`, `EDGE_ONLY`, `MIST_ONLY`, `EDGE_AND_CLOUD`, `MIST_AND_CLOUD`, `MIST_AND_EDGE`). Algorithm names are free-form.

To compare a custom algorithm with the built-in ones, extend `DefaultOrchestrator` and delegate:

```java
public class MyOrchestrator extends DefaultOrchestrator {

    public MyOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
    }

    @Override
    protected int findComputingNode(String[] architectureLayers, Task task) {
        if ("TRADE_OFF".equals(algorithmName) || "ROUND_ROBIN".equals(algorithmName))
            return super.findComputingNode(architectureLayers, task);
        if ("MY_ALGORITHM".equals(algorithmName))
            return myAlgorithm(architectureLayers, task);
        throw new IllegalArgumentException("Unknown algorithm: " + algorithmName);
    }
}
```

Now a single scenario sweep can compare `TRADE_OFF`, `ROUND_ROBIN`, and `MY_ALGORITHM` in one CSV.

## Where to keep state

Instance fields on the orchestrator are per-simulation, because each scenario creates a new orchestrator. Store counters, running averages, or learned tables there. Do not use static mutable fields, because parallel mode runs several simulations in the same JVM and static state would leak across scenarios.

The simulation manager, logger, network model, and computing nodes are all accessible:

```java
protected SimulationManager simulationManager;   // inherited
protected SimLog simLog;                          // inherited
protected List<ComputingNode> nodeList;           // inherited
```

`simulationManager.getSimulation().clock()` returns the current simulated time in seconds. Use it for time-dependent policies, for example an epsilon-greedy strategy that explores more at the beginning.

## What you cannot do from `findComputingNode`

The decision is synchronous. You cannot wait for a message, query a remote service, or run an experiment inside the method. If your algorithm needs information that is not available locally, precompute it in the constructor or in `onSimulationStart()`, or maintain it incrementally in `resultsReturned`.

You also cannot change the candidate list, create nodes, or alter the topology during a decision. The topology is built before the first task, and changing it mid-run requires a custom network model.

## Debugging

Enable deep logging in the properties file:

```properties
deep_log_enabled=true
```

The base class then logs each assignment with time, task identifier, node type, and node identifier. Your own diagnostics can use:

```java
simLog.deepLog(simulationManager.getSimulation().clock() + ": score=" + score + " node=" + node.getId());
```

Deep logging produces a very large text report and slows the simulation down. Use it on a short scenario, then disable it.

Common mistakes:

- Returning a node identifier instead of the index in `nodeList`. The base class indexes the list with your return value.
- Returning `-1` for tasks that could run locally. Local execution still needs a valid destination; if no node is eligible, the task fails.
- Forgetting that `nodeList` changes with the architecture. A cloud-only policy will fail every `MIST_ONLY` iteration.
- Assuming `resultsReturned` is called for every task. It is not called for failures that happen before the results leaves the orchestrator.
- Using `getAvgCpuUtilization()` as an instantaneous load signal.
- Keeping per-device state in a static map.

## Testing an orchestrator

Fast checks that do not require a full sweep:

1. Set `simulation_time=1`, one architecture, one algorithm, and one device count. The whole run should finish in seconds.
2. Run with `deep_log_enabled=true` and inspect a few assignments by hand.
3. Run once with `CLOUD_ONLY` and once with `MIST_ONLY` to confirm that your eligibility logic behaves differently when the candidate list changes.
4. Run with `use_one_shared_wan_link=true` if your policy calls `getWanUpUtilization()`, and with it false to confirm that the call is guarded.

For automated tests, instantiate the orchestrator with a real `SimulationManager` created by a small simulation, as the test suite does. Testing a decision in isolation with hand-built nodes is misleading, because the eligibility rules depend on current links and mobility state, which are only consistent after the topology is generated.

## Design advice

- State the question first. If you compare algorithms, keep everything except the decision fixed and run enough repetitions to average out task-time randomness.
- Report failure reasons separately. A policy that fails tasks on latency looks different from one that fails for lack of destinations.
- Prefer simple baselines. TRADE_OFF and ROUND_ROBIN are deliberately simple; a complex policy that does not beat them clearly is not yet a contribution.
- Record what the policy saw. If the paper claims network awareness, the implementation should actually read the network state, not just the node list.
- Keep the decision deterministic given its inputs, unless exploration is part of the contribution. Reproducibility depends on it.

## Where to go next

- Understand when the orchestrator is called: [Task lifecycle](task-lifecycle.md).
- Replace the underlying network or computing model: [Customizing models](customizing-models.md).
- Measure the result correctly: [Output and metrics](output-and-metrics.md).
