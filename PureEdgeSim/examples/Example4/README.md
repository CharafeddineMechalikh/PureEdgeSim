# Example 4: a custom computing node

## What it shows

How to add behavior to every computing node by subclassing the node class. All
nodes in the simulation are created from the class registered with
`setCustomComputingNode`, so one subclass covers cloud, edge, and device nodes.

`Example4CustomComputingNode` extends `DefaultComputingNode` rather than
`ComputingNode` directly, so it keeps the default energy accounting, resource
tracking, and event handling, and only adds its own logic. It also implements
`OnSimulationStartListener`: when the simulation starts, the node schedules an
event to itself to prove that nodes can run their own periodic or one-off work.

The subtle part is event dispatch. Nodes receive many events, and the example
handles one custom tag in a `switch`, then forwards every other tag to
`super.processEvent`. Dropping that default branch stops the parent class from
updating energy consumption and CPU utilization, which silently corrupts the
results.

```mermaid
sequenceDiagram
    participant SM as SimulationManager
    participant Node as Custom computing node
    participant Ev as Event queue
    participant Def as DefaultComputingNode
    SM->>Node: onSimulationStart()
    Node->>Def: super.onSimulationStart()
    Node->>Ev: scheduleNow(this, DO_SOMETHING)
    Ev->>Node: deliver DO_SOMETHING
    Node->>Node: println "Event received..."
    Ev->>Node: later energy and CPU events
    Node->>Def: super.processEvent(ev) in the default branch
    Def-->>SM: energy and utilisation charts keep updating
    classDef mine fill:#cfe8ff,stroke:#333;
    class Node mine;
```

## The scenario

The `settings/` folder of this example is a byte for byte copy of the packaged
defaults in `PureEdgeSim/settings/`. The point is the node class, not the
workload, so the default applications, map, and device types are kept.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example4"
```

## What to look for

The run finishes in `41 minutes, 17 seconds` of wall clock and writes 42 rows
to the sweep CSV. The last row, `ALL / ROUND_ROBIN / 300 devices`, is the
console tail and the clearest place to read the cost of the pattern:

| Metric | Value |
| --- | --- |
| Tasks generated, sent | 33740, 100 % of generated |
| Successfully executed | 26629, 78.9241 % of sent |
| Failed on delay, on mobility, on device death | 6805 (20.1689 %), 306 (0.9069 %), 0 |
| Average execution, waiting, request makespan | 0.2032 s, 0.0235 s, 0.2914 s |
| Tasks placed on cloud, edge, mist | 2409 (731 ok), 12333 (10691 ok), 18998 (15207 ok) |
| Energy (computing, WAN, MAN, LAN) | 102.9106 Wh, 1.6079 Wh, 0 Wh, 26.2859 Wh |
| WAN time, LAN time, total traffic | 848.34 s, 207.5328 s, 35844.93 MBytes |
| Average remaining device battery | 99.4792 %, 32.7678 Wh, 0 dead devices |

Reading the run against the point of the example:

- The hook is exercised, not the outcome. `Event received, you can do any
  action here` appears once per node at simulation start on all 42 iterations
  (the capture has 11187 lines, most of them that line), and every other event
  still reaches `super.processEvent`, which is why the energy and CPU columns
  stay populated: 26629 successful tasks, 102.9106 Wh of computing energy, an
  average CPU utilisation of 1.2291 %. Removing the `default` branch of the
  `switch` would zero those columns, and this run is the reference for what a
  healthy subclass produces.
- The pattern is a per-node print at simulation start, so the run does not
  change the placement policy. The `ALL / ROUND_ROBIN / 300` metrics differ
  from Example 1's identical row (76.802 % success against 78.9241 %, 1011
  mobility failures against 306) only because Example 1 also registers the
  custom mobility model from Example 2. The example itself buys nothing on the
  results axis, it just proves the subclass survives the event dispatch.
- The real cost is wall clock. This run is roughly eight times slower than
  Example 1's identical 42-row sweep (41 minutes 17 seconds against 4 minutes
  58 seconds). Example 1 layers the trivial `Example2CustomMobilityModel`
  on top of this same custom computing node, so the pair here, custom node
  plus the framework default mobility model, is the configuration that shows
  the cost. Narrow `orchestration_architectures` or `orchestration_algorithms`
  before adding expensive per-node logic.
- Use the pattern for per-node metrics, custom scheduling, or a state machine.
  Keep the workload narrow when you do: `settings/simulation_parameters.properties`
  sweeps 7 architectures times 2 algorithms times 3 device counts, so drop
  `orchestration_architectures` down to one value before adding expensive
  per-node logic.
- Results go to `PureEdgeSim/examples/Example4/output/`.

## Files

- `Example4.java`
- `Example4CustomComputingNode.java`
- `settings/`, a copy of the packaged defaults in `PureEdgeSim/settings/`

## Where to go next

- [Customizing models](../../../docs/customizing-models.md)
- [Models: computing node](../../../docs/models.md)
- [Architecture](../../../docs/architecture.md)
