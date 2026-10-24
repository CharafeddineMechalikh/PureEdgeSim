# Configuration

PureEdgeSim is configured through five files. This guide documents every parameter, its unit, its default value, and its effect on the simulation. It also explains the validation rules, the scenario sweep, and the topology format.

All default files are under `PureEdgeSim/settings/`. You can place them anywhere and call `sim.setCustomSettingsFolder("path/with/trailing/slash/")`, or configure files individually with `sim.setCustomFilePath(path, Simulation.Files.X)`.

The five files are:

| File | Purpose |
| --- | --- |
| `simulation_parameters.properties` | Global behavior: duration, sweep, network, registry, orchestrators, logging, charts |
| `applications.xml` | Workload classes: task length, sizes, latency bound, generation rate, share of devices |
| `cloud.xml` | Number and capacity of cloud data centers |
| `edge_datacenters.xml` | Edge data center capacities, locations, and MAN links between them |
| `edge_devices.xml` | Device types: hardware, battery, mobility, connectivity, task generation |

Parsing happens before any simulation starts. Invalid values raise `IllegalArgumentException` with the parameter name and file. The console prints one line per successfully parsed file.

## Simulation parameters

The values below are the defaults shipped with the repository. Constraints are enforced by `ParametersParser`.

### Time and execution

| Parameter | Default | Unit | Constraint | Effect |
| --- | --- | --- | --- | --- |
| `simulation_time` | `10` | minutes | `> 0` | Converted to seconds internally. The manager schedules its final log at this time. With `wait_for_all_tasks=true`, the simulation may run longer until every task is accounted for. |
| `parallel_simulation` | `false` | boolean | | Runs independent scenarios in parallel using `min(CPU cores, scenario count)` workers. Disables live charts and changes output file names. |
| `update_interval` | `1` | seconds | `>= 0.01` | Period of the mobility update and the static energy accounting. Lower values improve accuracy and increase simulation time. |
| `pause_length` | `3` | seconds (wall clock) | `>= 0` | Sleep between sequential iterations, only to make console output readable. It does not affect simulated time. |
| `batch_size` | `100` | tasks | `> 0` | Number of generated tasks scheduled into the event queue at once. Larger batches reduce queue overhead but increase memory. |

### Scenario sweep

| Parameter | Default | Unit | Constraint | Effect |
| --- | --- | --- | --- | --- |
| `min_number_of_edge_devices` | `100` | devices | `> 0` | First device count in the sweep. |
| `max_number_of_edge_devices` | `300` | devices | `> 0`, `>= min` | Last device count in the sweep, inclusive. |
| `edge_device_counter_size` | `100` | devices | `> 0` | Increment between device counts. |
| `orchestration_architectures` | all seven | comma-separated | Exact names | Architectures to evaluate. See the list below. |
| `orchestration_algorithms` | `TRADE_OFF,ROUND_ROBIN` | comma-separated | Names understood by the orchestrator | Algorithms to evaluate. A custom orchestrator may accept other names. |

The sweep is the Cartesian product: architectures, then algorithms, then device counts. The default configuration therefore produces `7 * 2 * 3 = 42` iterations.

Architecture names:

- `CLOUD_ONLY`
- `EDGE_ONLY`
- `MIST_ONLY`
- `MIST_AND_CLOUD`
- `EDGE_AND_CLOUD`
- `MIST_AND_EDGE`
- `ALL`

Names are case-sensitive. An unknown architecture leaves `architectureLayers` empty in `Orchestrator.initialize()`, and every task fails with no destination.

### Map and range

| Parameter | Default | Unit | Constraint | Effect |
| --- | --- | --- | --- | --- |
| `length` | `200` | meters | `> 0` | Map length on the y-axis. Devices are placed randomly within the map. |
| `width` | `200` | meters | `> 0` | Map width on the x-axis. |
| `edge_devices_range` | `10` | meters | `> 0` | Maximum distance for device-to-device (mist) communication and for mist eligibility. Increased mobility failures when devices move apart. |
| `edge_datacenters_coverage` | `200` | meters | `> 0` | Maximum distance between a device and its edge data center. The default topology connects each device to the closest edge data center within this radius. |

