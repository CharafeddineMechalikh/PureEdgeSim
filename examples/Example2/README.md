# Example 2: a custom mobility model

## What it shows

How to replace the movement of edge devices. PureEdgeSim calls
`getNextLocation` on the mobility model of every mobile node at each update
tick, and the returned location becomes the node's new position.

`Example2CustomMobilityModel` is deliberately naive: every tick it adds one
meter to both coordinates and wraps around when a node leaves the map. It reads
the current location, which is all a custom model receives, and ignores speed,
pauses, and direction. Real models (random waypoint, Gauss-Markov, Manhattan,
and the others listed in the default `edge_devices.xml`) use that same method
with more state.

## The scenario

This example uses the repository default settings in `settings/`. Mobility is
enabled per device type in `edge_devices.xml`, and `update_interval` decides how
often the model is asked for a new position. The default map is 200 by 200
meters, so the diagonal walk crosses it quickly, which is useful to watch on the
live map chart.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example2"
```

## What to look for

- On the map chart, devices move in the same north-east direction and re-enter
  from the opposite edge after wrapping.
- Distances to edge data centers change, so the device-to-edge links can change
  while the run is in progress.
- A device that moves out of radio range of its current destination triggers a
  new orchestration decision.

Compare with the default model by removing the `setCustomMobilityModel` call:
the movement becomes random and respects the speed and pause values of each
device type.

## Files

- `src/main/java/com/mechalikh/pureedgesim/examples/Example2.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example2CustomMobilityModel.java`
- `settings/` at the repository root

## Where to go next

- [Customizing models](../../docs/customizing-models.md)
- [Models: mobility](../../docs/models.md)
