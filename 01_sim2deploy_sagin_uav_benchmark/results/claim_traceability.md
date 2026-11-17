# Claim traceability (P1)

| Claim | Artifact | Evidence |
|---|---|---|
| Architecture and algorithm choice dominate MEC success under mobility and deadlines. | `tables/scale.tex, figures/f1_success_vs_devices` | Mean success ratio differs across architectures. |
| Cloud/backhaul offloading shifts failures and increases WAN usage. | `figures/f3_failure_causes, figures/f5_wan_usage` | Failure composition and WAN time. |
| Mobility model changes the success boundary. | `tables/mobility.tex, figures/f6_mobility` | Mobility main effect. |
| Trace-replayed production arrivals differ from synthetic bursts. | `tables/trace.tex, figures/f7_trace_vs_synthetic` | Trace versus synthetic delay. |
| Link capacity and WAN latency interact non-linearly. | `figures/f8_sensitivity` | Success ratio heatmap. |
| The benchmark is cheap enough to be routinely re-run. | `figures/f9_runtime` | Wall-clock runtime versus scale. |
