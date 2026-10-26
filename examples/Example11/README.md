# Example 11: Q-learning orchestration

## What it shows

How to learn the placement policy instead of writing it. The orchestrator keeps
a table of action values. A state is the application type, a coarse bucket of the
task length, and a coarse bucket of the pending load per node of a tier. An
action is a tier: cloud, edge, or mist. The chosen tier executes the task on its
least loaded eligible node, scored like `TRADE_OFF`.

When the task returns, `resultsReturned` receives it with its measured total
delay. A successful task gives a reward equal to the negative delay, a failed one
a large penalty. The update is the standard Q-learning rule, with the next state
rebuilt after the task leaves the tier:

```java
values[action] += LEARNING_RATE
        * (reward + DISCOUNT_RATE * maxValue(nextState) - values[action]);
```

The policy is epsilon-greedy, so one decision in ten explores a random tier. The
three application types of the default settings (hard, soft, and non real-time)
give the agent states that differ in their latency requirement.

## The scenario

The files in `settings/` describe:

- 100 devices on the default map, five simulated minutes;
- the `ALL` architecture, so the three tiers are candidates;
- `Q_LEARNING` as the only algorithm in the sweep;
- a fixed seed of 42, no charts, and no log file;
- the default applications, with their hard, soft, and non real-time types.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example11"
```

## What to look for

With the same seed and scenario, the two policies produce:

| Policy | Average delay (s) | Succeeded | Failed (delay) | Failed (mobility) | Cloud tasks | Mist tasks |
| --- | --- | --- | --- | --- | --- | --- |
| `TRADE_OFF` | 0.1735 | 4205 | 1207 | 138 | 465 | 2485 |
| `Q_LEARNING` | 0.1566 | 4182 | 1343 | 25 | 817 | 1320 |

- The learned policy lowers the average delay by about 10 percent. It sends more
  tasks to the cloud and fewer to mist devices, which is where most mobility
  failures come from.
- The number of successful tasks is slightly lower: the agent optimizes the
  delay of the tasks it completes, not the count of tasks that meet their
  deadline. A different reward would trade one for the other.
- The values are learned during the run, so five simulated minutes are enough to
  show a difference but not to converge. Raise `simulation_time` or lower
  `EXPLORATION_RATE` to give the agent more experience.
- The table is small: 3 application types times 4 length buckets times 3 load
  buckets times 3 actions.
- Results go to `output/Example11_output/`.

## The paper behind the scenario

The example follows the reinforcement-learning approach to workload
orchestration evaluated on PureEdgeSim by:

- Safavifar, Z., Ghanadbashi, S., & Golpayegani, F. (2021). *Adaptive workload
  orchestration in pure edge computing: A reinforcement-learning model.* 2021
  IEEE 33rd International Conference on Tools with Artificial Intelligence
  (ICTAI), 856-860. https://doi.org/10.1109/ICTAI52525.2021.00137
- Safavifar, Z., Gyamfi, E., Mangina, E., & Golpayegani, F. (2024).
  *Multi-objective deep reinforcement learning for efficient workload
  orchestration in extreme edge computing.* IEEE Access, 12, 74558-74571.
  https://doi.org/10.1109/ACCESS.2024.3405411

This is a compact tabular agent with a delay reward, not the deep or
multi-objective models of those papers. The scenario, the action space, and the
parameters are ours; the papers motivate the learning formulation and the use of
PureEdgeSim for its evaluation.

## Files

- `src/main/java/com/mechalikh/pureedgesim/examples/Example11.java`
- `src/main/java/com/mechalikh/pureedgesim/examples/Example11QLearningOrchestrator.java`
- `settings/` with `orchestration_algorithms=Q_LEARNING`

## Where to go next

- [Writing orchestrators](../../docs/writing-orchestrators.md) for the callback
  used to observe completed tasks
- [Example 8](../Example8/README.md) for a fuzzy-logic policy that does not learn
- [Example 10](../Example10/README.md) for a genetic search over service graphs
