# Example 30: keeping the subtasks of a service graph off devices that can fail

## What it shows

What happens to a service graph when the devices it runs on fail, and when it
is worth keeping work off them. In PureEdgeSim a subtask that fails takes down
every subtask that waits for it (`FAILED_BECAUSE_DEPENDENCY_FAILED`), so losing
the first stage of a pipeline costs the whole request, while losing the last one
costs one result.

`Example30RiskAwareOrchestrator` gives every node a risk. Cloud virtual machines
and edge data centers carry none. An edge device carries 0.25, plus up to 0.5
when it runs on a battery (more as the battery empties), plus 0.5 when it moves,
capped at 1. The orchestrator does not know about crashes in advance. The
algorithm name in the parameters file selects the rule:

- `LATENCY_GREEDY`: the lowest estimated delay as a share of the subtask's
  latency budget, with the estimator of Example 19;
- `RELIABLE_INNER`: the same, but a subtask that any other subtask waits for
  may only go to a node without risk, when one is allowed;
- `TRADE_OFF`, `ROUND_ROBIN`: the default orchestrator.

Every algorithm, the default ones included, decides once per subtask: the
orchestrator clears the cached placement after each decision, as Example 9
does.

## The scenario

The files in `settings/` describe a 1000 by 1000 meters map with four small
edge data centers (4 cores at 20000 MIPS), one cloud, and three kinds of
devices: phones that send requests and can compute (40%), gateways that only
compute and are faster than an edge data center (30%), and sensors that only
send requests (30%). Three service graphs share the fleet:

| Application | Class | Graph | Requests per device per minute |
| --- | --- | --- | --- |
| Video analytics | Soft real-time | the 7-subtask pipeline of Example 9 | 2 |
| Health monitoring | Hard real-time | ingest, three analyses, fuse, alert | 4 |
| Telemetry | Non real-time | collect, clean, then store and model, report | 1 |

`example30.scenario` picks one of three device files:

- `battery`: phones and gateways run on small batteries, starting between 20
  and 100 percent, and die when they empty;
- `mobility`: phones and gateways walk around the map on mains power;
- `crash`: everything is fixed and mains powered, and
  `Example30FailureSimulationManager` crashes every gateway inside a random
  150 m disk at 120, 240 and 360 s, and takes one edge data center down from
  300 s to 360 s. Phones and sensors do not crash, so the crashes only hit
  work that was placed on gateways.

PureEdgeSim already fails a task whose requesting device is out of range of
its destination when the task arrives. `Example30FailureSimulationManager` adds
the same check when the result comes back from a moving helper device, so a
helper that walks away while it computes loses the result. Both checks apply to
every algorithm.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example30" \
  -Dexample30.scenario=crash -Dexample30.devices=100 -Dexample30.seed=1 \
  -Dexample30.algorithms=RELIABLE_INNER
```

The other options are `-Dexample30.architecture=EDGE_AND_CLOUD`, which keeps
every subtask off the devices, and `-Dexample30.edgeOutage=false`, which keeps
the edge data centers up in the crash scenario. `-Dexample30.out=results.jsonl`
appends the metrics of each run to a file as one JSON line: requests generated
and failed, failure reasons, makespan, energy by layer, dead devices and where
the subtasks ran.

## What to look for

Each run prints one line with the requests that failed. Over 10 seeds and
100, 500 and 1,000 devices, the share of requests that failed was:

| Algorithm | `battery` | `mobility` | `crash` | Subtasks on devices |
| --- | --- | --- | --- | --- |
| `TRADE_OFF` | 0.23% | 0.77% | 0.68% | 92% |
| `LATENCY_GREEDY` | 0.22% | 0.58% | 1.13% | 81% |
| `RELIABLE_INNER` | 0.07% | 0.91% | 2.54% | 15% |
| `LATENCY_GREEDY`, `EDGE_AND_CLOUD` | 0.00% | 0.91% | 2.50% | 0% |

Keeping work off risky devices pays only when devices are what fails. It
removes most battery failures, but it concentrates the work on four edge data
centers. In the crash scenario the outage of one of them costs more than the
gateway crashes it avoided, and in the mobility scenario a phone that walks out
of its data center's coverage loses its task. With the outage switched off,
`RELIABLE_INNER` failed 0.10% of requests in the crash scenario at 1,000
devices, against 0.17% for `LATENCY_GREEDY`.
