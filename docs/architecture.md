# Architecture

This guide explains how PureEdgeSim is organized internally: the modules, the objects that own the simulation state, the initialization sequence, and the event engine that advances time. Read it before modifying the framework or debugging unexpected behavior. If you only want to run experiments, [Getting started](getting-started.md) and [Configuration](configuration.md) are enough.

## Design principles

PureEdgeSim is built around four ideas:

1. **Separation of concerns.** Each module owns one aspect of the simulated system: topology, computing resources, mobility, networking, energy, task generation, orchestration, logging, and visualization.
2. **Defaults plus extension points.** Every module has a working default implementation. Researchers replace only the model their contribution concerns, usually by subclassing a default class and registering the subclass with `Simulation`.
3. **No modification of the framework.** Custom behavior is added by subclassing, not by editing PureEdgeSim. This keeps experiments compatible with bug fixes and performance improvements.
4. **Scenario-driven experimentation.** A simulation file describes which architectures, algorithms, and device counts to evaluate. The framework expands that specification into independent runs and writes one result row per run.

## Package map

The source tree is under `src/main/java/com/mechalikh/pureedgesim/`.

| Package | Responsibility | Key classes |
| --- | --- | --- |
| `simulationengine` | Discrete-event engine, event queue, simulation entities, lifecycle listeners | `PureEdgeSim`, `SimEntity`, `Event`, `FutureQueue`, `Queue`, `OnSimulationStartListener`, `OnSimulationEndListener` |
| `simulationmanager` | Simulation orchestration, scenario expansion, logging, event handling, charts from CSV | `Simulation`, `SimulationAbstract`, `SimulationThread`, `SimulationManager`, `DefaultSimulationManager`, `SimLog`, `ChartsGenerator` |
| `scenariomanager` | Configuration parsing and global parameters | `SimulationParameters`, `ParametersParser`, `ApplicationFileParser`, `DatacentersParser`, `EdgeDevicesParser`, `ComputingNodesParser`, `Scenario` |
| `datacentersmanager` | Computing nodes, resource generation, topology creation | `ComputingNode`, `DefaultComputingNode`, `ComputingNodesGenerator`, `DefaultComputingNodesGenerator`, `DataCentersManager`, `TopologyCreator`, `DefaultTopologyCreator` |
| `network` | Links, bandwidth allocation, routing, transfer progress | `NetworkModel`, `DefaultNetworkModel`, `NetworkLink`, `InfrastructureGraph`, `TransferProgress` |
| `energy` | Energy consumption models for nodes and links | `EnergyModelComputingNode`, `EnergyModelNetworkLink` |
| `locationmanager` | Position and mobility | `Location`, `MobilityModel`, `DefaultMobilityModel` |
| `taskgenerator` | Task model and workload generation | `Task`, `TaskAbstract`, `DefaultTask`, `TaskGenerator`, `DefaultTaskGenerator`, `Application` |
| `taskorchestrator` | Offloading decisions | `Orchestrator`, `DefaultOrchestrator` |
| `simulationvisualizer` | Live charts during a run | `SimulationVisualizer`, `Chart`, `MapChart`, `CPUChart`, `TasksChart`, `WanChart` |

The `examples` package is deliberately outside the core packages. It demonstrates how to instantiate and extend the framework, and it is excluded from the published Javadoc.

## The object graph

At runtime, one simulation iteration is a graph of objects:

```
Simulation                      owns configuration paths and custom class choices
└── SimulationThread            one per scenario, controls one full iteration
    ├── PureEdgeSim             event engine and clock
    ├── SimLog                  per-iteration metrics and output files
    ├── SimulationManager       receives engine events, drives the task lifecycle
    │   ├── DataCentersManager
    │   │   ├── ComputingNodesGenerator   creates cloud, edge, and device nodes
    │   │   └── TopologyCreator           wires links and shortest paths
    │   ├── Orchestrator        chooses execution destinations
    │   ├── TaskGenerator       creates the task list
    │   ├── NetworkModel        transfers requests and results
    │   └── SimulationVisualizer (optional, live charts only)
    └── SimEntity instances     all scheduler-driven participants
```

The important ownership rule is that **`SimulationManager` is the hub**. It knows the data centers manager, the orchestrator, the task generator output, the network model, the logger, and the engine. Custom models receive the manager in their constructor and access everything through it.

## Initialization sequence

The sequence below describes one call to `new Simulation().launchSimulation()`.

### 1. Parse and validate configuration

`Simulation.launchSimulation()` calls `checkFiles()`, which runs five parsers:

1. `ParametersParser` reads `simulation_parameters.properties`.
2. `ApplicationFileParser` reads `applications.xml`.
3. `DatacentersParser` reads `cloud.xml` and `edge_datacenters.xml`.
4. `EdgeDevicesParser` reads `edge_devices.xml`.
5. `ComputingNodesParser` validates cross-file consistency, for example that the device percentages and orchestrator counts are coherent.

