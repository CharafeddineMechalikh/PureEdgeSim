---
description: "Use PureEdgeSim for publishable edge computing research: experiment design, scenario sweeps, randomness, validation, statistics, and reproducibility."
---
# Research guide

This guide is about using PureEdgeSim to produce results that survive review. It covers experiment design, randomness, scenario sweeps, validation, statistics, and the artifact you should keep. It assumes that you understand the simulator's architecture and metrics; if not, read [Architecture](architecture.md) and [Output and metrics](output-and-metrics.md) first.

## Start from the question, not from the simulator

Write down the claim you want to test before touching a configuration file. Most claims in edge computing papers fall into a few shapes:

- **Policy comparison:** algorithm A achieves lower delay or energy than algorithm B under the same infrastructure.
- **Architecture comparison:** cloud-only versus edge-assisted placement changes the delay/energy trade-off.
- **Sensitivity:** the benefit of a policy grows or shrinks with device count, task rate, mobility, bandwidth, or battery capacity.
- **Robustness:** a policy degrades gracefully when bandwidth drops, devices die, or tasks become larger.

Each claim maps to a different experimental design. A policy comparison holds infrastructure and workload fixed. A sensitivity study varies one factor and reports the trend. A robustness study introduces failures and reports the tail, not only the mean.

## Map the claim to a model change

| Claim involves | Change |
| --- | --- |
| Placement decision | A custom orchestrator, see [Writing orchestrators](writing-orchestrators.md) |
| Workload shape | Distributions in `applications.xml`, a custom arrival pattern, or a custom task generator |
| Device capacity or scheduling | Per-instance distributions in the device XML, a custom computing node, or tuned datacenter and device XML |
| Network behavior | A custom network model or link, or tuned bandwidth and latency |
| Mobility | One of the nine built-in models selected per device type, or a custom mobility model |
| Energy behavior | A custom energy model |

Change as little as possible. If the claim is about placement, keep the default networking and energy models and replace only the orchestrator. A result that depends on three simultaneous model changes is hard to attribute and hard to reproduce.

## Build the scenario matrix

The scenario sweep is the Cartesian product of the architecture list, the algorithm list, and the device counts:

```text
scenarios = architectures x algorithms x deviceCounts
```

With the defaults that is 7 x 2 x 3 = 42 simulations. Each simulation reuses the same settings files and differs only in the scenario key, so the comparison is controlled by construction.

Practical choices:

- **Fix what you are not studying.** If the claim is about algorithms, use one architecture unless architecture interaction is part of the claim.
- **Include a baseline.** `ROUND_ROBIN` is a useful naive baseline, and `CLOUD_ONLY` with `TRADE_OFF` is a useful architecture baseline.
- **Sweep enough device counts to see a trend.** Three to five points is usually the minimum for a line chart. The default 100, 200, 300 is only an example.
- **Choose the horizon from the system, not from convenience.** The simulation must be long enough that queueing and battery effects reach a steady state, and long enough that most tasks complete. Tasks in flight at the horizon are censored, as explained in [Output and metrics](output-and-metrics.md).
- **Do not compare different horizons.** A 10-minute run and a 60-minute run have different numbers of tasks and different battery states. Fix the horizon across compared scenarios.
- **Keep the workload proportional.** Increasing device count while keeping `simulation_time` fixed keeps per-device workload constant, which isolates the effect of scale. Increasing the task rate instead answers a different question.

## Parallel or sequential

`parallel_simulation=true` distributes scenarios across worker threads. It is the right mode for collecting data because it uses all available cores. Two consequences:

- Each worker writes `Parallel_simulation_<id>.csv`, so you must merge the files before analysis.
- Aggregate chart generation is disabled in parallel mode. Run a separate sequential sweep for the publication figures, or plot the merged CSV yourself.

`parallel_simulation=false` writes a single `Sequential_simulation.csv` and generates the aggregate charts. Use it for interactive exploration and for the final figure run.

Do not run several simulations in the same JVM with shared static state in custom models. The engine itself isolates scenarios by creating fresh models, but your own static fields would not be isolated.

## Randomness and repetitions

