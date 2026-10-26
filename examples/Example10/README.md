# Example 10: partitioning a service graph with a genetic algorithm

## What it shows

How to search for the placement of a service graph instead of deriving it from
the graph structure. The application is the same video analytics pipeline as
Example 9: seven subtasks, one request, several nodes.

`GeneticPartitioner` searches the node of every subtask:

- a chromosome holds one gene per subtask, and a gene is an index into the list
  of candidate nodes;
- the population is evolved with tournament selection, uniform crossover, a
  small per-gene mutation rate, and elitism, so the best plan found is never
  lost;
- the fitness estimates the makespan of the pipeline in topological order, plus
  a fraction of the time spent transferring parent results between nodes, plus a
  penalty for the excess work of the busiest used node.

Subtasks that receive the same node form a partition. `Example10GeneticOrchestrator`
resolves each subtask name to its gene, checks that the chosen node is eligible
for this request, and falls back to the default policy when it is not, for
example when a mist device has moved out of range.

## The API detail this example exists to show

As in Example 9, the application placement cache must be cleared after every
decision, otherwise later subtasks reuse the destination of the first one. Both
overrides of `orchestrate` clear `setApplicationPlaced(false)` for that reason.

The plan itself is static: it is computed once, from the mean task lengths and
result sizes, before any device moves. The eligibility check at runtime is what
keeps the plan safe on a mobile map.

## The scenario

The files in `settings/` use the same scenario as Example 9:

- 150 devices, a 2000 by 2000 meters map, a 50 meters mist range;
- four linked edge data centers and the `ALL` architecture;
- five simulated minutes and `random_seed=42`.

The seed is the same one the simulation uses, so the plan is reproducible.
Remove `random_seed` from the settings file to let the algorithm start from a
different random population on each run.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example10"
```

## What to look for

The console prints the best plan and its estimated cost before the run:

```text
Example10GeneticOrchestrator - Application 0 (Soft Real-time): fitness 0.2313 (makespan 0.2083 s, transfers 0.0017 s, imbalance 0.7374)
  {capture=Cloud#0, preprocess=Mist#67, detect=Edge#2, classify=Cloud#0, track=Mist#83, aggregate=Edge#2, notify=Mist#83}
```

- `makespan` is the estimated finish time of the last subtask, in seconds.
- `transfers` is the sum of the estimated result transfers that cross a node
  boundary.
- `imbalance` is the relative excess work of the busiest used node. A value near
  zero means the work is spread evenly.
- The placement shows which node type each subtask chose. A plan can keep a
  whole chain on one fast node, or split it to use idle devices.
- With seed 42 the shipped scenario exceeds 99.9 percent success.
- Results go to `output/Example10_output/`.

Compare the plan and its estimated cost with the cut printed by Example 9. The
two examples solve the same problem with different information: one reads the
graph structure, the other searches placements and accounts for node speed and
transfer time.

## Files

- `src/main/java/com/mechalikh/pureedgesim/examples/Example10.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example10GeneticOrchestrator.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/GeneticPartitioner.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/ServiceGraph.java`
- `settings/applications.xml` with the service graph

## Where to go next

- [Writing orchestrators: placing the subtasks of a service graph](../../docs/writing-orchestrators.md#placing-the-subtasks-of-a-service-graph)
- [Configuration: service graphs](../../docs/configuration.md#service-graphs)
- [Example 9: the same problem with graph theory](../Example9/README.md)
