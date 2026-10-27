# Example 20: multi-objective reinforcement learning

## What it shows

How an agent can optimize two objectives at once without collapsing them into a
single number before learning. The agent keeps two value tables, one for delay
and one for energy, over the same state (application type and task-length
bucket) and the same actions (cloud, edge, mist). It updates each table with its
own reward and combines them only when it chooses, by multiplying each value by
a weight.

The delay reward is the negated delay normalized by the task latency, as in the
other learning examples. The energy reward is the negated estimated energy of
the chosen node, normalized by a reference of 0.01 Wh, so a cloud task that
consumes a hundred times more than a mist task is penalized accordingly. A
failure applies the same penalty to both tables.

This is a compact scenario inspired by multi-objective reinforcement learning
for offloading, not a reproduction of its experiments or of its network
architecture.

## The scenario

The files in `settings/` describe:

- four peripheral edge data centers with 40000 MIPS and 10 cores each, linked in
  a ring over the MAN;
- 300 devices of the default mix on a 2000 by 2000 meters map;
- 5 simulated minutes, seed 53, the `ALL` architecture, and `SCALARIZED_Q`;
- local decisions (`enable_orchestrators=false`);
- a 20 Mbps WAN and a 1000 Mbps MAN, and a cloud that draws 5776 W against 150 W
  for each edge data center.

Both weights are 1, the exploration rate is 0.1, the step size 0.2, and the
discount rate 0.9.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example20"
```

Results go to `output/Example20_output/`.

## What to look for

With seed 53, 16870 tasks are generated and 75.72 percent complete within their
latency, with an average execution delay of 0.1810 s. The edge layer executes
11705 tasks and 9356 of them succeed, the mist layer executes 3736 tasks with
3208 successes, and the cloud receives 1429 tasks of which only 210 succeed.
Energy consumption is 74.0626 Wh, with 1.3912 Wh on the cloud, 38.3144 Wh on the
edge, and 34.3570 Wh on the mist devices.

The cloud share is the interesting part. It receives 1429 tasks, almost exactly
the 10 percent exploration rate: the learned values send essentially no task
there, because the hard real-time tasks miss their 20 ms bound over the WAN and
each failure writes a large negative value into both tables. Two extra runs with
`ENERGY_WEIGHT` set to 10 and to 0 leave the cloud share at 1423 and 1430 tasks,
which shows that the failures, not the weights, set the policy for this
scenario.

To see the weights matter, give the cloud a chance to succeed:

1. Raise the health application latency in `applications.xml` to 0.5 and rerun.
   With cloud tasks no longer failing, the energy value becomes the term that
   decides between a fast and a cheap destination, and changing `ENERGY_WEIGHT`
   shifts the cloud and mist shares.
2. Raise `EPSILON` to 0.3 and rerun. The cloud share should grow towards the
   random share, since the greedy part of the policy avoids the cloud.

## Files

- `Example20.java`
- `Example20MultiObjectiveOrchestrator.java`
- `settings/` with four linked edge data centers and 300 devices

## Where to go next

- [Example 11: tabular Q-learning](../Example11/README.md)
- [Example 19: deadline-aware placement](../Example19/README.md)
- [Writing orchestrators](../../../docs/writing-orchestrators.md)

## References

- Multi-objective task offloading optimization using deep reinforcement
  learning with resource distribution clustering, ICT Express, 2025,
  doi:10.1016/j.icte.2025.05.006
