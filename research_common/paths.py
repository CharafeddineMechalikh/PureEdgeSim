"""Repository paths and import bootstrap."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PY_SRC = ROOT / "_deps_pureedgesim_python" / "src"
PROJECTS = [
    "01_sim2deploy_sagin_uav_benchmark",
    "02_overhead_aware_agentic_edge",
    "03_feasibility_certified_fault_tolerant_edge",
    "04_secure_federated_agentic_mec",
    "05_crosslayer_iscc_mec_6g",
]


def ensure_pureedgesim() -> None:
    """Make the cloned PureEdgeSim Python package importable."""
    if str(PY_SRC) not in sys.path:
        sys.path.insert(0, str(PY_SRC))


def project_dir(name: str) -> Path:
    path = ROOT / name
    if not path.is_dir():
        raise FileNotFoundError(f"Unknown project folder: {name}")
    return path


def results_dir(name: str) -> Path:
    path = project_dir(name) / "results"
    path.mkdir(parents=True, exist_ok=True)
    return path
