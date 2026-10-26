# Example 5: plugging in a custom orchestrator

## What it shows

The minimum amount of code needed to replace the offloading decision. This
example keeps the default models and changes one line:
`sim.setCustomEdgeOrchestrator(Example8FuzzyLogicOrchestrator.class)`.

The orchestrator used here is the fuzzy-logic one from Example 8. It is a real
policy rather than a stub, so the run produces meaningful placement decisions
while the example itself stays short. Remove that line and the framework uses
`DefaultOrchestrator` with the same scenario, which makes the before and after
comparison immediate.

## Why a separate settings folder

A custom orchestrator may accept algorithm names that the default class does
not know. `Example8FuzzyLogicOrchestrator` accepts `FUZZY_LOGIC` and
`ROUND_ROBIN` and rejects anything else, so this example ships
`settings/simulation_parameters.properties` with exactly those two names. The
other files are the default scenario. A run therefore executes the custom
algorithm and the built-in baseline in the same sweep.

## Requirements

This example needs jFuzzyLogic. The jar is bundled at
`PureEdgeSim/libs/jFuzzyLogic_v3.0.jar`, and `pom.xml` excludes `Example5`,
`Example8`, and `Example8FuzzyLogicOrchestrator` from the default compilation
for that reason. Run it from an IDE with the project on the classpath, or remove
those exclusions temporarily if you have the jar available to Maven. The
framework tests do not compile this example.

## Run it

From the repository root, in an IDE:

1. Import the Maven project.
2. Add `PureEdgeSim/libs/jFuzzyLogic_v3.0.jar` to the module dependencies.
3. Run `examples.Example5`.

A `mvn exec:java` run works only after the exclusions in `pom.xml` are removed.

## What to look for

- The console reports which algorithm ran for each scenario.
- The CSV has one row per algorithm, so the fuzzy policy and round robin can be
  compared directly under the same seed and workload.
- Results are written to `output/Example5_output/`.

## Files

- `src/main/java/com/mechalikh/pureedgesim/examples/Example5.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example8FuzzyLogicOrchestrator.java`
- `settings/` with `orchestration_algorithms=FUZZY_LOGIC,ROUND_ROBIN`

## Where to go next

- [Writing orchestrators](../../docs/writing-orchestrators.md)
- [Example 8: fuzzy logic](../Example8/README.md)
- [Example 9: graph partitioning](../Example9/README.md)
