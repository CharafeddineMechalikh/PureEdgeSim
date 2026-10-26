---
description: "Install, build, and run your first PureEdgeSim simulation, read the output, create a custom scenario, and use the simulator as a library."
---
# Getting started

This guide takes you from a clean machine to a completed PureEdgeSim run, then explains what the run produced. It assumes no prior knowledge of the simulator, but it does assume basic familiarity with Java and the command line.

## What you need

| Requirement | Version | Notes |
| --- | --- | --- |
| Java Development Kit | 8 or newer | The project targets Java 8. Continuous integration builds with JDK 17. Any modern JDK works for running. |
| Maven | 3.6 or newer | Used for compilation, tests, packaging, and Javadoc. |
| Git | Any recent version | Needed only to clone the repository. |
| A desktop environment | Optional | Required only for the live charts. Headless servers can disable them. |

Check your installation:

```bash
java -version
mvn -version
```

If Maven prints a Java version different from `java -version`, set `JAVA_HOME` to the JDK you want Maven to use.

## Get the code

```bash
git clone https://github.com/CharafeddineMechalikh/PureEdgeSim.git
cd PureEdgeSim
```

The runnable examples live in `src/main/java/com/mechalikh/pureedgesim/examples/` and belong to the `examples` Java package. Each example that needs its own scenario has a folder at the repository root, with its README and its settings: `examples/ExampleX/README.md` and `examples/ExampleX/settings/`. The other examples use the default `settings/` folder.

The default configuration files are in `settings/`, and the default output folder is `output/`. These paths are relative to the working directory, so **run commands from the repository root** unless you configure absolute paths.

## Build

```bash
mvn clean install -DskipTests
```

This compiles the framework, runs the packaging steps, and installs the artifact into your local Maven repository. For a quick compile without installation:

```bash
mvn clean compile
```

The build excludes `Example5`, `Example8`, and `Example8FuzzyLogicOrchestrator` because they demonstrate optional of the jFuzzyLogic library and are intended to be run from an IDE after you add the bundled jar. The other examples, including `Example1`, are compiled normally.

## Run your first simulation

```bash
mvn exec:java -Dexec.mainClass="examples.Example1"
```

`Example1` launches the default scenario. It does not customize any model; it only shows the extension points so that you can see where custom classes would be plugged in. The simulation runs for 10 simulated minutes, generates tasks for 100, 200, and 300 edge devices, and evaluates all seven orchestration architectures with the two default algorithms. Depending on your machine this takes seconds to a few minutes of wall-clock time, and the live charts stay open when the run finishes.

To run an example from an IDE, import the repository as a Maven project and run the `main` method of any class in `examples`. This is the easiest route for the examples that require optional libraries.

### Run without charts

Live charts require a graphical environment and keep the process alive after the simulation ends. On a server, in a container, or in a CI job, edit `settings/simulation_parameters.properties`:

```properties
display_real_time_charts=false
save_charts=false
```

The simulation then runs headless and exits when the last iteration finishes. If you want chart images without displaying them, leave `save_charts=true` and disable only `display_real_time_charts`. Chart saving is controlled separately in `SimulationThread`, so both combinations are supported.

### Run with a shorter scenario

The default scenario is intentionally small but still runs 42 iterations (seven architectures times two algorithms times three device counts). For a first look, reduce the sweep in the same properties file:

```properties
simulation_time=1
min_number_of_edge_devices=100
max_number_of_edge_devices=100
edge_device_counter_size=100
orchestration_architectures=ALL
orchestration_algorithms=TRADE_OFF
```

This produces one iteration and finishes almost immediately. Restore the original values when you are ready to run a full experiment.

## What just happened

When you launched `examples.Example1`, the following sequence occurred:

1. `Simulation.launchSimulation()` parsed the five configuration files and validated them.
2. It expanded the architecture, algorithm, and device-count lists into a list of scenarios.
3. For each scenario, a `SimulationThread` created a fresh `PureEdgeSim` engine, a `SimulationManager`, and the default models.
4. The engine generated computing nodes, created the network topology, generated the task list, and started the event loop.
5. `DefaultSimulationManager` sent tasks to the `Orchestrator` in batches. The orchestrator chose a destination for each task.
6. The network model transferred the request, the destination executed the task when resources became available, and the results traveled back.
7. The logger aggregated metrics and appended one CSV row per iteration.
8. At the end, the chart window was shown (unless disabled) and the CSV and optional text report were written.

The next guides explain each step in detail. See [Architecture](architecture.md) and [Task lifecycle](task-lifecycle.md).

## Where the results are

The default output folder is `output/`. Every run creates a timestamped subfolder. In sequential mode the layout is:

