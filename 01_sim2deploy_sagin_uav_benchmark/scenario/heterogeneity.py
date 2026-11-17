"""Infrastructure heterogeneity for the P1 benchmark.

The crossed design so far varies *which tiers are eligible* but holds their
capacity fixed, so an adaptive orchestrator has nothing to exploit.  This
module supplies the missing factor: profiles in which edge and cloud differ in
throughput and in access cost, which is precisely the condition under which
"the algorithm's value should appear" (§3.3 and E2 of the revision plan).
"""
from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

# name -> (edge cores, edge mips per core-group, cloud cores, cloud mips, WAN s)
PROFILES = {
    "uniform": {
        "edge_cores": 16, "edge_mips": 80000, "cloud_cores": 400,
        "cloud_mips": 100000, "wan_latency": 0.12, "wan_bandwidth": 50,
        "note": "both tiers ample; the reference configuration",
    },
    "edge_weak": {
        "edge_cores": 2, "edge_mips": 8000, "cloud_cores": 400,
        "cloud_mips": 400000, "wan_latency": 0.02, "wan_bandwidth": 500,
        "note": "weak edge, powerful cloud over a fast link: cloud is the better host",
    },
    "edge_strong": {
        "edge_cores": 32, "edge_mips": 400000, "cloud_cores": 40,
        "cloud_mips": 40000, "wan_latency": 0.3, "wan_bandwidth": 20,
        "note": "powerful edge, weak and distant cloud: edge is the better host",
    },
    "split_task": {
        "edge_cores": 8, "edge_mips": 60000, "cloud_cores": 200,
        "cloud_mips": 600000, "wan_latency": 0.12, "wan_bandwidth": 200,
        "note": "mixed: light tasks favour the edge, heavy tasks favour the cloud",
    },
}


def _rewrite(path: Path, tag: str, updates: dict[str, object]) -> None:
    tree = ET.parse(path)
    for element in tree.getroot().iter(tag):
        for key, value in updates.items():
            node = element.find(key)
            if node is not None:
                node.text = str(value)
    tree.write(path, encoding="utf-8", xml_declaration=True)


def apply_profile(settings_dir: Path, name: str) -> dict:
    """Rewrite the edge and cloud descriptors and return the profile used."""
    profile = PROFILES[name]
    _rewrite(settings_dir / "edge_datacenters.xml", "datacenter",
             {"cores": profile["edge_cores"], "mips": profile["edge_mips"]})
    _rewrite(settings_dir / "cloud.xml", "datacenter",
             {"cores": profile["cloud_cores"], "mips": profile["cloud_mips"]})
    return profile


def profile_properties(name: str) -> dict:
    profile = PROFILES[name]
    return {"wan_latency": profile["wan_latency"],
            "wan_bandwidth": profile["wan_bandwidth"]}
