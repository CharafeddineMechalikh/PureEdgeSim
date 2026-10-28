---
description: "Reference for the five PureEdgeSim settings files: parameters, scenario sweep, applications, cloud and edge data centers, devices, and distributions."
---
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

Parsing happens before any simulation starts. A file that cannot be read, a missing or misspelled property, or a value of the wrong type stops the run with a single console line naming the file, the property or element, and the found value, instead of a stack trace. A property the parser never reads prints a warning that suggests the closest known name, and the run continues. Boolean properties accept only `true` and `false`. The console prints one line per successfully parsed file.

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
| `random_seed` | empty | whole number | | Makes every random value reproducible. When empty, each run draws its own values. See [Randomness and reproducibility](#randomness-and-reproducibility). |

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

## Probability distributions in the XML files

Any numeric element of `applications.xml`, `edge_devices.xml`, `cloud.xml`, and `edge_datacenters.xml` may hold a probability distribution instead of a fixed value. The element keeps its name and its unit; the distribution is declared with attributes:

```xml
<request_size distribution="lognormal" mean="25" std="10"/>
<mips distribution="normal" mean="25000" std="3000" min="5000"/>
<ram distribution="uniform" min="2048" max="8192"/>
```

A plain number means the same thing as `distribution="constant"`, so every scenario written before PureEdgeSim 5.7 keeps working unchanged.

Two attributes are understood for every distribution: `min` and `max` bound the generated values. For `uniform` and `triangular` they define the support; for any other distribution the sample is drawn again until it falls inside the range, and clamped after one hundred attempts, so a too narrow range quietly becomes a ceiling. The other attributes depend on the distribution and are listed in [Simulation models](models.md#probability-distributions).

What varies depends on where the distribution is written:

- in `applications.xml`, each **task** draws its own size, length, latency requirement, and arrival gap;
- in `edge_devices.xml`, each device **instance** of the type draws its own capacity, battery, speed, and position, which is how a heterogeneous fleet is modelled;
- in `cloud.xml` and `edge_datacenters.xml`, each `<datacenter>` entry draws its own values.

The mean of the distribution is the value used by the validation, the CSV columns, and the charts. That is what keeps a scenario with variable parameters comparable to the scenario with fixed ones, and it is also why a skewed distribution (lognormal, Pareto) reports a mean that is larger than its most likely value.

## Randomness and reproducibility

`random_seed` is the switch between a stochastic run and a reproducible one.

```properties
random_seed=42
```

With a seed, every random value in the simulation, task times and sizes, node capacities, device positions, and mobility choices, comes from a stream derived from that number. Relaunching the same scenario gives the same CSV row, which is what you want when you debug a configuration or when a reviewer asks for your exact setup. Without a seed, the seed key left empty or absent, the simulator uses a secure random source and every run differs, as in earlier versions.

Two practical rules:

- A seed freezes one sample of the process. To report an average over repetitions, use a different seed per repetition (vary it yourself, in the sweep or in the file) rather than the same one, and state the number of repetitions and the seeds in your methodology.
- Parallel sweeps reuse the same properties file, so all scenarios of one run share the seed. That makes the workload identical across orchestration algorithms, which is usually what a comparison needs.

The same source is available programmatically through `com.mechalikh.pureedgesim.distribution.Randomness`: `setSeed(long)` before launching the simulation, and `beginScenario(long)` when you build your own multi-threaded sweep and need one reproducible stream per scenario.

## Applications

Each `<application>` describes one workload class. The `name` attribute is an identifier, and `type` is free text that the orchestrator can use to distinguish workloads. The framework does not enforce a closed set of application types: a workload is defined by its numbers (rate, latency bound, sizes, and task length), and `type` only labels it. The shipped `PureEdgeSim/settings/applications.xml` uses three latency classes, `Hard Real-time`, `Soft Real-time`, and `Non Real-time`, while some examples add `Normal` or a workload name such as `Video analytics`. Orchestrators read the label with `task.getType()` and store it as a string, so any wording works.

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
| `rate` | requests per minute | `> 0` | Number of requests each assigned device generates per simulated minute. A monolithic application produces one task per request; a service graph produces one task per subtask. |
| `usage_percentage` | percent | `> 0` | Share of task-generating devices assigned to this application. |
| `latency` | seconds | `> 0` | Maximum acceptable end-to-end delay of a request. It is drawn once per request and shared by all of its subtasks; a request whose last result returns at or after this deadline fails. For a monolithic application the request and the task are the same thing. |
| `container_size` | kilobytes | `> 0` | Application container size. Used for RAM and storage checks and for registry downloads. Converted to bits with `* 8000`. |
| `request_size` | kilobytes | `> 0` | Offloading request size, converted to bits. |
| `results_size` | kilobytes | `> 0` | Result size, converted to bits. |
| `task_length` | million instructions (MI) | `> 0` | Computational demand used to compute execution time. |
| `arrival` | seconds, or a keyword | `> 0` | Optional. The time between two consecutive tasks of one device. `poisson` (or `exponential`) draws gaps with a mean of `60/rate`, `uniform` draws them between 0 and twice that mean, and a distribution element gives the exact inter-arrival law. Omit it, or write `constant`, to keep the evenly spaced bursts of earlier versions. When it is set, `rate` only supplies the mean gap: tasks are no longer counted per minute. |

Every element above may be a distribution, in which case each task draws its own value:

```xml
<application name="Video analytics">
    <type>Soft Real-time</type>
    <rate distribution="poisson" mean="30"/>
    <arrival>poisson</arrival>
    <usage_percentage>30</usage_percentage>
    <latency distribution="normal" mean="0.5" std="0.1" min="0.05"/>
    <container_size distribution="uniform" min="1000" max="2000"/>
    <request_size distribution="lognormal" mean="1500" std="400"/>
    <results_size>50</results_size>
    <task_length distribution="gamma" shape="4" scale="1250"/>
</application>
```

No configuration element replays a measured workload. The elements above describe a
distribution, from which each task is drawn independently, so a scenario reproduces
the average and the spread of a traffic but not the shape of its bursts. When the
traffic exists as data, replace the generator instead:
[Example 25](../PureEdgeSim/examples/Example25/README.md) reads a recorded trace of
8819 production LLM inference requests, one task per line, and on a server sized to
the mean demand of that hour it turns the 173 seconds of queueing measured with
poisson arrivals into 120201. The values in `applications.xml` then serve as defaults
for what a trace does not carry, such as the container size.

### How applications are assigned to devices

`DefaultTaskGenerator` iterates over applications in file order, except the last one. For each, it computes `usage_percentage * generatingDevices / 100`, removes that many devices at random from the pool, assigns them the application, and generates their tasks. The last application receives **all remaining devices**, regardless of its configured percentage.

Consequences:

- The percentages of all applications except the last must sum to at most 100. If they exceed 100 the device pool runs out first and the remaining applications simply get no device.
- The last application's realized share is `100 - sum(previous percentages)`.
- To model exact shares, arrange the file so that the last application is the one whose remainder you intend, or write a custom task generator.
- Device selection is random, so the realized counts can differ slightly from the requested counts due to truncation.
- With a distribution for `rate`, the number of tasks generated in a minute varies from one minute to the next while the mean stays the configured value.

### Service graphs

A `task` is a single independent job by default. An application may instead describe a **service graph**: several subtasks that depend on each other, declared with `<subtask>` elements inside the application. The attributes carry the graph, so the child elements of `<application>` keep their usual meaning.

The element supports any acyclic shape. The four common ones are monolithic (no `<subtask>`, one task per request), sequential (a chain of `depends_on`), parallel (a root that fans out, and a join that lists several parents), and complex (mixed fan-out, fan-in, and several roots). Diagrams and their runtime consequences are in [Simulation models](models.md#task-graph-shapes).

```xml
<application name="Video analytics">
    <type>Soft Real-time</type>
    <rate>10</rate>
    <usage_percentage>40</usage_percentage>
    <latency>0.5</latency>
    <container_size>1500</container_size>
    <request_size>1500</request_size>
    <results_size>50</results_size>
    <task_length>5000</task_length>
    <subtask name="capture" task_length="500"/>
    <subtask name="detect" depends_on="capture" task_length="5000"/>
    <subtask name="track" depends_on="detect" task_length="2000"/>
    <subtask name="alert" depends_on="detect" request_size="10" results_size="5"/>
</application>
```

| Attribute | Required | Meaning |
| --- | --- | --- |
| `name` | yes | Identifier of the subtask inside the application. Unique, and referenced by `depends_on`. |
| `depends_on` | no | Comma-separated names of the subtasks whose results this one needs. Omit it for a root, which is submitted as soon as the request arrives. |
| `task_length` | no | Million instructions. Overrides the application value. |
| `request_size` | no | Kilobytes. Overrides the application value. |
| `results_size` | no | Kilobytes. Overrides the application value. |
| `container_size` | no | Kilobytes. Overrides the application value. |
| `latency` | no | Seconds. Overrides the application value for this subtask. The application value remains the end-to-end deadline of the request. |
| `optional` | no | `true` or `false` (default `false`). An optional subtask is skipped, not failed, when one of its dependencies fails. It must have at least one dependency. |

An omitted attribute inherits the application value, and the application elements remain mandatory. Sizes written on a subtask are converted to bits exactly like the application sizes. Every `<subtask>` is a distribution-friendly value: only plain numbers are accepted here, because a subtask element has no child element to hold a distribution.

How a graph runs:

- One request from a device produces one task per subtask, so `rate` counts requests, not tasks. The `Number of generated tasks` column includes every subtask.
- Only the roots enter the simulation at the request time. A subtask is submitted when the last of its `depends_on` parents has returned its results, at the time those results reach the device. The results of each parent are added to the request size of the child before it is offloaded, so the data of a dependency is actually carried over the network.
- The application `latency` is the end-to-end deadline of the request: it is drawn once, and every subtask of the request carries the same absolute deadline. A subtask whose own `latency` bound is exceeded fails at its return, and a result that arrives after the request deadline fails with `FAILED_DUE_TO_LATENCY`, which fails the request even if every subtask met its own bound.
- If a required parent fails, its whole subtree fails with `FAILED_BECAUSE_DEPENDENCY_FAILED`. These subtasks are counted in a dedicated column, "Tasks not executed because a dependency failed", and are not added to the failure rate, since they were never sent.
- A subtask declared `optional="true"` is skipped with status `SKIPPED` when one of its dependencies fails, and the request continues. Its dependents are released without the missing input. Skipped subtasks have their own column, "Tasks skipped because their optional input failed", and are not failures. An optional subtask needs a dependency, so the parser rejects an optional root.
- Each subtask keeps its own latency bound, sizes, and task length, and each has its own container. The application placement is shared by default, so subtasks of one application follow the same orchestration decision until that placement is invalidated. An orchestrator that routes subtasks to different nodes clears the placement after every decision, as in `Example9`, `Example10`, and `Example24`; see [Writing orchestrators](writing-orchestrators.md#placing-the-subtasks-of-a-service-graph).
- The graph must be acyclic, every `depends_on` name must exist, and a subtask cannot depend on itself. A violation stops the run with a message naming the application, the subtask, and the file.

The text report and the CSV also contain request-level results: the number of
requests generated, the percentage that completed successfully, the number that
failed, and the average end-to-end makespan of the successful ones. A request
succeeds when none of its required subtasks failed; skipped optional subtasks do
not fail it. Use these values instead of the task counts when the claim is about
the service.

Applications without `<subtask>` elements are untouched: they keep producing one independent task per request, with the same fields, the same timing, and the same counts as before. You can mix monolithic and graph applications in one file.

Custom task classes that implement `Task` directly keep working for monolithic applications through the default methods of the interface. To take part in a graph, a task class must also implement the dependency methods, which `TaskAbstract` already does.


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
| `mobility` | boolean, with optional attributes | Whether the device moves. The `model` attribute selects a mobility model by name and the other attributes become its options, see [Choosing a model per device type](models.md#choosing-a-model-per-device-type). |
| `speed` | meters per second | Movement speed. `1.4` m/s is about 5 km/h. |
| `minPauseDuration`, `maxPauseDuration` | seconds | Range of the pause before each movement. |
| `minMobilityDuration`, `maxMobilityDuration` | seconds | Range of the movement duration before the next pause. |
| `battery` | boolean | Whether the device runs on battery. Devices without batteries never die from energy depletion. |
| `percentage` | percent | Share of this device type. The percentages of all types must sum to 100; the parser checks their means when they are distributions. |
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

Device types are instantiated in file order. Each type receives its exact share of the scenario device count, rounded with the largest remainder method: every share is rounded down, then the types with the largest fractional part receive one device each. The total is therefore the scenario count, without the old padding of missing devices with the last type. The parser requires the configured percentages, or their means when they are distributions, to sum to 100; a sampled total that deviates prints a warning and is used as is, capped at the scenario count.

### Mobility models and placement

Each type may move differently. The `model` attribute of the `mobility` element selects one of the built-in models, and the remaining attributes are its options:

```xml
<mobility model="gauss-markov" alpha="0.8" std="15">true</mobility>
<mobility model="waypoint" waypoints="100,100;400,100;400,400">true</mobility>
<mobility model="group" group="buses" anchor_speed="15">true</mobility>
```

Names: `random-waypoint` (the default, also spelled `default`), `random-walk`, `gauss-markov`, `manhattan`, `waypoint`, `circular`, `levy-walk`, `brownian`, and `group`. A fully qualified class name works too. Without the attribute, the class given to `setCustomMobilityModel` is used. An unknown name stops the run and lists the known ones. The options of each model, with their defaults, are in [Simulation models](models.md#choosing-a-model-per-device-type).

Devices of a type start at uniformly random positions on the map. To gather them in one area, add a `location` element to the device type; because its coordinates are distributions, each instance lands on a different point of the same hotspot:

```xml
<location>
    <x_pos distribution="normal" mean="150" std="30" min="0" max="300"/>
    <y_pos distribution="normal" mean="150" std="30" min="0" max="300"/>
</location>
```

Any fixed or sampled position is clamped to the map. Edge data centers keep the required single `location` with plain coordinates: two edge data centers at the same position are rejected.

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
- **Device percentages must sum to 100.** The parser rejects other sums (it checks the means when the percentages are distributions).
- **The application usage percentages have a remainder rule.** See the application section above.
- **The task generator removes non-generating devices.** If every device has `generateTasks=false`, the task list is empty and the simulation produces no meaningful results.
- **Sensors are excluded from multicast categories.** `mistOnlyList` includes them, while `mistOnlyListSensorsExcluded` does not. The default orchestrator uses the latter.
- **Changing the map requires updating edge data center coordinates.** A data center outside the map is still reachable in the graph, but the geometry no longer reflects your intended scenario.

## Related reading

- [Simulation models](models.md): how the configured values are used by the energy, mobility, and network models.
- [Output and metrics](output-and-metrics.md): where the configured output options send their files.
- [Research guide](research-guide.md): choosing parameter values for a credible study.