Task arrival times, task sizes, node capacities, device positions and movement are stochastic. Every draw goes through one service, `Randomness`, which uses a strong secure random generator unless the `random_seed` parameter of `simulation_parameters.properties` is set. With a seed, the whole run becomes reproducible: the same configuration replays the same task list, the same capacities and the same paths.

What this means for your experiment:

- **One run per scenario is not evidence.** A single seeded run is one draw from a distribution, not the distribution. Report the number of repetitions.
- **Use at least 10 to 30 repetitions per scenario** for means, and more when the metric is heavy-tailed, such as first death time or tail delay.
- **Vary the seed across repetitions.** `random_seed=42` for all ten repetitions gives ten copies of the same experiment, with zero spread. Keep a fixed seed while you debug, then change it (or leave it empty) when you measure.
- **Report a measure of dispersion.** Mean with confidence interval, or median with interquartile range when the distribution is skewed. Delay and energy are often skewed.
- **You can now pair repetitions across configurations.** Give each repetition its own seed and run every condition with that same seed. The pairing removes the seed-to-seed variance from the comparison, which is the difference between a noisy and a usable result when the effect is small.
- **Parallel runs are handled for you.** Each scenario restarts the random stream from the seed mixed with its position in the scenario list, so parallel scenarios do not replay each other's draws, while the same scenario index keeps the same stream across configurations. That is what makes a device-count sweep usable: the fifth scenario of configuration A sees the same task lengths as the fifth scenario of configuration B.
- **The stream depends on the scenario list, not only on the seed.** Changing `edge_device_counter_size`, the number of devices, or the batch order shifts which index a configuration lands on, and with it its draws. Record the scenario list together with the seed.
- **State the stochastic model.** "Lognormal task lengths with mean 5000 MI, Poisson arrivals, Gauss-Markov mobility, seed 20260425, 20 repetitions" is a fully reproducible description.

Keep the seed in the settings file you archive with your results, together with the simulator version, the configuration files, and the CSV outputs. Published numbers whose seed was not recorded cannot be re-run by anyone else, including you.

## Validation before measurement

A simulator result is only credible if the simulator itself behaves as expected. Before collecting data, run a few sanity checks:

- **Zero-task run.** Set `simulation_time` very small or the task rate to zero. The run should finish without exceptions, and the CSV should contain one row with zero or `NaN` averages. If it crashes or produces a plausible-looking nonzero row, something is wrong.
- **Cloud-only monotonicity.** With `CLOUD_ONLY`, increasing the device count should not reduce average delay or energy. If it does, check whether the task count is changing at the same time.
- **Mist-only locality.** With `MIST_ONLY`, tasks should not use WAN except for container downloads when the registry is enabled. If WAN usage is high, check the registry mode and the architecture name.
- **Bandwidth sensitivity.** Lowering WAN bandwidth should increase WAN usage time and delay for cloud-bound tasks. With `realistic_network_model=false`, each transfer still computes its delay from its bandwidth share but completes in one step, so the effect is weaker and temporal congestion is absent. Compare the two modes before choosing one for the final runs.
- **Battery death.** With a small battery capacity, devices should die and the death count should rise as the capacity falls. A zero death count with a tiny battery usually means the devices are not battery powered in the XML.
- **Orchestrator sensitivity.** A policy that always chooses the cloud should produce the same per-level task distribution as `CLOUD_ONLY`, but not necessarily the same delay, because the architecture restricts the candidate list differently.

If a sanity check fails, stop and fix the model or configuration before running the full matrix. Do not average away a structural bug.

## Statistical analysis

A simple, defensible analysis pipeline:

1. **Merge** all `Parallel_simulation_*.csv` files into one table with a `run` identifier.
2. **Clean** the table: strip whitespace from headers, replace `NaN` and `Infinity` with missing values, and convert columns to numeric types.
3. **Derive** success rate, completion rate, throughput, and energy per successful task using the formulas in [Output and metrics](output-and-metrics.md). Do not use raw column 9 as a success count.
4. **Check** the number of rows per scenario. It should equal the number of repetitions. A missing row means a crashed or skipped run.
5. **Summarize** each scenario with mean and 95 percent confidence interval, or median and IQR. Report the sample size.
6. **Compare** conditions using a test appropriate to the distribution. For skewed metrics, bootstrap the difference in means or use a rank-based test. Report effect sizes, not only p-values.
7. **Plot** trends with uncertainty bands. A line without error bars is a claim without evidence.

