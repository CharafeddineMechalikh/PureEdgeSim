"""Render the cross-simulator Mermaid diagrams to PNG (and PDF via pdflatex-ready PNG).

Usage:  python crosssim/diagrams/render.py
"""
from __future__ import annotations

import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent

DIAGRAMS = [
    "application_graph",
    "infrastructure",
    "deployments",
    "framework",
    "calibration",
    "orchestrator",
]


def main() -> None:
    for name in DIAGRAMS:
        source = HERE / f"{name}.mmd"
        target = HERE / f"{name}.png"
        command = [
            "npx.cmd", "--yes", "@mermaid-js/mermaid-cli",
            "-i", str(source), "-o", str(target),
            "-b", "white", "-s", "2",
        ]
        print(f"[diagram] {name}")
        result = subprocess.run(command, capture_output=True, text=True)
        if result.returncode != 0:
            print(result.stderr[-1500:])
            raise SystemExit(result.returncode)
        print(f"  {target.name}: {target.stat().st_size:,} bytes")


if __name__ == "__main__":
    main()
