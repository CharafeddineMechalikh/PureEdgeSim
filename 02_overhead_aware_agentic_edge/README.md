# 02_overhead_aware_agentic_edge

## Contents
- paper/main.tex: LaTeX draft paper for this research direction.
- scenario/: Feasibility scenario built on latest PureEdgeSim (Python, cloned from GitHub in workspace _deps_pureedgesim_python).

## Run feasibility scenario
From repository root:
`powershell
$env:PYTHONPATH = "_deps_pureedgesim_python/src"
python 02_overhead_aware_agentic_edge/scenario/run_scenario.py
`
Outputs: console summary, CSV/logs under scenario/output/, and scenario/output/feasibility_summary.json (where applicable).