Every parser validates range constraints and throws `IllegalArgumentException` with a message that names the offending parameter and file. Parsing also performs unit conversions, which are documented in [Configuration](configuration.md).

### 2. Expand the scenario sweep

`loadScenarios()` builds the Cartesian product of:

- the architectures in `orchestration_architectures`,
- the algorithms in `orchestration_algorithms`,
- the device counts from `min_number_of_edge_devices` to `max_number_of_edge_devices` in steps of `edge_device_counter_size`.

Each combination becomes a `Scenario` object that stores the architecture name, algorithm name, and device count. The order is deterministic: architectures outer, algorithms middle, device counts inner.

### 3. Run each scenario

If `parallel_simulation=false`, the scenarios run sequentially in the current thread. If `parallel_simulation=true`, PureEdgeSim uses a parallel stream with a pool sized to `min(availableProcessors, scenarioCount)`. Each worker executes `new SimulationThread(simulation, simulationId, iteration).startSimulation()`.

Parallelism changes two things:

- Live charts are disabled, because a single Swing window cannot represent multiple concurrent simulations.
- Output files are named `Parallel_simulation_<id>.csv` instead of `Sequential_simulation.csv`.

Each iteration is otherwise independent. Results are written by that iteration's `SimLog`.

### 4. Create the engine and the manager

`SimulationThread.startSimulation()` performs these steps for one scenario:

1. Create a `SimLog` for the iteration. If this is the first iteration and `clear_output_folder=true`, previous output is deleted.
2. Instantiate a fresh `PureEdgeSim` engine with `SimulationParameters.simulationDuration`.
3. Reflectively instantiate the configured `SimulationManager` subclass using the constructor `(SimLog, PureEdgeSim, int simulationId, int iteration, Scenario)`.
4. Call `loadModels(simulationManager)`.
5. Call `simulationManager.startSimulation()`.
6. Pause for `pause_length` seconds (a visual aid between iterations).
7. Generate summary charts from the CSV if `save_charts=true` and the run was sequential.

### 5. Load the models

`SimulationThread.loadModels()` instantiates the models in dependency order:

1. **Network model.** Instantiated reflectively with a `SimulationManager` constructor. The constructor creates the `InfrastructureGraph` and registers link types.
2. **Data centers manager.** `new DataCentersManager(manager, mobilityClass, computingNodeClass, generatorClass, topologyClass)`. It creates the computing nodes generator, generates all nodes, creates the topology, and connects the result to the network model.
3. **Task generator.** Instantiated reflectively with a `SimulationManager` constructor, then `generate()` is called. The resulting `FutureQueue<Task>` is handed to the manager.
4. **Orchestrator.** Instantiated reflectively with a `SimulationManager` constructor. Its constructor calls `initialize()`, which selects the node list that matches the architecture, and registers itself with the manager.

All model classes must therefore expose a public constructor with the exact signature expected by the loader. This is the most common cause of `NoSuchMethodException` when plugging in a custom model. See [Customizing models](customizing-models.md).

### 6. Start the event loop

`SimulationManager.startSimulation()` optionally creates the live visualizer, prints the scenario header, and calls `PureEdgeSim.start()`.

`PureEdgeSim.start()`:

1. Sets the clock to 0 and marks the engine as running.
2. Calls `onSimulationStart()` on every registered `SimEntity` and on the simulation manager.
3. Repeatedly removes the earliest event from the `FutureQueue`, advances the clock to its time, and calls `processEvent(event)` on the target entity.
4. Stops when the queue is empty or `terminate()` has been called.
5. Calls `onSimulationEnd()` on listeners.
6. Clears the entity list so the next iteration starts clean.

## The event engine

### Time and ordering

Simulation time is a `double` measured in seconds. The queue orders events by time and, for equal times, by insertion serial number. This gives deterministic ordering when a scenario is run sequentially, which is essential for reproducible experiments.

- `schedule(SimEntity destination, double delay, int tag, Object data)` schedules an event `delay` seconds in the future.
- `scheduleNow(...)` is a shorthand for a zero-delay event.
- `clock()` returns the current simulation time in seconds.
- `clockInMinutes()` returns it in minutes.

Events are **not** executed immediately when scheduled, even with zero delay. They are executed when the engine reaches them in the queue. This is why a zero-delay event can still allow other events at the same timestamp to run first, depending on their serial numbers.

### Simulation entities

A `SimEntity` is any object that reacts to events. The base class provides `schedule`, `scheduleNow`, `processEvent`, and lifecycle callbacks. Every model that needs timed behavior extends `SimEntity` and registers itself with the engine, usually by calling `super(simulation)` in its constructor.

### Event tags

`DefaultSimulationManager` uses the following tags. Custom simulation managers can define additional tags.

