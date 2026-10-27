# Example 3: cooperative edge data centers

## What it shows

What changes when edge data centers can work together instead of each one
serving only its own devices. The scenario places four edge data centers at the
corners of a 2000 by 2000 meters map and links them in a ring, so a task can
travel from its device to one edge server, then to another, or to the cloud.

The decision is delegated to the edge tier. `deploy_orchestrator=EDGE` puts an
orchestrator on every edge data center, and a device sends its request to the
closest one. That orchestrator chooses between executing locally, forwarding to
another edge data center, and the cloud, using the TRADE_OFF algorithm.

## The scenario

The files in `settings/` describe:

- four linked peripheral edge data centers with 40000 MIPS and 10 cores each;
- 500 devices, one simulation of ten minutes;
- the `EDGE_AND_CLOUD` architecture, so both tiers are candidates;
- orchestrators enabled on the edge tier;
- no container registry, to keep the delay comparison about placement only;
- a 1000 Mbps WAN and a 1000 Mbps MAN, so moving a task between edge servers
  stays cheaper than a cloud round trip.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example3"
```

## What to look for

- The text report and CSV split the executed tasks between the cloud and the
  edge columns. With this scenario most tasks stay on the edge tier.
- The console shows each device contacting its nearest edge orchestrator.
- Results go to `output/Example3_output/` with charts and a `run.properties`
  file.

Two comparisons make the example useful:

1. Set `enable_orchestrators=false`. Every device decides locally, so no request
   travels to an edge orchestrator first, and the edge servers no longer
   coordinate.
2. Add `CLOUD_ONLY` to `orchestration_architectures`, as the file suggests in a
   comment. The cloud-only run is the baseline for the delay and energy values.

To see the effect of the MAN links, shrink `man_bandwidth` or raise
`man_latency` and rerun: forwarding to a remote edge server becomes less
attractive compared with local execution or the cloud.

## Files

- `Example3.java`
- `settings/` with the four linked data centers and the scenario parameters

## Where to go next

- [Writing orchestrators](../../../docs/writing-orchestrators.md)
- [Configuration: orchestration](../../../docs/configuration.md)