If you enlarge the map, update the edge data center locations in `edge_datacenters.xml`. If `edge_datacenters_coverage` is too small, some devices have no edge server and edge-only scenarios fail with no destination.

### Application registry

| Parameter | Default | Constraint | Effect |
| --- | --- | --- | --- |
| `enable_registry` | `false` | boolean | When true, the destination may need to download the application container before executing a task. |
| `registry_mode` | `CLOUD` | `CLOUD` or `CACHE` | With the default network model, `CLOUD` downloads the container from the first cloud node when the destination is not a cloud node. `CACHE` is recognized only by `Example7CustomNetworkModel`; with the default network model it performs no download. The value has no effect when `enable_registry=false`. |

The registry location itself is the first cloud node, assigned to every task during generation. A custom network model can override `getRegistry` behavior by setting a different node on each task.

### Orchestrators

| Parameter | Default | Constraint | Effect |
| --- | --- | --- | --- |
| `enable_orchestrators` | `false` | boolean | When false, each device is its own orchestrator and makes local decisions. When true, requests are sent to a remote orchestrator before the decision. |
| `deploy_orchestrator` | `CLOUD` | `CLOUD`, `EDGE`, `MIST`, or another value | Placement of the orchestrator. `CLOUD` uses the first cloud data center, `EDGE` uses the closest edge data center, `MIST` uses the device itself, and any other value selects the reachable node in `orchestratorsList` with the lowest topology delay. Individual nodes can also be marked with `<isOrchestrator>true</isOrchestrator>`. |

Setting `deploy_orchestrator=EDGE` does not by itself deploy an orchestrator on every edge server. The generator adds edge data centers to `orchestratorsList` only when `enable_orchestrators=true` and the value is `EDGE`; devices then select their closest edge data center as orchestrator. The `isOrchestrator` flag in the XML adds a specific node to the list regardless of the deployment mode.

### Completion and logging

| Parameter | Default | Constraint | Effect |
| --- | --- | --- | --- |
| `wait_for_all_tasks` | `true` | boolean | If true, the simulation continues past `simulation_time` until every generated task has succeeded or failed. If false, it stops at the configured time and tasks still in flight are unresolved. |
| `save_log_file` | `true` | boolean | Writes the per-iteration text report in addition to the CSV. The CSV is always written. |
| `clear_output_folder` | `false` | boolean | Deletes the contents of the output folder at the start of the first iteration. Use with care, because the folder may contain results from previous experiments. |
| `deep_log_enabled` | `false` | boolean | Enables very verbose per-event logging through `SimLog.deepLog`. Extremely slow for large scenarios. |

### Visualization

| Parameter | Default | Constraint | Effect |
| --- | --- | --- | --- |
| `display_real_time_charts` | `true` | boolean | Opens the live Swing charts. Disabled automatically in parallel mode. |
| `auto_close_real_time_charts` | `true` | boolean | Hides the chart window when the iteration ends. |
| `charts_update_interval` | `30` | simulated seconds | `0.01` to `60` | How often live charts refresh. Refreshing more often costs CPU. |
| `save_charts` | `true` | boolean | Saves chart images. With live charts enabled, per-iteration images are saved. After sequential runs, aggregate comparison charts are generated from the CSV under `Final results/`. No charts are produced in parallel mode. |

### Network model

| Parameter | Default | Constraint | Effect |
| --- | --- | --- | --- |
| `realistic_network_model` | `false` | boolean | When false, each transfer completes in a single network update, which is fast but coarse. When true, remaining file size decreases according to bandwidth during each `network_update_interval`, which models progressive transfers and contention. |
| `network_update_interval` | `1` | seconds | `>= 0.001` | Period at which links update transfer progress in the realistic model. Smaller values improve accuracy and increase event count. |
| `one_shared_wan_network` | `true` | boolean | When true, all cloud traffic shares one WAN uplink and downlink, so cloud transfers compete for the same bandwidth. The WAN chart is shown only in this mode. When false, devices connect to the cloud through separate paths. |

Bandwidths are in **megabits per second** and are multiplied by `1,000,000` during parsing. Latencies are in **seconds**. Energy values are in **nanojoules per bit** and are converted to watt-hours per bit by multiplying by `2.7777777777778e-13`. The cellular base station downlink uses `12.7777777777778e-13`, matching the larger energy cost of the downlink in the original network data.

