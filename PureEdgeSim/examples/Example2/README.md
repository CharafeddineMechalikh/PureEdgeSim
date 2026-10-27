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

```mermaid
sequenceDiagram
    participant SM as SimulationManager
    participant Dev as Mobile edge device
    participant MM as Example2CustomMobilityModel
    participant Net as Network model
    participant Orch as Orchestrator
    SM->>Dev: every update_interval (1 s)
    Dev->>MM: getNextLocation(currentLocation)
    MM-->>Dev: (x+1, y+1), wrapped on the 200 by 200 map
    Dev->>Net: distance to the destination changed
    Net-->>Orch: request re-orchestrated or lost
    Note over Orch: mobility failures feed the "Tasks failed (mobility)" column
    classDef mm fill:#cfe8ff,stroke:#333;
    class MM mm;
```

## The scenario

The `settings/` folder of this example is a byte for byte copy of the packaged
defaults in `PureEdgeSim/settings/`. Mobility is enabled per device type in
`edge_devices.xml`, and `update_interval` decides how often the model is asked for
a new position. The default map is 200 by 200 meters, so the diagonal walk crosses
it quickly, which is useful to watch on the live map chart.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example2"
```

## What to look for

The run finishes in `6 minutes, 3 seconds` of wall clock and writes 42 rows to
the sweep CSV. Because the walk is fixed at one meter per tick, its fingerprint
is in the mobility column, and that column is easiest to read on the `MIST_ONLY`
rows where the destination is another device rather than a fixed edge data
center:

| Row | Generated | Successful | Failed on mobility | Failed on delay |
| --- | --- | --- | --- | --- |
| `MIST_ONLY / TRADE_OFF / 100` | 11100 | 3326 (29.964 %) | 360 (3.2432 %) | 1271 |
| `MIST_ONLY / TRADE_OFF / 200` | 22370 | 10898 (48.717 %) | 1331 (5.9499 %) | 2491 |
| `MIST_ONLY / TRADE_OFF / 300` | 33740 | 18102 (53.6515 %) | 2283 (6.7664 %) | 3678 |
| `ALL / ROUND_ROBIN / 300` (last row) | 33740 | 25803 (76.476 %) | 990 (2.9342 %) | 6947 |

Reading the run against the point of the example:

- The custom walk runs and never stalls: across the sweep, mobility failures
  grow monotonically with the device count on `MIST_ONLY`, from 360 at 100
  devices to 2283 at 300, because two straight-line devices fall out of each
  other's 10 meter range at a rate proportional to how many pairs exist.
- Movement costs wireless time, not batteries. On the last row, energy
  totals 100.9536 Wh and splits as 5.2834 Wh on the cloud, 25.2065 Wh on the
  edge tier, and 70.4636 Wh on the mist tier, with only 1.8216 Wh on the WAN
  and 23.2557 Wh on the LAN. Zero devices die, and the average remaining
  battery is 99.5287 %, 32.7791 Wh, which shows the naive walk does not
  consume the battery model. The visible loss is in the two failure columns,
  990 tasks that ran but could not return because the destination moved, plus
  6947 delay failures that reflect the whole workload rather than mobility
  alone.
- The example does not prove a win over the default model. The same sweep with
  `setCustomMobilityModel(...)` removed is not in this capture, so the only
  defensible comparison is inside the run: `MIST_ONLY / TRADE_OFF / 300` fails
  2283 tasks on mobility and 3678 on delay, while `ALL / ROUND_ROBIN / 300`
  fails 990 on mobility and 6947 on delay. The custom walk is a demo of the
  hook, not a claim of better placement.
- The chart window still updates every `charts_update_interval=30` simulated
  seconds, and devices travel in the same north-east direction and re-enter
  from the opposite edge after wrapping.
- Results go to `PureEdgeSim/examples/Example2/output/`.

## Files

- `Example2.java`
- `Example2CustomMobilityModel.java`
- `settings/`, a copy of the packaged defaults in `PureEdgeSim/settings/`

## Where to go next

- [Customizing models](../../../docs/customizing-models.md)
- [Models: mobility](../../../docs/models.md)
