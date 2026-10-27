# Example 6: custom settings files and a parallel sweep

## What it shows

Two things that are easy to mix up:

- how to point the simulation at another scenario, either the whole settings
  folder or one file at a time through `setCustomFilePath(..., Files.X)`;
- how a larger sweep behaves when it runs in parallel.

The example sets both. It calls `setCustomSettingsFolder` for the folder, then
sets each of the five files individually, which shows the per-file API without
changing the outcome.

## The scenario

The files in `settings/` were written to make the run observable rather than
fast:

- every architecture is simulated in one sweep, with MIST orchestration and
  container downloads from the cloud registry;
- `realistic_network_model=true`, so transfers progress in steps and share
  links instead of completing instantly;
- `parallel_simulation=true`, so the sweep uses one worker per scenario and
  live charts are disabled automatically;
- `wait_for_all_tasks=false`, so the run stops at `simulation_time` and tasks
  still in flight stay unresolved;
- shorter batteries and higher energy consumption, so devices die during the run
  and the death columns are populated.

## Run it

From the repository root, after `mvn clean install -DskipTests`:

```bash
mvn exec:java -Dexec.mainClass="examples.Example6"
```

## What to look for

- Results go to `output/Example6_output/` and the sweep writes
  `Parallel_simulation.csv`, not `Sequential_simulation.csv`.
- Each row is one architecture. Compare the delay and energy columns across
  them, with the same devices and workload.
- The dead device count and the per-device death times change with the energy
  parameters, not with the orchestration algorithm.
- The network columns reflect the realistic model: WAN, MAN, and LAN usage grow
  over time instead of appearing as one transfer delay per task.
- `run.properties` records the seed and the five resolved input paths, so the
  parallel run remains reproducible.

Remember that parallel mode turns charts off. Run with
`parallel_simulation=false` to watch the map and the CPU charts.

## Files

- `Example6.java`
- `settings/` with the scenario parameters and the higher energy consumption

## Where to go next

- [Configuration](../../../docs/configuration.md)
- [Output and metrics](../../../docs/output-and-metrics.md)
- [Models: energy](../../../docs/models.md)