| Parameter | Default | Unit | Notes |
| --- | --- | --- | --- |
| `wan_bandwidth` | `20` | Mbps | Shared cloud link if `one_shared_wan_network=true`. Low values cause latency failures for cloud tasks. |
| `wan_latency` | `0.06` | s | Per-link propagation delay used in routing and delay accounting. |
| `wan_nanojoules_per_bit` | `46.7` | nJ/bit | WAN link energy. |
| `man_bandwidth` | `1000` | Mbps | Metropolitan area links between edge data centers. |
| `man_latency` | `0.01` | s | MAN propagation delay. |
| `man_nanojoules_per_bit` | `0` | nJ/bit | MAN energy, disabled by default. |
| `wifi_bandwidth` | `1300` | Mbps | Wi-Fi access link. |
| `wifi_latency` | `0.005` | s | Wi-Fi propagation delay. |
| `wifi_device_transmission_nanojoules_per_bit` | `283.17` | nJ/bit | Energy for a device transmitting. |
| `wifi_device_reception_nanojoules_per_bit` | `137.01` | nJ/bit | Energy for a device receiving. |
| `wifi_access_point_transmission_nanojoules_per_bit` | `23.8` | nJ/bit | Energy for the access point transmitting. |
| `wifi_access_point_reception_nanojoules_per_bit` | `23.8` | nJ/bit | Energy for the access point receiving. |
| `ethernet_bandwidth` | `1000` | Mbps | Wired link. |
| `ethernet_latency` | `0.002` | s | Ethernet propagation delay. |
| `ethernet_nanojoules_per_bit` | `40000` | nJ/bit | Ethernet energy. |
| `cellular_bandwidth` | `100` | Mbps | Cellular link. |
| `cellular_latency` | `0.03` | s | Cellular propagation delay. |
| `cellular_device_transmission_nanojoules_per_bit` | `438.4` | nJ/bit | Device uplink energy. |
| `cellular_device_reception_nanojoules_per_bit` | `51.97` | nJ/bit | Device downlink energy. |
| `cellular_base_station_nanojoules_per_bit_up_link` | `6200` | nJ/bit | Base station uplink energy. |
| `cellular_base_station_nanojoules_per_bit_down_link` | `20500` | nJ/bit | Base station downlink energy. |

## Applications

Each `<application>` describes one workload class. The `name` attribute is an identifier, and `type` is free text that the orchestrator can use to distinguish workloads.

```xml
<application name="Augmented reality">
    <type>Soft Real-time</type>
    <rate>30</rate>
    <usage_percentage>30</usage_percentage>
    <latency>0.5</latency>
    <container_size>1500</container_size>
    <request_size>1500</request_size>
    <results_size>50</results_size>
    <task_length>5000</task_length>
</application>
```

| Element | Unit | Constraint | Meaning |
| --- | --- | --- | --- |
| `rate` | tasks per minute | `> 0` | Number of tasks each assigned device generates per simulated minute. |
| `usage_percentage` | percent | `> 0` | Share of task-generating devices assigned to this application. |
| `latency` | seconds | `> 0` | Maximum acceptable total delay. Tasks whose total delay reaches this value fail at the final check. |
| `container_size` | kilobytes | `> 0` | Application container size. Used for RAM and storage checks and for registry downloads. Converted to bits with `* 8000`. |
| `request_size` | kilobytes | `> 0` | Offloading request size, converted to bits. |
| `results_size` | kilobytes | `> 0` | Result size, converted to bits. |
| `task_length` | million instructions (MI) | `> 0` | Computational demand used to compute execution time. |

### How applications are assigned to devices

`DefaultTaskGenerator` iterates over applications in file order, except the last one. For each, it computes `usage_percentage * generatingDevices / 100`, removes that many devices at random from the pool, assigns them the application, and generates their tasks. The last application receives **all remaining devices**, regardless of its configured percentage.

Consequences:

- The percentages of all applications except the last must sum to at most 100. If they exceed 100, the device pool can be exhausted and generation fails.
- The last application's realized share is `100 - sum(previous percentages)`.
- To model exact shares, arrange the file so that the last application is the one whose remainder you intend, or write a custom task generator.
- Device selection is random, so the realized counts can differ slightly from the requested counts due to truncation.

