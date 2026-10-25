---
description: "The PureEdgeSim manual: tutorials, architecture, models, configuration, output metrics, and research guidance for the edge computing simulator."
permalink: /docs/
---
# PureEdgeSim documentation

This folder is the long-form manual for PureEdgeSim, written for new researchers, PhD students, and engineers who need to understand, configure, extend, and trust the simulator. The guides explain both **how to use** the framework and **why it behaves the way it does**.

If you are looking for the shortest path to a running simulation, read [Getting started](getting-started.md). If you want to understand the internals before changing anything, read [Architecture](architecture.md) and [Task lifecycle](task-lifecycle.md).

## Reading paths

### Path 1: I want to run an experiment this week

1. [Getting started](getting-started.md): install, build, run an example, read the output.
2. [Configuration](configuration.md): change the scenario, workload, devices, network, and algorithms.
3. [Output and metrics](output-and-metrics.md): understand the CSV columns and the charts.
4. [Research guide](research-guide.md): design a defensible experiment and report it.

### Path 2: I want to implement a new offloading algorithm

1. [Getting started](getting-started.md).
2. [Task lifecycle](task-lifecycle.md): where decisions happen in the event flow.
3. [Writing orchestrators](writing-orchestrators.md): a complete, buildable custom orchestrator.
4. [API reference](api-reference.md): the classes and methods you will call.

### Path 3: I want to change how the simulated system works

1. [Architecture](architecture.md): modules, dependencies, and event processing.
2. [Simulation models](models.md): computing, energy, mobility, and network models.
3. [Customizing models](customizing-models.md): replace each default model without modifying the codebase.
4. [Contributing](contributing-guide.md): tests, conventions, and the Javadoc build.

## Documentation map

| Document | What it answers |
| --- | --- |
| [Getting started](getting-started.md) | How do I install, build, and run PureEdgeSim? What does a first simulation look like? |
| [Architecture](architecture.md) | How is the simulator organized? How does the discrete-event engine work? How are scenarios initialized? |
| [Configuration](configuration.md) | What does every parameter in every configuration file mean, and what are its units? |
| [Task lifecycle](task-lifecycle.md) | What happens to a task from generation to result, and where can it fail? |
| [Simulation models](models.md) | How are computing nodes, energy, mobility, and network transfers modeled? |
| [Writing orchestrators](writing-orchestrators.md) | How do I implement and plug in my own orchestration algorithm? |
| [Customizing models](customizing-models.md) | How do I replace the mobility, energy, network, task generation, topology, or simulation manager? |
| [Output and metrics](output-and-metrics.md) | How do I read the CSV, the console report, and the charts? |
| [Research guide](research-guide.md) | How do I design, run, and report a credible simulation study? |
| [FAQ](faq.md) | Why did my task fail? Why is the simulation slow? Why is the chart empty? |
| [Contributing](contributing-guide.md) | How do I run the tests, check coverage, and build the Javadoc? |
| [API reference](api-reference.md) | Which package and class should I use for a given job? |

## The one-paragraph mental model

PureEdgeSim is a **discrete-event simulator**. You describe a scenario in five configuration files: simulation parameters, applications, cloud data centers, edge data centers, and edge devices. A **scenario** is one combination of orchestration architecture, orchestration algorithm, and number of edge devices. The framework generates a list of tasks, feeds them to an event engine, and lets a **workload orchestrator** decide where each task executes. The task then travels through the network, waits for computing resources, executes, and sends its results back. Every event updates the simulator clock. When the simulation ends, the framework aggregates delays, failures, CPU use, network traffic, and energy consumption into a CSV row, a console report, and optional charts. Each of these terms is explained in the guides above.

## Other sources of information

- [Project README](../): installation, features, publications, citation.
- [Examples](https://github.com/CharafeddineMechalikh/PureEdgeSim/tree/master/src/main/java/com/mechalikh/pureedgesim/examples): runnable scenarios with increasing complexity.
- [Javadoc](https://javadoc.io/doc/com.mechalikh/pureedgesim): method-level API documentation.
- [CHANGELOG](../CHANGELOG.md): release notes and behavioral changes.
- [Issue tracker](https://github.com/CharafeddineMechalikh/PureEdgeSim/issues): questions and bug reports.

## Conventions used in this manual

- **Simulation time** is the time inside the simulated world, measured in seconds unless stated otherwise.
- **Wall-clock time** is the real time your computer spends running the simulation.
- **Cloud**, **edge**, and **mist** refer to the three computing levels of the reference architecture: remote data centers, edge data centers, and end devices respectively.
- **Task** and **offloading request** are used interchangeably. A task is a unit of work generated by an application.
- Code snippets assume Java 8 syntax, because PureEdgeSim targets Java 8 even when built with a newer JDK.
- Configuration examples assume the default settings folder (`settings/`) and a working directory at the repository root. Paths are configurable, see [Configuration](configuration.md).
