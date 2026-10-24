# Frequently asked questions

Short answers to questions that come up when running, extending, or interpreting PureEdgeSim. Each answer points to the guide that covers the topic in depth.

## Running and building

### Which Java version do I need?

The source targets Java 8, so any JDK from 8 onward can compile and run it. The project is built and tested with newer JDKs in continuous integration. If Maven picks a different JDK than the one on your `PATH`, set `JAVA_HOME`.

### The quick start uses `examples.Example1`, but I see no such class.

Run from the repository root with Maven, which compiles the sources first:

```bash
mvn compile exec:java -Dexec.mainClass="examples.Example1"
```

The Java package is `examples`, even though the source files live under `src/main/java/com/mechalikh/pureedgesim/examples/`. Three optional examples (`Example5`, `Example8`, `Example8FuzzyLogicOrchestrator`) are excluded from the default build because they need jFuzzyLogic. Run those from an IDE after adding the bundled library.

### The JVM does not exit when the simulation finishes.

Live charts keep a Swing window open, and a non-daemon window keeps the JVM alive. Close the chart window, or disable charts:

```properties
display_real_time_charts=false
save_charts=false
```

### Why is there a pause between iterations?

`pause_length` adds a wall-clock pause after each scenario so that live results can be inspected. It does not affect simulated time. Set it to `0` for batch runs.

### How do I run a single scenario instead of the full sweep?

Set one architecture, one algorithm, and one device count:

```properties
min_number_of_edge_devices=100
max_number_of_edge_devices=100
edge_device_counter_size=100
orchestration_architectures=ALL
orchestration_algorithms=TRADE_OFF
```

The device count list is built from `min`, `max`, and the step size, so equal minimum and maximum produce one value.

### How do I use my own settings folder?

```java
Simulation simulation = new Simulation();
simulation.setCustomSettingsFolder("/absolute/path/to/settings/");
simulation.launchSimulation();
```

The path must end with a separator, because the setter appends the file names directly. The folder must contain exactly the five input files listed in [Configuration](configuration.md).

### Can I launch several simulations at once with different settings?

Not safely in parallel. The parsed settings live in static fields (`SimulationParameters`), so two concurrent launches with different folders overwrite each other. Launch them sequentially, or run separate JVMs.

## Results and configuration

### Why are my results different on every run?

Task arrival times and mobility are generated with `SecureRandom`, and there is no seed parameter. The simulator is stochastic by design. Use repetitions and report distributions. If you need bit-identical runs for debugging, implement deterministic random sources in a custom task generator and mobility model, as described in [Research guide](research-guide.md).

### Why do all tasks fail in some architectures?

The default orchestrator cannot serve an architecture whose layers it cannot use. A cloud-only orchestrator returns `-1` when the candidate list contains no cloud node, for example under `MIST_ONLY`. Tasks then fail with `NO_OFFLOADING_DESTINATIONS`. Either restrict the architecture list or make the orchestrator handle every architecture in the sweep.

### The CSV success count is negative. Is that a bug?

Column 9 is computed as sent tasks minus failed tasks, and the failed counter includes tasks that were never sent because their device was already dead. In runs with early device deaths the subtraction can undercount and occasionally go negative. Derive success from generated tasks and the failure columns instead. The exact formulas and the censoring caveat are in [Output and metrics](output-and-metrics.md).

### Why is the execution delay so small compared with the task latency?

The execution-delay columns contain CPU time only. Network time is not included. End-to-end delay is available inside the simulator as `task.getTotalDelay()` but is not written to the CSV. Use a custom logger if the claim is about end-to-end latency.

### Why did changing bandwidth not change my results much?

Check `realistic_network_model`. When it is `false`, each transfer completes in one step and uses only its instantaneous bandwidth share, so there is no congestion over time. Bandwidth still affects the computed transfer time, but much less than in a loaded, realistic run. Also confirm that the changed parameter corresponds to the link type used by your traffic.

### Why do tasks keep going to the same node even though another node is free?

Application placement is cached per device. The orchestrator is called only when the device has no placement. Later tasks reuse the first decision until a failure or a mobility event clears it. See [Models](models.md) for the exact invalidation rules.

### Why do tasks fail with "no resources" when nodes look idle?

The destination rejects a task when available RAM is not greater than the container size or when storage is insufficient. A node can be idle in CPU terms and still lack RAM or storage. The failure is checked before execution at phase 2. See [Task lifecycle](task-lifecycle.md).

### Why are no tasks generated for some devices?

Sensors and devices with `isGeneratingTasks()` false are excluded. Application usage percentages also assign some devices exclusively to other applications. The last application in `applications.xml` receives all remaining devices, including rounding remainders. See [Configuration](configuration.md).

### Why is the output folder deleted at the start of a run?

`clear_output_folder=true` deletes the configured output folder on the first iteration of a run. Keep it `false` unless you are sure the folder contains nothing you need. Point experiments at a dedicated output folder.

### Why does the CSV contain `NaN` or `Infinity`?

An average divided by a zero count. Common causes are a scenario with no completed tasks, no finished transfers, or an architecture with no node of a given level. Treat these as missing values and check the scenario setup. Do not replace them with zero.