| Constant | Value | Purpose |
| --- | --- | --- |
| `PRINT_LOG` | 1 | End of the simulation or periodic progress check; triggers final aggregation and termination. |
| `SHOW_PROGRESS` | 2 | Updates the progress bar during execution. |
| `EXECUTE_TASK` | 3 | The destination received a task and should submit it to its execution queue. |
| `TRANSFER_RESULTS_TO_ORCH` | 4 | The task finished executing and its results should return to the orchestrator. |
| `RESULT_RETURN_FINISHED` | 5 | Results reached the source device; the task is complete. |
| `SEND_TO_ORCH` | 6 | A generated task is sent to its orchestrator. |
| `UPDATE_REAL_TIME_CHARTS` | 7 | Refreshes the live visualization. |
| `SEND_TASK_FROM_ORCH_TO_DESTINATION` | 8 | The orchestrator made a decision and the request travels to the destination. |
| `NEXT_BATCH` | 9 | Schedules the next batch of generated tasks. |

The full flow through these tags is described in [Task lifecycle](task-lifecycle.md).

### Task batching

Generating hundreds of thousands of events at time zero would make the queue large and slow. Instead, `DefaultSimulationManager` schedules only the first `batch_size` tasks at the start of the simulation. When that batch has been scheduled, a `NEXT_BATCH` event schedules the next one, and so on. Batching does not change the simulated task arrival times; it only changes when their events enter the queue.

### Termination

The manager schedules `PRINT_LOG` at `simulationDuration`. When it fires, the manager checks whether all generated tasks have been accounted for. If `wait_for_all_tasks=true`, it reschedules `PRINT_LOG` every 10 simulated seconds until every task has succeeded or failed. This can extend the simulated time beyond the configured duration, because a task already in execution is allowed to finish.

If `wait_for_all_tasks=false`, the simulation terminates at the configured duration even if tasks are still in flight. Tasks not completed are counted in the results according to their state at termination. This is why the documentation recommends leaving the option enabled unless simulation time is a serious constraint.

## State and determinism

Each iteration creates new instances of the engine, the manager, all models, and the logger. The only shared state is the static configuration in `SimulationParameters` and the application list, both of which are parsed once and then read-only. This design makes iterations independent.

However, there are two practical caveats:

- **Randomness is not seeded explicitly.** Task generation and mobility use a `Random` instance. Runs are not guaranteed to be bit-for-bit reproducible across JVMs or after code changes. If you need reproducible experiments, set a seed in a custom task generator or custom mobility model.
- **Parallel runs share the JVM.** Static state is read-only in normal operation, but custom models that write to static fields can introduce cross-run interference. Keep custom model state in instance fields.

## Where to plug in

The `Simulation` class exposes setters for each replaceable model. The table below lists the class you subclass, the constructor your subclass must provide, and the behavior you normally override.

| Extension point | Setter | Required constructor | Main methods |
| --- | --- | --- | --- |
| Mobility model | `setCustomMobilityModel` | `(SimulationManager, Location)` | `updateLocation`, `distanceTo` |
| Computing node | `setCustomComputingNode` | `(SimulationManager, double mipsPerCore, int cores, double storage, double ram)` | Device state, execution, resource management |
| Task generator | `setCustomTaskGenerator` | `(SimulationManager)` | `generate` |
| Orchestrator | `setCustomEdgeOrchestrator` | `(SimulationManager)` | `findComputingNode`, `resultsReturned` |
| Network model | `setCustomNetworkModel` | `(SimulationManager)` | `processEvent`, `transferFinished` |
| Simulation manager | `setCustomSimulationManager` | `(SimLog, PureEdgeSim, int, int, Scenario)` | `startSimulation`, `processEvent` |
| Topology creator | `setCustomTopologyCreator` | `(SimulationManager, ComputingNodesGenerator)` | `generateTopologyGraph` |
| Computing nodes generator | `setCustomComputingNodesGenerator` | `(SimulationManager, Class<? extends MobilityModel>, Class<? extends ComputingNode>)` | `generateDatacentersAndDevices` |

The exact signatures are documented in [Customizing models](customizing-models.md) and in the Javadoc.

## Output architecture

Output is produced in three places:

1. **`SimLog`** accumulates metrics during the run, prints the console report, and writes the CSV and optional text file. One `SimLog` exists per iteration.
2. **`ChartsGenerator`** reads the completed CSV and produces aggregate comparison charts under `Final results/`, grouped by algorithm and by architecture.
3. **`SimulationVisualizer`** draws the live charts and, when enabled, saves per-iteration images.

The separation matters for reproducibility: the CSV is the authoritative result. Charts are derived from it. You can re-plot results without rerunning the simulation.

## Related reading

- [Task lifecycle](task-lifecycle.md): the event flow in detail.
- [Simulation models](models.md): how each default model computes its behavior.
- [Customizing models](customizing-models.md): practical extension recipes.
- [API reference](api-reference.md): package and class-level guidance.
