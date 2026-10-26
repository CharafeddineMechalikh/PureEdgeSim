# Examples

Each runnable scenario has a folder here with its own README, and its scenario
files when the default settings are not enough:

| Example | Shows | Scenario | Build |
| --- | --- | --- | --- |
| [Example 1](Example1/README.md) | Every extension point of `Simulation` in one place | default `settings/` | default |
| [Example 2](Example2/README.md) | A custom mobility model | default `settings/` | default |
| [Example 3](Example3/README.md) | Cooperative edge data centers with edge orchestrators | `Example3/settings/` | default |
| [Example 4](Example4/README.md) | A custom computing node and its own events | default `settings/` | default |
| [Example 5](Example5/README.md) | Plugging in a custom orchestrator | `Example5/settings/` | needs jFuzzyLogic |
| [Example 6](Example6/README.md) | Custom settings files and a parallel sweep | `Example6/settings/` | default |
| [Example 7](Example7/README.md) | Clustering and cooperative container caching | `Example7/settings/` | default |
| [Example 8](Example8/README.md) | Fuzzy logic orchestration | `Example8/settings/` | needs jFuzzyLogic |
| [Example 9](Example9/README.md) | Service graph partitioning with graph theory | `Example9/settings/` | default |
| [Example 10](Example10/README.md) | Service graph partitioning with a genetic algorithm | `Example10/settings/` | default |
| [Example 11](Example11/README.md) | Tabular Q-learning orchestration | `Example11/settings/` | default |
| [Example 12](Example12/README.md) | Multi-access MEC with an access-aware orchestrator | `Example12/settings/` | default |
| [Example 13](Example13/README.md) | Multi-layer fog for a smart home | `Example13/settings/` | default |
| [Example 14](Example14/README.md) | Mist-only reinforcement placement | `Example14/settings/` | default |
| [Example 15](Example15/README.md) | Multi-layer guided reinforcement learning | `Example15/settings/` | default |
| [Example 16](Example16/README.md) | Softmax policy for smart-city offloading | `Example16/settings/` | default |
| [Example 17](Example17/README.md) | Context-aware deep reinforcement offloading | `Example17/settings/` | default |
| [Example 18](Example18/README.md) | Ant colony offloading | `Example18/settings/` | default |
| [Example 19](Example19/README.md) | Deadline-aware placement by task criticality | `Example19/settings/` | default |
| [Example 20](Example20/README.md) | Multi-objective reinforcement learning | `Example20/settings/` | default |
| [Example 21](Example21/README.md) | Storage-aware container caching with LRU eviction | `Example21/settings/` | default |
| [Example 22](Example22/README.md) | Self-organizing device clusters with an elected head | `Example22/settings/` | default |
| [Example 23](Example23/README.md) | Custom nodes generator, task generator, topology creator, and simulation manager | `Example23/settings/` | default |

The Java classes are in
`src/main/java/com/mechalikh/pureedgesim/examples/`. Run one from the repository
root after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example1"
```

`Example5`, `Example8`, and `Example8FuzzyLogicOrchestrator` are excluded from
the default Maven build because they need the jFuzzyLogic jar in
`PureEdgeSim/libs/`. Their READMEs explain how to run them from an IDE.

Start with [Example 1](Example1/README.md) for the structure of a simulation,
[Example 3](Example3/README.md) for edge coordination, and
[Example 9](Example9/README.md) and [Example 10](Example10/README.md) for
service graphs. The full manual is in [docs/](../docs/).
