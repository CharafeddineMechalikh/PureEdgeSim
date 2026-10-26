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

- The WAN usage and WAN energy columns are the interesting results: with a warm
  cache, later requests avoid the remote registry.
- The container usage columns separate WAN and LAN container downloads.
- Results go to `output/Example7_output/`.

The comparison the paper makes, and the one this example is built for, is:

| Registry mode | Orchestrator deployment | Expected effect |
| --- | --- | --- |
| `CLOUD` | `CLUSTER` | every container download crosses the WAN |
| `CACHE` | `CLUSTER` | replicas serve nearby devices, WAN usage drops |
| `CACHE` | `CLOUD` | caching still helps, but decisions come from the cloud |

Change `registry_mode` and `deploy_orchestrator` in
`settings/simulation_parameters.properties` and rerun. Keep the seed and the
device count fixed, or the comparison mixes several effects.

## Files

- `src/main/java/com/mechalikh/pureedgesim/examples/Example7.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example7ClusteringDevice.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example7CachingDevice.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example7CustomNetworkModel.java`
- `settings/` with the cache and cluster configuration

## Where to go next

- [Customizing models](../../docs/customizing-models.md)
- [Writing orchestrators](../../docs/writing-orchestrators.md)
- [Output and metrics](../../docs/output-and-metrics.md)
