# Example 22: devices that cluster themselves

## What it shows

This scenario adds no central cluster manager. Each device scores itself from
four local signals, points to the best neighbor it can reach, and the clusters
emerge from those pointers. The example connects two extension points:

| Extension point | Class |
| --- | --- |
| Computing node | `Example22SelfClusteringDevice` |
| Task orchestrator | `Example22ClusterOrchestrator` |

The computing node carries the election and its periodic refresh. The
orchestrator reads the resulting clusters and places each task inside the
cluster of the origin device. Neither class needs a change in the framework;
both are registered with `setCustomComputingNode` and
`setCustomEdgeOrchestrator`.

## How the election works

A device scores itself with four weighted terms:

| Signal | Weight | Detail |
| --- | --- | --- |
| Capacity | 0.40 | total MIPS divided by the most powerful device of the fleet |
| Battery | 0.25 | remaining percentage, or 1 for a mains-powered device |
| Stability | 0.20 | 1 for a fixed device, decreasing with speed, 0 at 10 m/s |
| Neighborhood | 0.15 | devices within `edge_devices_range`, saturating at 10 |

Each device then compares its score with every neighbor within range and points
to the best one, itself included. Ties go to the lower device identifier. A
device that points to itself is a cluster head. Every other device follows the
pointers until it reaches a head, so a device can join a head through one or
more intermediate devices. Dead devices and sensors are left out.

The first alive device acts as coordinator for a round: it runs the election in
one pass and writes the result into every device. If it dies, the next alive
device takes over on the following round. Rounds happen every 30 seconds, and
no message is exchanged to decide membership, which is why the scheme is called
self-organizing.

## How tasks are placed

The orchestrator considers the head, the members of the head's cluster that the
origin can reach, and the origin itself. Each candidate costs:

```text
transfer latency + (1 + pending tasks) * task length / capacity
```

The transfer term is the wireless latency plus the time to send the task over
the device-to-device link. The head then receives a 25 percent discount, so at
equal capacity the head wins, and a much faster member still wins over a slow
head. Tasks placed on the origin cost no transfer, which is why the origin often
keeps latency-sensitive work: a hard real-time task has 20 milliseconds to
spend, and 5 milliseconds of Wi-Fi latency is a quarter of that budget.

## The scenario

`settings/` starts from the mist-only fleet of Example 14: 200 devices on a
500 by 500 meters map, a 100 meter mist range, and a five minute run. All three
device types generate and execute tasks; there are no sensors in this fleet.
Orchestrators are disabled, so each device consults its own cluster without an
extra network hop. The seed is 71.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example22"
```

## What to look for

At the end of the run the console prints two lines:

```text
Example22 - cluster placement: 3051 placements, 18.6 % to heads, 2.0 % to members, 79.4 % local, 0.0 % outside the cluster
Example22 - clustering: 10 clusters over 200 devices, 11 rounds, average size 20.0, largest 33
```

A placement is an orchestration decision. Once an application is placed on a
device, its later tasks go to the same device directly, so the placement count
is smaller than the number of tasks. The CSV under `output/Example22_output/`
holds the usual latency, success, network and energy columns.

## Measured effect

Both runs use the same seed, fleet, duration and architecture. The baseline
keeps the clustering device but places tasks with the built-in `TRADE_OFF`
policy, an ablation that isolates the placement rule.

| Metric | `TRADE_OFF` | Cluster placement | Change |
| --- | --- | --- | --- |
| Tasks successfully executed | 77.83 % | 79.94 % | +2.11 points |
| Failed due to latency | 22.10 % | 20.00 % | -2.10 points |
| Average task execution time | 0.2314 s | 0.1332 s | -42 % |
| Total task execution time | 3352 s | 1931 s | -42 % |
| Network usage | 106.53 s | 81.14 s | -24 % |
| Transferred data | 17,311 MB | 13,186 MB | -24 % |

The cluster policy keeps most tasks on the device that generated them (79.4
percent), sends the rest to the stronger nodes that the election promoted, and
cuts both compute time and radio time. The gain is modest in success rate but
clear in execution time and traffic, which is the intended trade-off of putting
a fast head at the center of each neighborhood.

The 200 devices form 10 clusters of 20 devices on average, the largest holding
33. With a 100 meter range on a 500 by 500 meters map, that is roughly one head
per neighborhood, which is what the degree and capacity terms should produce.

## Tune it

- Change the weights in `Example22SelfClusteringDevice`: capacity-heavy
  elections promote laptops, battery-heavy elections keep mobile phones alive
  longer.
- Change `CLUSTER_UPDATE_INTERVAL` to see how stale membership affects
  placement: shorter rounds react faster to mobility and cost more scans.
- Change `HEAD_COST_FACTOR` to 1.0 to remove the head discount, or below 0.5 to
  concentrate even more work on the heads.
- Change `edge_devices_range` to grow or shrink the neighborhoods and therefore
  the number of clusters.

## Files

- `Example22.java`
- `Example22SelfClusteringDevice.java`
- `Example22ClusterOrchestrator.java`
- `examples/Example22/settings/`

## References

The election follows the decentralized, self-organizing, spatial approach of:

> R. Casadei and M. Viroli, "Coordinating Computation at the Edge: a
> Decentralized, Self-Organizing, Spatial Approach," 2019 Fourth International
> Conference on Fog and Mobile Edge Computing (FMEC), Rome, Italy, 2019,
> pp. 60-67, doi: 10.1109/FMEC.2019.8795355.

That work partitions the edge into areas governed by an elected manager and
sets up coordination flows between managers and peripheral nodes. This example
keeps the elected-manager idea and implements the election with local scores, a
30 second refresh, and a placement rule that prefers the elected head.

## Where to go next

- [Example 7](../Example7/README.md) for cluster heads that also cache
  containers
- [Customizing models](../../../docs/customizing-models.md)
- [Writing orchestrators](../../../docs/writing-orchestrators.md)
