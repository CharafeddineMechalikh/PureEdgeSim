# Example 1: wiring the extension points

## What it shows

This is the smallest runnable simulation and the tour of the `Simulation`
class. It is the example to read first if you want to know where PureEdgeSim
lets you replace a model. It registers one class for every point the framework
exposes, then launches the run:

| Extension point | Class used here |
| --- | --- |
| Mobility model | `Example2CustomMobilityModel` |
| Task orchestrator | `DefaultOrchestrator` |
| Computing node | `Example4CustomComputingNode` |
| Task generator | `DefaultTaskGenerator` |
| Network model | `DefaultNetworkModel` |
| Simulation manager | `DefaultSimulationManager` |
| Topology creator | `DefaultTopologyCreator` |

The point is not the classes themselves. Three of them are the framework
defaults, which shows that registering a default explicitly changes nothing.
The other two are the small custom models from Example 2 and Example 4, wired
here so every hook is visible in one place. Removing any `setCustom...` line
restores the default for that hook.

## The scenario

This example uses the repository default settings in `settings/`: a 200 by 200
meters map, 100 to 300 edge devices, a ten minute run, and the default
applications. Real time charts are enabled by default, so a Swing window opens
while the simulation runs.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example1"
```

## What to look for

- The chart window updates while the simulation runs, at the period set by
  `charts_update_interval`.
- The console prints the scenario progress and the final report.
- Results are written to a timestamped folder under `output/`, next to a
  `run.properties` file with the resolved seed, version, and input paths.
- The custom computing node prints one line when it receives its own start
  event. That line comes from Example 4, wired through this example.

## Files

- `Example1.java`
- `settings/` at the repository root

## Where to go next

- [Writing orchestrators](../../../docs/writing-orchestrators.md)
- [Customizing models](../../../docs/customizing-models.md)
- [Getting started](../../../docs/getting-started.md)
