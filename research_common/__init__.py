"""Shared experiment infrastructure for the five 2026 edge-computing research projects.

The package provides:
- a deterministic runner around the latest PureEdgeSim Python checkout,
- per-run provenance and per-task extraction,
- parallel grid execution with resume,
- bootstrap confidence intervals and non-parametric tests,
- publication-quality matplotlib figures and LaTeX tables,
- artifact hashing/manifest helpers.

It is deliberately dependency-light: numpy, pandas, scipy, matplotlib.
"""
from .paths import ROOT, PY_SRC, ensure_pureedgesim

__all__ = ["ROOT", "PY_SRC", "ensure_pureedgesim"]
