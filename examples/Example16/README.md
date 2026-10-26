# Example 16: softmax policy for smart-city offloading

## What it shows

How a reinforcement agent behaves when it samples the offloading layer from a
Boltzmann distribution instead of following an epsilon-greedy rule. Every layer
keeps a value for each application type, task-length bucket, and layer-load
bucket. The sampling probability of a layer grows with its value, so the policy
explores while the values are close and settles on the best layer as they
separate, without a fixed exploration rate.

The scenario is city-scale: 300 devices and four edge data centers on a 2000 by
2000 meters map. Each device decides locally, and the chosen layer is executed
by its least loaded eligible node.

This is a compact scenario inspired by softmax offloading studies for smart
cities, not a reproduction of their experiments.

## The scenario

The files in `settings/` describe:

- four peripheral edge data centers with 40000 MIPS and 10 cores each, linked in
  a ring over the MAN;
- 300 devices of the default mix, including Wi-Fi sensors and battery-powered
  phones, on a 2000 by 2000 meters map;
- 5 simulated minutes, seed 11, the `ALL` architecture, and `SOFTMAX`;
- local decisions (`enable_orchestrators=false`), so no orchestrator delay is
  added to the samples;
- a 20 Mbps WAN and a 1000 Mbps MAN.

The policy uses a temperature of 0.05 over values normalized by the task
latency, a learning rate of 0.2, a discount rate of 0.9, and a failure penalty
of 10. A successful task returns the negated delay divided by its latency, so
the values stay in the same range whatever the task size.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example16"
```

Results go to `output/Example16_output/`.

## What to look for

With seed 11, 16870 tasks are generated and 75.89 percent of them complete
within their latency. The edge layer executes 11577 tasks and 9887 of them
succeed, the mist layer executes 2486 tasks with 2120 successes, and the cloud
receives 2807 tasks of which only 795 succeed. The average execution delay is
0.1693 s and energy consumption is 78.4957 Wh, split between the cloud
(4.9886 Wh), the edge (38.0436 Wh), and the mist devices (35.4636 Wh).

The cloud share is the interesting part: the WAN delay makes cloud samples
return a worse reward, so their values fall and the Boltzmann distribution
sends fewer tasks there over time. That learning happens without tuning an
exploration schedule.

Two changes show the effect of the temperature:

1. Raise `TEMPERATURE` in `Example16SoftmaxOrchestrator` to 0.5 and rerun. The
   distribution stays close to uniform, so the cloud keeps receiving a large
   share of the tasks and the success rate drops.
2. Lower it to 0.01 and rerun. Early samples are close to greedy, so the agent
   commits to the layer that looked best first and converges faster but less
   reliably.

## Files

- `src/main/java/com/mechalikh/pureedgesim/examples/Example16.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example16SoftmaxOrchestrator.java`
- `settings/` with four linked edge data centers and 300 devices

## Where to go next

- [Example 11: Q-learning over cloud, edge, and mist](../Example11/README.md)
- [Example 15: multi-layer guided reinforcement learning](../Example15/README.md)
- [Writing orchestrators](../../docs/writing-orchestrators.md)

## References

- Softmax-based deep reinforcement learning for computation offloading in
  smart cities, doi:10.1016/j.comcom.2025.108105
