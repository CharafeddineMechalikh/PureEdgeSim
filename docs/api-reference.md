# API reference

This is a curated map of the public API. It explains what each package and type is for, which methods are the intended entry points, and which contracts a custom implementation must respect. The complete signature-level documentation is published as Javadoc at [javadoc.io/doc/com.mechalikh/pureedgesim](https://javadoc.io/doc/com.mechalikh/pureedgesim).

The reference is organized by task, not alphabetically. If you are looking for a specific class, the package tables point you to the right module.

## Entry points

| Class | Package | Purpose |
| --- | --- | --- |
| `Simulation` | `simulationmanager` | Parses settings, builds the scenario list, and launches one thread per scenario. |
| `SimulationAbstract` | `simulationmanager` | Holds the model class references and the custom registration setters. |
| `SimulationManager` | `simulationmanager` | Per-scenario facade that owns the task pipeline and exposes the logger, topology, and models. |
| `DefaultSimulationManager` | `simulationmanager` | Default implementation of the task pipeline and failure policy. |
| `SimulationThread` | `simulationmanager` | Runs a subset of scenarios in one thread. |

### `Simulation`

Create it, optionally register custom classes or paths, then call `launchSimulation()`.

```java
Simulation simulation = new Simulation();
simulation.setCustomSettingsFolder("/path/to/settings/");
simulation.setCustomOutputFolder("/path/to/output");
simulation.setCustomEdgeOrchestrator(MyOrchestrator.class);
simulation.launchSimulation();
```

`launchSimulation()` parses the five input files, validates the scenario parameters, builds the scenario list, and starts one `SimulationThread` in sequential mode or several in parallel mode. It does not return a result object; results are written to files and printed to the console. `getScenarios()` returns the expanded list if you need to inspect it.

The custom settings path must end with a separator because the setter concatenates file names directly. The output path does not need one.

### `SimulationAbstract`

The registration API:

| Method | Required signature of the custom class |
| --- | --- |
| `setCustomComputingNode` | `(SimulationManager, double, int, double, double)` |
| `setCustomTaskGenerator` | `(SimulationManager)` |
| `setCustomEdgeOrchestrator` | `(SimulationManager)` |
| `setCustomMobilityModel` | `(SimulationManager, Location)` |
| `setCustomNetworkModel` | `(SimulationManager)` |
| `setCustomTopologyCreator` | `(SimulationManager, ComputingNodesGenerator)` |
| `setCustomSimulationManager` | `(SimLog, PureEdgeSim, int, int, Scenario)` |
| `setCustomComputingNodesGenerator` | `(SimulationManager, Class, Class)` |
| `setCustomOutputFolder` | Path string, no trailing separator required |
| `setCustomSettingsFolder` | Path string, trailing separator required |
| `setCustomFilePath` | Absolute file path plus a `SimulationAbstract.Files` enum value |

### `SimulationManager`

The per-scenario context. Public methods:

| Method | Returns |
| --- | --- |
| `getSimulation()` | `PureEdgeSim`, the event engine |
| `getSimulationLogger()` | `SimLog` |
| `getDataCentersManager()` | `DataCentersManager` |
| `getNetworkModel()` | `NetworkModel` |
| `getScenario()` | `Scenario` |
| `getIteration()` | Scenario iteration number |
| `getSimulationId()` | Simulation identifier, used in parallel file names |
| `getFinishedTaskList()` | Tasks that finished execution |
| `getFailureRate()` | Failure rate, implementation-defined |
| `setOrchestrator`, `setNetworkModel`, `setTaskList`, `setDataCentersManager` | Wiring methods, called during initialization |

`SimulationManager` is abstract. Extend `DefaultSimulationManager` to change pipeline behavior; implement the abstract class only for a completely different control flow.

## Event engine

Package `simulationengine`.

| Class | Purpose |
| --- | --- |
| `PureEdgeSim` | Discrete-event engine: clock, event queue, entity registration, termination. |
| `SimEntity` | Base class for every entity. Provides scheduling and identity. |
| `Event` | A timestamped event with a tag and optional data object. |
| `FutureQueue<T>` | Sorted queue of events or tasks. Ordering is by time, then serial number. |
| `Queue<T>`, `QueueElement` | Minimal queue contracts. |
| `OnSimulationStartListener` | Callback interface for entities that need a start hook. |
| `OnSimulationEndListener` | Callback interface for entities that need an end hook. |

### `PureEdgeSim`

| Method | Meaning |
| --- | --- |
| `start()` | Runs the event loop until the queue is empty or `terminate()` is called. |
| `clock()` | Current simulated time in seconds. |
| `clockInMinutes()` | Current simulated time in minutes. |
| `addEntity(SimEntity)` | Registers an entity. Entities are started before the loop. |
| `getEventsQueue()` | The pending event queue, for inspection. |
| `insertFirst(Event)` | Inserts an event at the front, used internally. |
| `terminate()` | Stops the loop at the current time. |

Entities are not Java threads. They are registered objects whose `processEvent` methods are called by the single engine thread. Scheduling an event does not block.

### `SimEntity`

| Method | Meaning |
| --- | --- |
| `schedule(SimEntity target, Double delay, int tag)` | Schedules an event for `target` after `delay` seconds. |
| `schedule(SimEntity target, Double delay, int tag, Object data)` | Same, with a data payload. |
| `scheduleNow(SimEntity target, int tag)` | Schedules at the current time. |
| `scheduleNow(SimEntity target, int tag, Object data)` | Same, with data. |
| `processEvent(Event)` | Override to handle your tags. Call `super` for inherited tags. |
| `getId()` | Entity identifier. |
| `getSimulation()` | The engine. |

The scheduling methods are protected, so they are available inside subclasses, not to arbitrary callers.

### `Event`

`getTag()`, `getTime()`, `getSimEntity()`, `getData()`, and `getSerial()` are the accessors. An event orders by time first and serial number second, which makes the simulation deterministic for a fixed event insertion order.

## Scenario and configuration

Package `scenariomanager`.

| Class | Purpose |
| --- | --- |
| `SimulationParameters` | Static parsed values for all configuration fields. |
| `ParametersParser` | Reads `simulation_parameters.properties` and converts units. |
| `Scenario` | Architecture index, algorithm index, and device count for one simulation. |
| `ApplicationFileParser` | Reads `applications.xml`. |
| `EdgeDevicesParser` | Reads `edge_devices.xml`. |
| `DatacentersParser` | Reads `cloud.xml`. |
| `ComputingNodesParser` | Shared datacenter parsing logic. |
| `FileParserAbstract`, `XmlFileParser` | Parser base classes. |

`SimulationParameters` is static. Every value is parsed once per launch and shared by all scenarios. This is why two concurrent launches with different settings folders are unsafe, and why custom models should read the static fields as read-only inputs.

`Scenario` exposes `getOrchArchitecture()`, `getOrchAlgorithm()`, `getDevicesCount()`, and their string forms. The string forms are what the orchestrator sees as `architectureName` and `algorithmName`.

`ParametersParser` throws `IllegalArgumentException` when a value violates its documented range, and the message names the offending property. The unit conversions are described in [Configuration](configuration.md).

## Computing nodes and topology

Package `datacentersmanager`.

| Class | Purpose |
| --- | --- |
| `ComputingNode` | Interface for a cloud node, edge data center, edge device, router, or null node. |
| `DefaultComputingNode` | Default resources, queue, execution, and energy integration. |
| `AbstractNode` | Identity and periodic status updates. |
| `NetworkingNode` | Current links and link orientation. |
| `EnergyAwareNode` | Energy model, battery state, and death detection. |
| `LocationAwareNode` | Location updates, closest data center, and orchestrator lookup. |
| `ComputingNodeNull` | Null Object implementation. |
| `Router` | Infrastructure node used for the shared WAN model. |
| `ComputingNodesGenerator` | Abstract generator and list accessors. |
| `DefaultComputingNodesGenerator` | XML-driven node and device creation. |
| `DataCentersManager` | Holds the generator and topology. |
| `TopologyCreator` | Abstract topology builder. |
| `DefaultTopologyCreator` | Default WAN, MAN, and LAN topology. |

### `ComputingNode` contract

The interface is broad because every module consumes it. The most useful groups:

- Identity and type: `getType()`, `setType()`, `getId()`, `getName()`, `setName()`.
- Orchestration role: `isOrchestrator()`, `setAsOrchestrator(boolean)`, `getOrchestrator()`, `setOrchestrator(ComputingNode)`.
- Resources: `getMipsPerCore()`, `getTotalMipsCapacity()`, `getNumberOfCPUCores()`, `getAvailableRam()`, `getRamCapacity()`, `getAvailableStorage()`, `getTotalStorage()`.
- State: `isIdle()`, `getCurrentCpuUtilization()`, `getAvgCpuUtilization()`, `isDead()`, `getDeathTime()`, `isSensor()`, `isPeripheral()`.
- Queue: `getTasksQueue()`, `submitTask(Task)`.
- Placement: `getApplicationPlacementLocation()`, `setApplicationPlacementLocation(ComputingNode)`, `isApplicationPlaced()`, `setApplicationPlaced(boolean)`.
- Links: `getCurrentLink(LinkOrientation)`, `setCurrentLink(NetworkLink, LinkOrientation)`.
- Models: `getEnergyModel()`, `setEnergyModel(...)`, `getMobilityModel()`, `setMobilityModel(...)`.

`LinkOrientation` is an enum with `UP_LINK`, `DOWN_LINK`, and `DEVICE_TO_DEVICE`.

`ComputingNode.NULL` is a safe null object. Comparing against it is preferred over null checks, and it is used throughout the code. It is not a valid offloading destination.

### `DefaultComputingNode`

Implements the resource and execution behavior described in [Models](models.md). The protected extension points are `startExecution(Task)`, `executionFinished(Event)`, `submitTask(Task)`, and `processEvent(Event)`. If you override execution, preserve the core, RAM, and storage bookkeeping.

### `ComputingNodesGenerator`

Provides the candidate lists consumed by the orchestrator and logger:

- `getCloudOnlyList()`, `getEdgeOnlyList()`, `getMistOnlyList()`
- `getMistOnlyListSensorsExcluded()`
- `getEdgeAndCloudList()`
- `getMistAndCloudListSensorsExcluded()`
- `getMistAndEdgeListSensorsExcluded()`
- `getAllNodesList()`, `getAllNodesListSensorsExcluded()`
- `getOrchestratorsList()`

These lists are populated during generation and are read-only by convention afterwards.

## Network

Package `network`.

| Class | Purpose |
| --- | --- |
| `NetworkModel` | Abstract event handler for transfers, with shared WAN accessors. |
| `DefaultNetworkModel` | Default routing, transfer stages, registry download, and energy charging. |
| `NetworkLink` | Abstract link with bandwidth sharing and per-hop latency. |
| `NetworkLinkWanUp`, `NetworkLinkWanDown` | WAN links with WAN per-bit energy. |
| `NetworkLinkMan` | MAN links. |
| `NetworkLinkWifi`, `NetworkLinkWifiUp`, `NetworkLinkWifiDown`, `NetworkLinkWifiDeviceToDevice` | WiFi links. |
| `NetworkLinkCellular`, `NetworkLinkCellularUp`, `NetworkLinkCellularDown` | Cellular links, including base station energy. |
| `NetworkLinkEthernet` | Ethernet links. |
| `NetworkLinkNull` | Null Object link. |
| `InfrastructureGraph` | Directed multigraph and cached shortest paths. |
| `TransferProgress` | Per-transfer state and metrics. |

### `NetworkModel` constants

These are the event tags used by the pipeline:

| Constant | Value | Meaning |
| --- | --- | --- |
| `SEND_REQUEST_FROM_ORCH_TO_DESTINATION` | 1 | Send the task to its destination |
| `TRANSFER_FINISHED` | 2 | Internal link completion event |
| `DOWNLOAD_CONTAINER` | 3 | Pull a container from the registry |
| `SEND_REQUEST_FROM_DEVICE_TO_ORCH` | 4 | Send the request to the orchestrator |
| `SEND_RESULT_TO_ORCH` | 6 | Send results from destination to orchestrator |
| `SEND_RESULT_FROM_ORCH_TO_DEV` | 7 | Send results from orchestrator to device |

`getWanUpUtilization()` and `getWanDownUtilization()` return the used bandwidth of the shared WAN links. They throw `IllegalArgumentException` unless `use_one_shared_wan_link=true`.

### `NetworkLink`

Key methods:

- `addTransfer(TransferProgress)` registers a transfer and starts the progress loop.
- `getUsedBandwidth()` returns the bandwidth in use, capped at the link bandwidth.
- `getTotalTransferredData()` returns the total bits registered on the link.
- `getEnergyModel()` returns the per-bit energy model.
- `setBandwidth(double)`, `setLatency(double)` configure the link.
- `getType()` returns `WAN`, `MAN`, `LAN`, or `IGNORE`.

The link schedules `UPDATE_PROGRESS` every `network_update_interval` while transfers exist. Do not schedule it yourself unless you replace the progress mechanism.

### `TransferProgress`

Holds the task, the total and remaining file size in bits, the current and average allocated bandwidth, the transfer type, and the LAN, MAN, and WAN usage accumulated on each hop. `Type` is `TASK`, `CONTAINER`, `RESULTS_TO_DEV`, `RESULTS_TO_ORCH`, or `REQUEST`.

## Energy

Package `energy`.

| Class | Purpose |
| --- | --- |
| `EnergyModelComputingNode` | Linear CPU power model, battery, and wireless energy. |
| `EnergyModelComputingNodeNull` | Null Object. |
| `EnergyModelNetworkLink` | Per-bit energy for wired links. |
| `EnergyModelNetworkLinkNull` | Null Object. |

`EnergyModelComputingNode` exposes `updateStaticEnergyConsumption()`, `updateDynamicEnergyConsumption(length, mipsCapacity)`, `updatewirelessEnergyConsumption(bits, flag)`, `getTotalEnergyConsumption()`, `getCpuEnergyConsumption()`, `getBatteryLevelWattHour()`, `getBatteryLevelPercentage()`, and the setters for capacity, battery state, initial level, and connectivity.

`EnergyModelNetworkLink` exposes `getCurrentEnergyConsumption()`, `getTotalEnergyConsumption()`, `getEnergyPerBit()`, and `getLink()`.

The flag inversion in `updatewirelessEnergyConsumption` is documented in [Models](models.md). Read it before relying on transmit versus receive values.

## Tasks

Package `taskgenerator`.

| Class | Purpose |
| --- | --- |
| `Task` | Task interface. |
| `TaskAbstract` | Base implementation of the timing and identity fields. |
| `DefaultTask` | Default task. |
| `Application` | Parsed application definition. |
| `TaskGenerator` | Abstract generator and task list. |
| `DefaultTaskGenerator` | Default traffic model. |

`Task` exposes the request, result, and container sizes in bits, the length in million instructions, the maximum latency, the generating device, the orchestrator, the registry, the offloading destination, the application identifier, the type, the failure reason, the status, and the timing fields.

Timing accessors:

- `getTime()` is the scheduled generation time.
- `setArrivalTime(double)` sets arrival and also initializes the execution start time.
- `setExecutionStartTime(double)` sets start and finish time.
- `setExecutionFinishTime(double)` sets the finish time.
- `getWatingTime()` returns `execStartTime - arrivalTime`.
- `getActualCpuTime()` returns the execution duration.
- `getTotalDelay()` returns network time plus waiting time plus CPU time.

`FailureReason` values include `NO_OFFLOADING_DESTINATIONS`, `INSUFFICIENT_RESOURCES`, `FAILED_BECAUSE_DEVICE_DEAD`, `FAILED_DUE_TO_DEVICE_MOBILITY`, and `FAILED_DUE_TO_LATENCY`.

## Orchestration

Package `taskorchestrator`.

| Class | Purpose |
| --- | --- |
| `Orchestrator` | Abstract decision policy and candidate lists. |
| `DefaultOrchestrator` | `TRADE_OFF` and `ROUND_ROBIN`. |

`Orchestrator` provides:

- `orchestrate(Task)`, the entry point called by the simulation manager.
- `findComputingNode(String[], Task)`, the abstract decision method.
- `resultsReturned(Task)`, the abstract completion callback.
- `offloadingIsPossible(Task, ComputingNode, String[])`, the eligibility helper.
- `sameLocation(ComputingNode, ComputingNode, int)`, the range check.
- `arrayContains(String[], String)`, the layer-membership helper.
- `checkComputingNode(ComputingNode)`, the sensor guard.

Protected fields: `nodeList`, `simulationManager`, `simLog`, `algorithmName`, `architectureName`, `architectureLayers`.

`DefaultOrchestrator` adds the `historyMap` load counters and the protected `tradeOff(...)` and `roundRobin(...)` methods. Its `resultsReturned` is intentionally empty.

## Logging and visualization

Package `simulationmanager`:

| Class | Purpose |
| --- | --- |
| `SimLog` | CSV, text report, console output, and metric counters. |
| `ChartsGenerator` | Aggregate charts from a sequential CSV. |
| `SimulationVisualizer` | Live Swing charts and per-iteration snapshots. |

Package `simulationvisualizer`: individual chart classes (`MapChart`, `CpuUtilizationChart`, `TasksSuccessRateChart`, `NetworkUsageChart`, `SimulationVisualizer`).

`SimLog` is created per scenario. It writes the CSV row when `showIterationResults` is called and appends to the output file. `getFileName(extension)` returns the correct path for the current mode. `deepLog` writes only when `deep_log_enabled=true`.

## Geometry and mobility

Package `locationmanager`.

| Class | Purpose |
| --- | --- |
| `Location` | Immutable x and y coordinates with distance helpers. |
| `MobilityModel` | Abstract path model and closest data center lookup. |
| `DefaultMobilityModel` | Random-waypoint model. |
| `MobilityModelNull` | Null Object. |

`Location` is the type passed to mobility model constructors. `MobilityModel` exposes `generatePath()`, `updateLocation(double)`, `getCurrentLocation()`, `getClosestEdgeDataCenter()`, `isMobile()`, and the speed and duration setters. The abstract method is `getNextLocation(Location)`.

## Contracts and invariants

These are the invariants that hold across the public API. Custom implementations should preserve them.

1. **Single-threaded event loop.** One engine thread processes all events for a scenario. Scheduling is the only way to defer work.
2. **Time is seconds.** All public timing methods use seconds. Configuration files use minutes or seconds as documented per parameter.
3. **Bits and watt-hours inside the simulator.** Sizes are bits and energy is watt-hours. Configuration units are converted during parsing.
4. **Failures are values, not exceptions.** A task with no destination uses a `FailureReason`, not an exception. Exceptions abort a scenario; use them only for programming errors.
5. **Null Objects instead of null.** `ComputingNode.NULL`, `NetworkLink.NULL`, `MobilityModel.NULL`, and the energy Null Objects are safe to call. Do not assign `null` to these fields.
6. **Per-scenario state is instance state.** Models are created fresh for each scenario. Static mutable state crosses scenario boundaries and breaks parallel runs.
7. **The event tags of a subclass start above the base range.** Use a distinct base such as `12000` for custom tags.
8. **Lists from the generator are read-only by convention after initialization.** Changing them during the run invalidates caches and candidate lists.
9. **A task that is submitted must eventually finish or fail.** The pipeline assumes no silent drops.
10. **The CSV writer appends one row per scenario; the header is written once per file.** Keep that assumption if you extend the logger.

## Where the Javadoc fits

The Javadoc contains the full list of methods, parameters, returns, and `@see` references for every public class. This page gives the map and the contracts; the Javadoc gives the signatures. When the two disagree, the source code is authoritative; please open an issue so the documentation can be corrected.

## Version notes

This documentation describes PureEdgeSim 5.5. The latest published Maven Central artifact may lag the repository. Check the version in `pom.xml` and the README before copying dependency snippets.

## Where to go next

- Start running: [Getting started](getting-started.md).
- Understand internals: [Architecture](architecture.md).
- Extend a model: [Customizing models](customizing-models.md).
- Design an experiment: [Research guide](research-guide.md).