```text
output/
└── 2026-10-24_23-29-48/
    ├── run.properties
    ├── settings/
    │   ├── simulation_parameters.properties
    │   ├── applications.xml
    │   ├── edge_datacenters.xml
    │   ├── edge_devices.xml
    │   └── cloud.xml
    ├── Sequential_simulation.csv
    ├── Sequential_simulation.txt              (if save_log_file=true)
    ├── Final results/                         (if save_charts=true)
    │   ├── Architectures Energy/
    │   │   ├── Average energy consumption (Wh/Computing node)__TRADE_OFF.png
    │   │   └── ...
    │   ├── Algorithms Energy/
    │   │   ├── Average energy consumption (Wh/Computing node)__ALL.png
    │   │   └── ...
    │   ├── Architectures CPU Utilization/
    │   ├── Algorithms CPU Utilization/
    │   ├── Architectures Network/
    │   ├── Algorithms Network/
    │   ├── Architectures Tasks/
    │   └── Algorithms Tasks/
    └── simulation_0/                          (only if live charts are enabled)
        └── iteration_2__ALL_TRADE_OFF_100/
            ├── map_chart.png
            ├── cpu_usage.png
            ├── tasks_success_rate.png
            └── network_usage.png              (if use_one_shared_wan_link=true)
```

Two kinds of images are produced, and they answer different questions:

- **Aggregate charts** in `Final results/` compare scenarios after the whole sweep has finished. `ChartsGenerator` reads the CSV back from disk and draws one image per metric: the `Architectures...` folder fixes an algorithm and compares architectures, while the `Algorithms...` folder fixes an architecture and compares algorithms. These are the plots you use in a paper.
- **Per-iteration snapshots** under `simulation_<id>/iteration_.../` are taken at the end of each simulation while the live visualizer is running. They are only written when `display_real_time_charts=true` and `save_charts=true`. They show the state of that single iteration (map, CPU, success rate, network), not a cross-scenario comparison.

In parallel mode, `simulationId` is no longer always 0, so the CSV files are named `Parallel_simulation_<id>.csv`, one per worker. Aggregate charts are disabled in that mode because there is no single merged CSV to read. See [Output and metrics](output-and-metrics.md) for the folder rules and the meaning of every CSV column.

A quick sanity check:

```bash
head -2 output/<timestamp>/Sequential_simulation.csv
```

The first line is the header, and the following lines are one scenario each. If the file is empty, the run probably failed during configuration parsing; scroll up in the console for the validation message.

## Create your first custom scenario

The smallest possible program that changes behavior is a custom orchestrator. The following class sends every task to the cloud, regardless of its requirements:

```java
package examples;

import com.mechalikh.pureedgesim.simulationmanager.Simulation;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.taskgenerator.Task;
import com.mechalikh.pureedgesim.taskorchestrator.Orchestrator;

public class MyFirstOrchestrator extends Orchestrator {

    public MyFirstOrchestrator(SimulationManager simulationManager) {
        super(simulationManager);
    }

    @Override
    protected int findComputingNode(String[] architectureLayers, Task task) {
        for (int i = 0; i < nodeList.size(); i++) {
            if (offloadingIsPossible(task, nodeList.get(i), architectureLayers)
                    && nodeList.get(i).getType() == com.mechalikh.pureedgesim.scenariomanager.SimulationParameters.TYPES.CLOUD) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void resultsReturned(Task task) {
        // Called when the results of a task come back to the source device.
    }

    public static void main(String[] args) {
        Simulation sim = new Simulation();
        sim.setCustomEdgeOrchestrator(MyFirstOrchestrator.class);
        sim.launchSimulation();
    }
}
```

The constructor is mandatory because PureEdgeSim instantiates orchestrators reflectively with a `SimulationManager` argument. The `findComputingNode` method returns an index into `nodeList`, or `-1` when no destination is acceptable. The architecture list must contain the layer you target; the default scenario evaluates every architecture, so some iterations will legitimately return `-1` for a cloud-only orchestrator. See [Writing orchestrators](writing-orchestrators.md) for a complete explanation and a more realistic example.

To run this class from the command line after adding it to the source tree:

```bash
mvn compile exec:java -Dexec.mainClass="examples.MyFirstOrchestrator"
```

## Use PureEdgeSim as a library

You do not have to clone the repository. The simulator is published on Maven Central as `com.mechalikh:pureedgesim`. Add it to your project, create a `Simulation`, and call `launchSimulation()`:

```xml
<dependency>
  <groupId>com.mechalikh</groupId>
  <artifactId>pureedgesim</artifactId>
  <version>5.1.0</version>
</dependency>
```

When using the library, either place the configuration files in the default relative location or point the simulation to your own folder:

```java
Simulation sim = new Simulation();
sim.setCustomSettingsFolder("/absolute/path/to/my_settings/");
sim.setCustomOutputFolder("/absolute/path/to/my_results/");
sim.launchSimulation();
```

The settings folder must contain exactly these five files: `simulation_parameters.properties`, `applications.xml`, `cloud.xml`, `edge_datacenters.xml`, and `edge_devices.xml`.

## Run the tests

```bash
mvn test
```

The suite contains more than 500 tests. Some are integration tests that launch small simulations, so the full run takes longer than a typical unit-test suite. To run only the fast unit tests:

```bash
mvn test -Punit-tests
```

Coverage is measured with JaCoCo. After `mvn test`, open `target/site/jacoco/index.html` in a browser. The XML report is at `target/site/jacoco/jacoco.xml`.

## Next steps

- Change the scenario: [Configuration](configuration.md).
- Understand the event flow: [Task lifecycle](task-lifecycle.md).
- Write a real orchestrator: [Writing orchestrators](writing-orchestrators.md).
- Replace a simulation model: [Customizing models](customizing-models.md).
- Design an experiment: [Research guide](research-guide.md).
