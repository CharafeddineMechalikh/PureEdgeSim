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

## The scenario

This example uses the repository default settings in `settings/`. The point is
the node class, not the workload, so the default applications, map, and device
types are kept.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example4"
```

## What to look for

- The console prints `Event received, you can do any action here` for every node
  when the simulation starts. This line comes from the custom tag.
- Energy and CPU charts still update, because the other events reach the default
  handler through `super.processEvent`.
- Every node type is an instance of the custom class, since the framework
  instantiates that class for clouds, edge data centers, and devices.

Use this pattern to add per-node metrics, custom scheduling, or a state machine
without rewriting the default node model.

## Files

- `Example4.java`
- `Example4CustomComputingNode.java`
- `settings/` at the repository root

## Where to go next

- [Customizing models](../../../docs/customizing-models.md)
- [Models: computing node](../../../docs/models.md)
- [Architecture](../../../docs/architecture.md)
