# 01 - From Simulation to Deployment: Reproducible Benchmark for UAV- and SAGIN-Integrated Edge Computing

## Contents
- `paper/main.tex`: LaTeX draft paper.
- `paper/research_plan.md`: objectives, expected results, implementation checklist, metrics, acceptance criteria, and figure plan.
- `scenario/`: Feasibility scenario using latest PureEdgeSim (Python) from GitHub (`_deps_pureedgesim_python`).

## Run feasibility scenario
From repository root:
```powershell
$env:PYTHONPATH = "_deps_pureedgesim_python/src"
python 01_sim2deploy_sagin_uav_benchmark/scenario/run_scenario.py
```
Outputs: console summary, CSV/logs under `scenario/output/`, and `scenario/output/feasibility_summary.json`.
