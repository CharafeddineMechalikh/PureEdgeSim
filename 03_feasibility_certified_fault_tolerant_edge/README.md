# 03_feasibility_certified_fault_tolerant_edge

## Contents
- paper/main.tex: LaTeX draft paper for this research direction.
- scenario/: Feasibility scenario built on latest PureEdgeSim (Python, cloned from GitHub in workspace _deps_pureedgesim_python).

## Run feasibility scenario
From repository root:
`powershell
$env:PYTHONPATH = "_deps_pureedgesim_python/src"
python 03_feasibility_certified_fault_tolerant_edge/scenario/run_scenario.py
`
Outputs: console summary, CSV/logs under scenario/output/, and scenario/output/feasibility_summary.json (where applicable).