## Cloud data centers

`cloud.xml` contains one or more `<datacenter>` entries. The default file defines a single cloud data center.

```xml
<datacenter>
    <idleConsumption>0</idleConsumption>
    <maxConsumption>5776</maxConsumption>
    <isOrchestrator>false</isOrchestrator>
    <cores>200</cores>
    <mips>40000</mips>
    <ram>16000</ram>
    <storage>1000000</storage>
</datacenter>
```

| Element | Unit | Meaning |
| --- | --- | --- |
| `idleConsumption` | watts | Static power draw when the node is idle. |
| `maxConsumption` | watts | Power draw at 100 percent CPU utilization. |
| `isOrchestrator` | boolean | Adds this node to the orchestrator list. |
| `cores` | count | Number of CPU cores. Each core executes at most one task at a time. |
| `mips` | MIPS per core | Computing rate of one core. Total capacity is `mips * cores`. |
| `ram` | megabytes | RAM available for task containers. |
| `storage` | megabytes | Storage available for application containers. |

A node is treated as a sensor when `mips <= 0`, `cores <= 0`, or `storage <= 0`. Sensors are excluded from execution destinations.

## Edge data centers

`edge_datacenters.xml` extends the cloud syntax with a `name`, a `periphery` flag, a location, and optional MAN links.

```xml
<datacenter name="dc1">
    <periphery>true</periphery>
    <idleConsumption>100</idleConsumption>
    <maxConsumption>250</maxConsumption>
    <isOrchestrator>false</isOrchestrator>
    <location>
        <x_pos>100</x_pos>
        <y_pos>100</y_pos>
    </location>
    <cores>10</cores>
    <mips>40000</mips>
    <ram>16000</ram>
    <storage>200000</storage>
</datacenter>
<network_links>
    <link>
        <from>dc1</from>
        <to>dc2</to>
    </link>
</network_links>
```

| Element | Unit | Meaning |
| --- | --- | --- |
| `name` | string | Unique identifier used by `<network_links>`. |
| `periphery` | boolean | Metadata indicating a peripheral data center. The default models store it but do not use it; custom topology or orchestration models can. |
| `location.x_pos`, `location.y_pos` | meters | Position on the map. Two edge data centers cannot share the same location. |
| `network_links.link.from`, `to` | names | Creates a bidirectional MAN connection between the two named edge data centers. The default topology also creates a path from each edge data center to the cloud through the topology graph. |

The default topology:

1. Creates a WAN router node.
2. Connects every edge device to the router with a WAN link, which provides cloud connectivity.
3. Connects the first edge data center to the router with a WAN link.
4. Connects each edge device to the closest edge data center within `edge_datacenters_coverage` using a LAN link.
5. Computes and caches shortest paths between all edge and cloud nodes.

If a device is not within range of any edge data center, its edge uplink remains `ComputingNode.NULL`. The device can still reach the cloud, but edge-only architectures will have no valid destination for its tasks.

## Edge devices

`edge_devices.xml` defines device **types**. The number of actual devices of each type is derived from the scenario's device count and the type's percentage.

```xml
<device arch="x86" os="linux" vmm="xen">
    <connectivity>wifi</connectivity>
    <mobility>true</mobility>
    <speed>1.8</speed>
    <minPauseDuration>100</minPauseDuration>
    <maxPauseDuration>400</maxPauseDuration>
    <minMobilityDuration>60</minMobilityDuration>
    <maxMobilityDuration>100</maxMobilityDuration>
    <battery>true</battery>
    <percentage>18</percentage>
    <batteryCapacity>18.75</batteryCapacity>
    <initialBatteryLevel>100</initialBatteryLevel>
    <idleConsumption>0.2</idleConsumption>
    <maxConsumption>5</maxConsumption>
    <isOrchestrator>false</isOrchestrator>
    <generateTasks>true</generateTasks>
    <cores>8</cores>
    <mips>25000</mips>
    <ram>4000</ram>
    <storage>256</storage>
</device>
```

