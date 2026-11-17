# Reproducing the cross-simulator replication

## Environment

* Windows 10/11, PowerShell 7
* Python 3.12.4, `numpy 2.0.1`, `pandas 2.2.2`, `scipy 1.18.1`, `networkx`,
  `simpy` (YAFS requirement)
* Java 23 (`C:\Program Files\Java\jdk-23`), Maven 3.8.1

## Vendored platforms

| Platform | Repository | Commit |
|---|---|---|
| YAFS | https://github.com/acsicuib/YAFS (branch `YAFS3.1`) | `98425c10109a2098cd07eb2a3d6b48c2110d4baa` |
| EdgeCloudSim | https://github.com/CagataySonmez/EdgeCloudSim | `83969dd15ad1b09bd6ca1b5d0bc84ae4670cd844` |

Both are cloned into `crosssim/vendor/`, which is excluded from the parent
repository. YAFS sources are **unmodified**: `git -C crosssim/vendor/YAFS status
--porcelain` is empty.

## YAFS replication

```powershell
$env:PYTHONPATH = "D:\Charef\doctorat\Work folder\PureEdgeSim\crosssim\vendor\YAFS\src"
python crosssim/yafs_impl/run_yafs.py --grid --seeds 3 --out crosssim/results/yafs
python crosssim/aggregate.py
```

`--grid` runs 3 architectures x 4 temporal settings x 3 seeds = 36 runs per
deadline factor. The deadline-scaled arm (`df16`) is a separate invocation:

```powershell
python crosssim/yafs_impl/run_yafs.py --grid --seeds 3 --deadline-factor 16 --out crosssim/results/yafs
```

Each run writes an event CSV (`ev_*.csv`, YAFS's own output) and appends per-task
records to `results/yafs/tasks_df*.csv`. `aggregate.py` reads only the latter.

## EdgeCloudSim

```powershell
powershell -File crosssim/ecs/run_ecs.ps1 -LogName smoke
```

The shipped tutorial runs to completion (log: `crosssim/ecs/out_smoke_log.txt`).
The three-class scenario was **not** ported: EdgeCloudSim exposes no global
orchestration period, so the artefact cannot exist there. See
`SIMULATOR_SEMANTICS.md`.

## Determinism check

YAFS is a SimPy model and is deterministic given `random.seed(seed)`; the three
seeds produce identical per-cell success ratios to three decimals, which is
visible in `results/replication.csv` (the seed CIs collapse). This is reported
as an observation, not as evidence of correctness: it means the replication's
uncertainty comes from the model, not from sampling noise.

## Files

* `aggregate.py` — produces `results/replication.csv`, `results/replication_effect.csv`, `tables/replication.tex`
* `definitions.md` — the measured quantities
* `FINDINGS.md` — what generalises
* `SIMULATOR_SEMANTICS.md` / `.csv` — the semantics table
* `PORTING_NOTES.md` — fidelity of the port
