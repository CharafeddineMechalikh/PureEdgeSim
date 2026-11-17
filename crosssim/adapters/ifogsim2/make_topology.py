"""Generate the iFogSim2 topology JSON from the common scenario specification.

Three sensors (one per class) attach to the first edge replica; three actuators
receive the class results. One additional edge replica and one cloud node give
the controller placement choices. Capacities and link latencies come from
crosssim/scenario.json.
"""
from __future__ import annotations

import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
CROSSSIM = HERE.parents[1]
SCN = json.loads((CROSSSIM / "scenario.json").read_text(encoding="utf-8"))

nodes = []
links = []
net = SCN["network"]

# cloud
nodes.append({"name": "cloud", "type": "FOG_DEVICE", "level": 0,
              "mips": SCN["cloud_tier"]["mips"], "ram": SCN["cloud_tier"]["ram_MB"],
              "upBw": int(net["wan_bandwidth_Mbps"] * 1e6),
              "downBw": int(net["wan_bandwidth_Mbps"] * 1e6), "ratePerMips": 0.0})
# three edge replicas
for index in range(SCN["edge_tier"]["n_datacenters"]):
    nodes.append({"name": f"edge-{index}", "type": "FOG_DEVICE", "level": 1,
                  "mips": SCN["edge_tier"]["mips"], "ram": SCN["edge_tier"]["ram_MB"],
                  "upBw": int(net["wifi_bandwidth_Mbps"] * 1e6),
                  "downBw": int(net["wifi_bandwidth_Mbps"] * 1e6), "ratePerMips": 0.0})
    links.append({"source": f"edge-{index}", "destination": "cloud",
                  "latency": net["wan_latency_s"] * 1000.0})
for index in range(1, SCN["edge_tier"]["n_datacenters"]):
    links.append({"source": "edge-0", "destination": f"edge-{index}",
                  "latency": net["man_latency_s"] * 1000.0})
# Sensors and actuators are created by the Java adapter (exact trace arrivals
# require a custom Sensor subclass), so the JSON topology contains only compute
# nodes and their links.

target = HERE / "config" / "topology.json"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps({"nodes": nodes, "links": links}, indent=2), encoding="utf-8")
print(f"wrote {target}: {len(nodes)} nodes, {len(links)} links")