If you compare many scenarios, say so and correct for multiple comparisons, or frame the analysis as exploratory. Reviewers notice unadjusted pairwise tests across dozens of conditions.

## Common pitfalls

- **Treating execution delay as end-to-end delay.** The CSV execution columns contain CPU time only. Network time is separate and not reported. Use a custom logger if the claim is about latency.
- **Trusting the success column.** It is computed as sent minus failed, and failures include tasks that were never sent. Derive success from the generated count and the failure breakdown.
- **Ignoring censoring.** Tasks still in flight at the horizon are neither successful nor failed. Short horizons bias success down and can bias delay up or down depending on which tasks finish.
- **Adding energy categories.** WAN/MAN/LAN and WiFi/LTE/Ethernet overlap. Node energy and link energy are different boundaries. Pick one and say which.
- **Leaving the realistic network model off.** With `realistic_network_model=false`, transfers complete in one step. They still use bandwidth to compute their delay, but they do not model congestion over time. If the claim involves congestion or sustained load, the realistic model must be on.
- **Leaving application placement on without noticing.** The orchestrator is called when a device has no placement, and later tasks reuse the decision. A policy that would make different per-task choices is not exercised unless placement is cleared or the orchestrator overrides the caching.
- **Comparing a custom model against a default with different settings.** Change one thing at a time. If the custom run also changes the registry, the number of devices, or the simulation time, the comparison is confounded.
- **Reporting device-count effects with a fixed total workload.** If the task generator produces proportional workload, delay reflects scale. If it produces a fixed total workload, delay may fall with more devices. State which one you use.
- **Using wall-clock time as a simulated result.** Simulation speed is a property of your machine and model complexity. Never mix it into a performance claim about the simulated system.

## Reproducibility checklist

Before you submit or publish, make sure you can answer these questions:

- Which version or commit of PureEdgeSim was used?
- Which settings folder was used, and are all five files included in the artifact?
- Which Java and Maven versions were used?
- Which custom classes were loaded, and where are their sources?
- How many repetitions per scenario?
- Which `random_seed` was used, and was it varied across repetitions or left empty?
- Which probability distributions replaced the default fixed values, and with which means?
- How were runs parallelized?
- How were `NaN`, `-1` death times, and in-flight tasks handled?
- What is the simulation horizon, and why is it long enough?
- Which energy boundary is reported: computing nodes, links, or both?
- Are the raw CSV files included, not only the plots?
- Is the orchestration algorithm description precise enough to reimplement, including tie-breaking and state?

Keep one directory per experiment:

```text
experiments/2026-10-24-policy-comparison/
├── settings/                  # the five input files
├── src/                       # custom classes, if any
├── raw/                       # unmodified CSV and text reports
├── analysis/                  # scripts that produce the tables and figures
├── figures/                   # generated figures
└── README.md                  # version, commands, hardware, date
```

The raw files are the primary record. Analysis scripts should be rerunnable from the raw files without a simulator run, so that a reviewer can check a calculation.

## When to extend the simulator

Extend the simulator when the question requires a mechanism it does not model, not when a parameter is merely inconvenient. For example:

- A new placement policy: custom orchestrator.
- A new workload trace: custom task generator.
- A new wireless technology: custom link and energy model.
- A different failure process: custom simulation manager or orchestrator state.

If the mechanism already exists, use it. A custom model that reimplements default behavior is harder to review and more likely to diverge from the documented simulation.

When you extend, follow [Customizing models](customizing-models.md) and add tests. A custom orchestrator that is not tested against a known baseline is a source of unexplained results.

## Publication and citation

If you use PureEdgeSim in a paper, cite the framework papers listed in the repository README rather than the GitHub URL. Describe the exact version and the model changes. State the stochastic assumptions and the number of repetitions. Include the raw results or a link to them.

The strongest methodology section is short and specific: what was simulated, what was varied, what was fixed, how many runs, and which metrics were derived from which fields.

## Where to go next

- Configure the matrix: [Configuration](configuration.md).
- Interpret each field: [Output and metrics](output-and-metrics.md).
- Implement the policy or mechanism: [Writing orchestrators](writing-orchestrators.md) and [Customizing models](customizing-models.md).
