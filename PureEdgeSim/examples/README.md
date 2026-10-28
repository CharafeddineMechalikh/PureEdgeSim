# Examples

Each runnable scenario has a self-contained folder: its Java sources, a
`README.md`, the `settings/` files it runs with, and an `output/` folder for its
results. The classes share the flat `examples` package, so a scenario can reuse a
class from another folder (for instance the Q-learning orchestrator of
`Example11`); the READMEs link to the folder that owns each shared class.

| Example | Shows | Scenario | Build |
| --- | --- | --- | --- |
| [Example 1](Example1/README.md) | Every extension point of `Simulation` in one place | `Example1/settings/` | default |
| [Example 2](Example2/README.md) | A custom mobility model | `Example2/settings/` | default |
| [Example 3](Example3/README.md) | Cooperative edge data centers with edge orchestrators | `Example3/settings/` | default |
| [Example 4](Example4/README.md) | A custom computing node and its own events | `Example4/settings/` | default |
| [Example 5](Example5/README.md) | Plugging in a custom orchestrator | `Example5/settings/` | default (jFuzzyLogic) |
| [Example 6](Example6/README.md) | Custom settings files and a parallel sweep | `Example6/settings/` | default |
| [Example 7](Example7/README.md) | Clustering and cooperative container caching | `Example7/settings/` | default |
| [Example 8](Example8/README.md) | Fuzzy logic orchestration | `Example8/settings/` | default (jFuzzyLogic) |
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
| [Example 24](Example24/README.md) | Repartitioning a service graph while the simulation runs | `Example24/settings/` | default |
| [Example 25](Example25/README.md) | Replaying a recorded production trace instead of random arrivals | `Example25/settings/` | default |

The Java classes of each example are in its own folder. Run one from the
repository root after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example1"
```

`Example5` and `Example8` use the jFuzzyLogic jar that is committed at `libs/` in
the repository root. The `exec` plugin of the pom runs with the compile classpath,
so the two examples start with the same command as all the others. `Example25` reads
its trace from a path relative to the working directory, so it too must be started
from the repository root.

Start with [Example 1](Example1/README.md) for the structure of a simulation,
[Example 3](Example3/README.md) for edge coordination,
[Example 9](Example9/README.md) and [Example 10](Example10/README.md) for
service graphs, and [Example 25](Example25/README.md) to drive the traffic from
measured data rather than from a distribution. The full manual is in
[docs/](../../docs/).