### Where is the MAN network usage in the CSV?

It is not exported. MAN usage is computed and printed in the text report and console, but the CSV contains LAN and WAN usage columns. Use `save_log_file=true` if you need MAN numbers, or extend `SimLog`.

### Why are the first network column and the LAN column identical?

That is a known labeling issue in the current writer. The column labeled "Network usage (s)" receives LAN usage, and the next LAN column receives the same value. Do not add them together. See [Output and metrics](output-and-metrics.md).

## Charts and visualization

### Why were no charts generated?

Three conditions can prevent charts:

- `save_charts=false` disables both aggregate and per-iteration images.
- `parallel_simulation=true` disables both aggregate charts and the live visualizer, so parallel runs produce no images at all.
- Per-iteration snapshots also require `display_real_time_charts=true`.

The aggregate charts run in sequential mode when `save_charts=true`, even if live display is off. See [Output and metrics](output-and-metrics.md).

### Where is `Final results`?

It is created next to the CSV file: `<output>/<timestamp>/Final results/`. Inside are `Architectures ...` and `Algorithms ...` folders grouped by metric family. If the folder is missing, check that the run was sequential and that `save_charts` was true.

### The aggregate charts are empty or have one point.

The x-axis is the edge device count. A sweep with a single device count produces a single point per series. Add device counts to get lines.

### Can I get the raw chart data?

Yes. The charts are drawn from the CSV. Read the CSV directly and plot it with your own tool; this is also the recommended route for publication figures because it gives full control over colors, labels, and confidence intervals.

## Custom models and orchestrators

### How do I add a new orchestration algorithm?

Set a free-form algorithm name in `orchestration_algorithms` and handle that name in a custom orchestrator. Algorithm names are not validated by the parser; your class decides what they mean. Architecture names, by contrast, are fixed by `Orchestrator.initialize()`. See [Writing orchestrators](writing-orchestrators.md).

### How do I add a new architecture?

You cannot add one through configuration alone. The seven architecture names are hard-coded in `Orchestrator.initialize()`, and an unknown name leaves the candidate list and layer array unset. Adding an architecture requires a custom orchestrator that overrides `initialize()` and a scenario configuration that accepts the name.

### Why does my custom class fail with `NoSuchMethodException`?

Models are instantiated reflectively with fixed constructor signatures. Check the table in [Models](models.md). A common mistake is swapping the storage and RAM arguments of the computing node constructor, or using a different order than the one declared.

### Can I change the energy model without replacing the computing node?

Not through a top-level setter. The default generator constructs `EnergyModelComputingNode` inside `createComputingNode`. Replace the model in a custom computing node constructor, or provide a custom computing nodes generator that assigns your energy model. See [Customizing models](customizing-models.md).

### Why does my custom event never arrive?

Common causes:

- The event tag collides with a base-class tag.
- The event was scheduled on another entity.
- `processEvent` does not call `super.processEvent`, which breaks the base behavior, or returns before handling the tag.
- The model was never registered with the matching setter.

Use deep logging and a short scenario to trace the first events.

### How do I print per-task decisions?

Set `deep_log_enabled=true`. The orchestrator base class logs each assignment, and the simulation manager logs the task pipeline. The output is large, so use it on a short run.

### Can I run a custom orchestrator with the default algorithms in the same sweep?

Yes. Extend `DefaultOrchestrator` and delegate to `super.findComputingNode(...)` for `TRADE_OFF` and `ROUND_ROBIN`, then handle your own algorithm name. See [Writing orchestrators](writing-orchestrators.md).

## Energy and battery

### Why do data centers report no battery?

Only edge devices are battery powered by default. The `battery` field in `edge_devices.xml` controls it, and data center entries do not have that field. Battery-death statistics therefore refer to edge devices.

### Why is the average remaining power high when many devices died?

The average is computed over battery-powered devices that are still alive. Dead devices are represented as zero in the per-device list but excluded from the average. Read the dead-device count and the per-device list together.

### Why did no device die even with a small battery?

Check that the device entry actually has `battery=true`, a finite `batteryCapacity`, and a nonzero initial level. Also check the simulation horizon: death is only detected on `update_interval` ticks, and a long horizon is needed to drain a battery through idle and dynamic consumption.

## Research practice

### How many repetitions do I need?

At least 10 to 30 per scenario for means, and more for heavy-tailed metrics such as first death time. Report the number and a confidence interval or IQR. A single run is not evidence. See [Research guide](research-guide.md).

### Can I compare a 10-minute run with a 60-minute run?

No. The number of tasks, battery states, and in-flight censoring differ. Fix the horizon across compared scenarios and justify it.

### How do I cite PureEdgeSim?

Use the framework papers listed in the repository README, preferably the 2021 PureEdgeSim paper and the 2025 comparative study, rather than the GitHub URL. Include the version and any model changes in your methodology.

### Where do I report a bug or ask for help?

Use the GitHub issue tracker. Include the settings files, the PureEdgeSim version or commit, the Java version, the exact command, and the full console output. A runnable minimal scenario is worth more than a screenshot.
