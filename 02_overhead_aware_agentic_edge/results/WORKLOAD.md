# Workload reconciliation

Every task count that appears in the paper is derived here from the artifact.
The referee found three different counts in the previous draft (2,100, 2,175 and
"20+15 req/min"); this file shows that they are three descriptions of one
workload and states which one is authoritative.

## The authoritative number: 2,100 requests per run

One run simulates **5 minutes** (`duration_min = 5`) of a fleet of **30
devices** running two applications, and generates **2,100 requests**. The count
is not a nominal rate assumed in the text: it is `len(tasks.csv)` for every run
in the `factorial`, `cp_split`, `ablation2`, `cache`, `tiers` and `main` grids.

Read back from `results/runs/factorial/runs/...seed-1.../tasks.csv`
(naive arm, 1x):

| Application | Requests | Share | Deadline | Nominal length | Request size |
|---|---|---|---|---|---|
| `Agentic_Inference_RAG` | 1,200 | 57.1% | 0.80 s | 18,000 MI | 1,200 KB |
| `Agentic_Control_Reasoning` | 900 | 42.9% | 0.25 s | 6,000 MI | 400 KB |
| **Total** | **2,100** | 100% | | | |

Arrivals span 3.0 s to 314.0 s, i.e. the generator covers the whole 300 s
horizon; the extra 14 s are in-flight requests at the stop time
(`wait_for_all_tasks = true`), not a second generation window.

## Why "20+15 req/min" is not 2,100

`applications_agentic.xml` declares rates of **20** and **15 requests per
minute per generating device**, with `usage_percentage = 50` each. The fleet has
30 devices, but five device *types* are declared in `edge_devices.xml` and two
of them (11% + 11% = **22%**) carry `generateTasks = false`; they are the
simulator's own "server" device types. The census is therefore:

```
30 devices x (1 - 0.22) = 23.4 -> 24 generating devices
```

PureEdgeSim assigns each generating device to one application by
`usage_percentage`, so 12 devices run the RAG application at 20/min and 12 run
control at 15/min:

```
12 x 20 + 12 x 15 = 240 + 180 = 420 requests/minute = 7.0 requests/second
7.0 /s x 300 s = 2,100 requests
```

That is the arithmetic behind the observed 1,200 / 900 split: 1,200 = 12 x 20 x 5
and 900 = 12 x 15 x 5.

## Where "2,175" came from

2,175 was the request count of the **older `applications.xml`** profile, whose
rates were 20 and 15 with different usage percentages, and which was used only
before R2.2 removed the profile switch. No grid in this artifact uses that file;
the count survives nowhere in the data. It is recorded here only so that a
reader who has the older draft can see which number was superseded.

## Overhead multipliers

The multiplier is applied to the *nominal* length and request size of every
request, before the class mix matters:

```
length      = nominal x compute_multiplier + control_messages x control_mi
request_kb  = nominal_kb x payload_multiplier + control_messages x control_kb
```

with `control_messages = 3`, `control_mi = 150` and `control_kb = 0.5`. At 2x a
RAG request is therefore 36,450 MI and 2,401.5 KB, and the control request
12,450 MI and 801.5 KB. The `cp_split` grid moves the two multipliers
independently; every other grid moves them together (the published definition of
"overhead").

## Horizon

| `duration_min` | Requests | Per minute |
|---|---|---|
| 5 | 2,100 | 420 |
| 15 | 6,300 | 420 |
| 30 | 12,600 | 420 |

The rate is constant, which is what makes the horizon grid a test of transients
rather than of load. The 60 s warm-up in that grid is excluded at analysis time
(`warmup_s` in the run metadata); it changes no table, because the success ratio
is computed from the task records that the harness already de-duplicates across
the horizon.

## What is *not* modelled

* No subtask DAG: every request is one task. The simulator supports graphs; this
  artifact does not exercise them, and the paper says so.
* No retries and no recovery: `task_retries_total` and `task_recovered_total` are
  zero in every run.
* No container download: `enable_registry = false`. The container term in the
  objective is a *penalty on moving work to a cold host*, not a simulated
  transfer.

## Reproduction

```powershell
python 02_overhead_aware_agentic_edge/scenario/experiment.py --only factorial --seeds 1
python -c "import pandas as pd,glob; print(len(pd.read_csv(glob.glob('02_overhead_aware_agentic_edge/results/runs/factorial/runs/*seed-1*/tasks.csv')[0])))"
```

The second command prints `2100` for every arm.
