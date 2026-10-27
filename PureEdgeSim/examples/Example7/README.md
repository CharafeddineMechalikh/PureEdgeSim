# Example 7: clustering and cooperative container caching

## What it shows

How to use two extension points together to change how containers reach the
edge. The example implements the cooperative caching algorithm from:

> Mechalikh, C., Taktak, H., Moussa, F. Towards a Scalable and QoS-Aware Load
> Balancing Platform for Edge Computing Environments. HPCS 2019, 684-691.

Two custom classes cooperate:

- `Example7CachingDevice` extends the clustering device, so nearby devices form
  clusters with a cluster head, and tracks which application containers each
  device already holds;
- `Example7CustomNetworkModel` overrides the transfer completion of the default
  network model: with the registry in `CACHE` mode it pulls a container when a
  request arrives, keeps replicas on devices, and serves later requests from a
  nearby cache instead of the cloud registry.

The cluster head is the orchestrator (`deploy_orchestrator=CLUSTER`). It decides
where the tasks of its members run, which is why the two mechanisms belong in
one example: a cache is useful only if the tasks that need it stay in the
cluster.

```mermaid
sequenceDiagram
    participant Dev as Requesting device
    participant CH as Cluster head<br/>orchestrator
    participant Cache as Cluster cache<br/>on a nearby device
    participant Reg as Cloud registry
    Dev->>CH: request, application k
    CH->>Dev: pick the destination inside the cluster
    alt container already in the cluster
        CH->>Cache: fetch replica over Wi-Fi
        Cache-->>Dev: LAN container transfer
    else first request for that container
        CH->>Reg: pull container over the WAN
        Reg-->>Cache: replica stored on a cluster device
    end
    Cache-->>Dev: task executes on the mist destination
    classDef wan fill:#f3c9c9,stroke:#333;
    class Reg wan;
    classDef lan fill:#cfe8ff,stroke:#333;
    class Cache lan;
```

## The scenario

The files in `settings/` describe:

- 300 devices on the default map, 60 simulated minutes;
- the `MIST_ONLY` architecture, so no task goes to the cloud and the WAN is used
  only by container downloads;
- the registry enabled in `CACHE` mode;
- orchestrators enabled on `CLUSTER`;
- the `ROUND_ROBIN` algorithm, kept simple so the caching effect is not
  confounded by a load-balancing heuristic.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example7"
```

## What to look for

The shipped matrix has one row, `MIST_ONLY / ROUND_ROBIN / 300 devices`, 60
simulated minutes, and 25 seconds of wall clock. Its numbers are the CACHE
side of the paper's comparison; the CLOUD side is not in this capture.

| Metric | Value |
| --- | --- |
| Tasks generated, sent, successful | 182700, 100 % of generated, 180396 (98.7389 %) |
| Failed on delay, on mobility, on device death | 1048 (0.5736 %), 1256 (0.6875 %), 0 |
| Average execution, waiting, request makespan | 0.1322 s, 0 s, 0.1448 s |
| Tasks placed on cloud, edge, mist | 0, 0, 182700 |
| Network usage total, WAN time, LAN time | 2079.3657 s, 14.3403 s (0.6896 %), 2065.0254 s (99.3104 %) |
| Container share of WAN time, share of LAN time | 14.3403 s (100 %), 915.5524 s (44.3361 %) |
| Total traffic, average bandwidth per task | 335879.55 MBytes, 1299.5153 Mbps |
| Energy computing nodes, WAN, MAN, LAN | 601.3353 Wh, 23.6753 Wh, 0 Wh, 309.4299 Wh |
| Energy by technology, WiFi, cellular | 309.5224 Wh, 23.5829 Wh |
| Dead devices, remaining battery | 0, 94.2706 % (32.0896 Wh) |

Reading the run against the point of the example:

- The cache is warm and it is doing work. Every second of the 14.3403 s of WAN
  time is a container download (100 % of WAN), and 44.3361 % of the 2065.0254 s
  of LAN time is serving those cached replicas to nearby devices, 915.5524 s.
  Under `registry_mode=CLOUD` the same container traffic would have to cross
  the WAN, so a fair comparison would show that 915.55 s of LAN container time
  move to the WAN side. That row is not in this capture.
- The scenario keeps tasks on the mist tier so the WAN signal is only
  containers. 182700 tasks, 100 % executed on mist, 0 on edge, 0 on cloud. The
  MIST_ONLY architecture is what makes the WAN columns readable, and the
  `edge_devices_range=20` m on a 100 by 100 m map keeps devices close enough
  that cluster heads can serve replicas over the LAN.
- The caching does not buy end-to-end latency in this run. The average request
  makespan is 0.1448 s and the average execution delay is 0.1322 s, and the
  residual losses are 1048 delay failures (0.5736 %) and 1256 mobility failures
  (0.6875 %). What the algorithm buys is the shape of the WAN column: total
  computing energy is 601.3353 Wh over 60 simulated minutes, only 23.6753 Wh
  (3.94 %) of which sits on the WAN, because the cached replicas stay local
  and only first-miss downloads cross it.
- Cluster heads work at scale. 300 devices, `deploy_orchestrator=CLUSTER`,
  round robin, no queueing (average waiting time is 0 s), and only 1256
  mobility failures out of 182700 tasks. The cluster head is chosen once per
  device group, so most requests reach the orchestrator without crossing the
  WAN.
- Battery is the cost of a 60 minute run. Devices end at 94.2706 % average
  remaining charge (32.0896 Wh), 0 dead. Nothing in the run demonstrates
  battery exhaustion, only the drain from a long run.

To complete the paper's comparison, edit `settings/simulation_parameters.properties`
and switch `registry_mode` to `CLOUD` with everything else fixed (same seed,
same device count), then rerun and read the new row against these numbers. The
current table of expected effects is only the CACHE column of that comparison.
Results go to `PureEdgeSim/examples/Example7/output/`.

## Files

- `Example7.java`
- `Example7ClusteringDevice.java`
- `Example7CachingDevice.java`
- `Example7CustomNetworkModel.java`
- `settings/` with the cache and cluster configuration

## Where to go next

- [Customizing models](../../../docs/customizing-models.md)
- [Writing orchestrators](../../../docs/writing-orchestrators.md)
- [Output and metrics](../../../docs/output-and-metrics.md)
