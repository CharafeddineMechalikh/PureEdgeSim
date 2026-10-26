# Example 8: fuzzy logic orchestration

## What it shows

How to implement a published offloading policy on top of the orchestrator API.
The example follows the two-stage fuzzy workload orchestration of:

> C. Sonmez, A. Ozgovde and C. Ersoy. Fuzzy Workload Orchestration for Edge
> Computing. IEEE Transactions on Network and Service Management 16(2), 2019,
> 769-782.

with one extension: mist devices are candidates too. The policy reads the CPU
usage of the candidates and the task requirements, evaluates two fuzzy rule
sets, and returns the class of node to use. The example evaluates stage 2 before
stage 1, as the original work suggests, to avoid running both stages when the
first one already answers.

The fuzzy rule sets are ordinary `.fcl` files in `settings/`, so the policy can
be inspected and changed without recompiling. `Example8FuzzyLogicOrchestrator`
also keeps `ROUND_ROBIN`, which gives a baseline in the same run.

## The scenario

The files in `settings/` describe:

- the `ALL` architecture, so cloud, edge, and mist nodes are candidates;
- 300 devices and a ten minute run;
- `orchestration_algorithms=FUZZY_LOGIC,ROUND_ROBIN`;
- `enable_orchestrators=false`, so each device applies the algorithm locally
  instead of sending its request to a remote orchestrator. The settings file
  explains the alternative.

## Requirements

This example needs jFuzzyLogic. The jar is bundled at
`PureEdgeSim/libs/jFuzzyLogic_v3.0.jar`, and `pom.xml` excludes the fuzzy
classes from the default compilation. Import the project in an IDE, add the jar
to the module dependencies, and run `examples.Example8`. A `mvn exec:java` run
works only after those exclusions are removed.

## What to look for

- The CSV has one row for `FUZZY_LOGIC` and one for `ROUND_ROBIN`, under the
  same seed and workload. The delay, failure, and energy columns are the
  comparison.
- The per-tier executed-task counts show how often each stage chose the cloud,
  the edge, or a mist device. A policy that concentrates on one tier barely uses
  the fuzzy stages.
- Fuzzy decisions react to load: in a short run with few devices, many tasks
  find an idle local candidate and stay on mist.
- Results go to `output/Example8_output/`.

## Files

- `src/main/java/com/mechalikh/pureedgesim/examples/Example8.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example8FuzzyLogicOrchestrator.java`
- `settings/stage1.fcl` and `settings/stage2.fcl`
- `settings/` with the algorithm list and architecture

## Where to go next

- [Writing orchestrators](../../docs/writing-orchestrators.md)
- [Example 5: plugging in a custom orchestrator](../Example5/README.md)
- [Example 9: graph partitioning](../Example9/README.md)