| Element | Unit | Meaning |
| --- | --- | --- |
| `arch`, `os`, `vmm` | strings | Informational metadata. The default models do not use them. |
| `connectivity` | `wifi`, `cellular`, `ethernet` | Selects the link type used to connect the device to its edge data center. |
| `mobility` | boolean | Whether the device moves. |
| `speed` | meters per second | Movement speed. `1.4` m/s is about 5 km/h. |
| `minPauseDuration`, `maxPauseDuration` | seconds | Range of the pause before each movement. |
| `minMobilityDuration`, `maxMobilityDuration` | seconds | Range of the movement duration before the next pause. |
| `battery` | boolean | Whether the device runs on battery. Devices without batteries never die from energy depletion. |
| `percentage` | percent | Share of this device type. The percentages of all types must sum exactly to 100. |
| `batteryCapacity` | watt-hours | Total battery capacity. |
| `initialBatteryLevel` | percent | Initial charge, between 0 and 100. |
| `idleConsumption` | watts | Idle power draw. |
| `maxConsumption` | watts | Power draw at full CPU utilization. |
| `isOrchestrator` | boolean | Adds the device to the orchestrator list. |
| `generateTasks` | boolean | Whether devices of this type generate workload. |
| `cores` | count | CPU cores. Set to 0 for a pure sensor. |
| `mips` | MIPS per core | Per-core computing rate. |
| `ram` | megabytes | RAM available for task containers. |
| `storage` | megabytes | Storage available for application containers and for caching in the examples. |

Device types are instantiated in file order. If the requested device count does not divide evenly by the percentages, PureEdgeSim fills the missing devices with the last type. This keeps the total exactly equal to the scenario count.

## Worked configuration examples

### Evaluate one architecture with a single device count

```properties
simulation_time=10
min_number_of_edge_devices=100
max_number_of_edge_devices=100
edge_device_counter_size=100
orchestration_architectures=ALL
orchestration_algorithms=TRADE_OFF
```

This produces one iteration. It is the best way to debug a custom model.

### Compare cloud and mist for latency-sensitive work

```properties
orchestration_architectures=CLOUD_ONLY,MIST_ONLY
orchestration_algorithms=TRADE_OFF
min_number_of_edge_devices=100
max_number_of_edge_devices=100
edge_device_counter_size=100
edge_devices_range=50
```

The cloud has high WAN latency but ample capacity. Mist has low latency but limited capacity and mobility failures. Increase `edge_devices_range` if tasks fail because devices cannot reach each other.

### Run a large sweep in parallel

```properties
parallel_simulation=true
min_number_of_edge_devices=100
max_number_of_edge_devices=1000
edge_device_counter_size=100
```

Live charts are disabled. Results are written as `Parallel_simulation_<id>.csv`. Memory is the limiting factor: each iteration creates its own object graph, so start with a device count your machine can handle.

### Model a low-bandwidth WAN

```properties
wan_bandwidth=5
wan_latency=0.1
one_shared_wan_network=true
```

Cloud tasks may fail due to latency. This is often the intended experiment. Verify that failures are reported as `Tasks failed (delay)` and not as `NO_OFFLOADING_DESTINATIONS`.

## Validation and common mistakes

- **Paths are relative to the working directory.** Running from an IDE and from Maven can produce different working directories. Use absolute paths in scripts.
- **The settings folder must contain all five files.** `setCustomSettingsFolder` does not copy defaults; it points to your folder.
- **Architecture and algorithm names are case-sensitive.** `cloud_only` is not `CLOUD_ONLY`.
- **Device percentages must sum to 100.** The parser rejects other sums.
- **The application usage percentages have a remainder rule.** See the application section above.
- **The task generator removes non-generating devices.** If every device has `generateTasks=false`, the task list is empty and the simulation produces no meaningful results.
- **Sensors are excluded from multicast categories.** `mistOnlyList` includes them, while `mistOnlyListSensorsExcluded` does not. The default orchestrator uses the latter.
- **Changing the map requires updating edge data center coordinates.** A data center outside the map is still reachable in the graph, but the geometry no longer reflects your intended scenario.

## Related reading

- [Simulation models](models.md): how the configured values are used by the energy, mobility, and network models.
- [Output and metrics](output-and-metrics.md): where the configured output options send their files.
- [Research guide](research-guide.md): choosing parameter values for a credible study.
