# P2 agentic-overhead data dictionary

| Column | Unit | Meaning | Source |
|---|---|---|---|
| `job_id` | - | Derived by the harness | harness |
| `algorithm` | - | Derived by the harness | harness |
| `architecture` | - | Derived by the harness | harness |
| `battery_initial` | - | Derived by the harness | harness |
| `cache_hit_ratio` | - | Derived by the harness | harness |
| `compute_multiplier` | - | Agentic compute and payload multiplier | job |
| `control_messages` | - | Derived by the harness | harness |
| `deadline_margin` | - | Derived by the harness | harness |
| `devices` | - | Derived by the harness | harness |
| `energy_weight` | - | Derived by the harness | harness |
| `experiment` | - | Derived by the harness | harness |
| `fidelity` | - | Derived by the harness | harness |
| `payload_multiplier` | - | Derived by the harness | harness |
| `policy` | - | naive, edge, aware_nofid, aware_flat or aware | job |
| `rate_factor` | - | Derived by the harness | harness |
| `seed` | - | Derived by the harness | harness |
| `run_id` | - | Derived by the harness | harness |
| `duration_s` | - | Derived by the harness | harness |
| `wall_clock_s` | s | Harness wall-clock runtime | harness |
| `Orchestration architecture` | - | Derived by the harness | harness |
| `Orchestration algorithm` | - | Derived by the harness | harness |
| `Edge devices count` | - | Derived by the harness | harness |
| `Total tasks execution delay (s)` | - | Derived by the harness | harness |
| `Average execution delay (s)` | - | Derived by the harness | harness |
| `Total tasks waiting time (s)` | - | Derived by the harness | harness |
| `Average waiting time (s)` | - | Derived by the harness | harness |
| `Number of generated tasks` | - | Derived by the harness | harness |
| `Tasks successfully executed` | - | Derived by the harness | harness |
| `Task not executed (No resources available or long waiting time)` | - | Derived by the harness | harness |
| `Tasks failed (delay)` | - | Derived by the harness | harness |
| `Tasks failed (device dead)` | - | Derived by the harness | harness |
| `Tasks failed (mobility)` | - | Derived by the harness | harness |
| `Tasks not generated due to the death of devices` | - | Derived by the harness | harness |
| `Tasks not executed because a dependency failed` | - | Derived by the harness | harness |
| `Total tasks executed (Cloud)` | - | Derived by the harness | harness |
| `Tasks successfully executed (Cloud)` | - | Derived by the harness | harness |
| `Total tasks executed (Edge)` | - | Derived by the harness | harness |
| `Tasks successfully executed (Edge)` | - | Derived by the harness | harness |
| `Total tasks executed (Mist)` | - | Derived by the harness | harness |
| `Tasks successfully executed (Mist)` | - | Derived by the harness | harness |
| `Network usage (s)` | - | Derived by the harness | harness |
| `Wan usage (s)` | - | Derived by the harness | harness |
| `Lan usage (s)` | - | Derived by the harness | harness |
| `Total network traffic (MBytes)` | - | Derived by the harness | harness |
| `Containers wan usage (s)` | - | Derived by the harness | harness |
| `Containers lan usage (s)` | - | Derived by the harness | harness |
| `Average bandwidth per task (Mbps)` | - | Derived by the harness | harness |
| `Average CPU usage (%)` | - | Derived by the harness | harness |
| `Average CPU usage (Cloud) (%)` | - | Derived by the harness | harness |
| `Average CPU usage (Edge) (%)` | - | Derived by the harness | harness |
| `Average CPU usage (Mist) (%)` | - | Derived by the harness | harness |
| `Energy consumption of computing nodes (Wh)` | - | Derived by the harness | harness |
| `Average energy consumption (Wh/Computing node)` | - | Derived by the harness | harness |
| `Cloud energy consumption (Wh)` | - | Derived by the harness | harness |
| `Average Cloud energy consumption (Wh/Data center)` | - | Derived by the harness | harness |
| `Edge energy consumption (Wh)` | - | Derived by the harness | harness |
| `Average Edge energy consumption (Wh/Data center)` | - | Derived by the harness | harness |
| `Mist energy consumption (Wh)` | - | Derived by the harness | harness |
| `Average Mist energy consumption (Wh/Device)` | - | Derived by the harness | harness |
| `WAN energy consumption (Wh)` | - | Derived by the harness | harness |
| `MAN energy consumption (Wh)` | - | Derived by the harness | harness |
| `LAN energy consumption (Wh)` | - | Derived by the harness | harness |
| `WiFi energy consumption (Wh)` | - | Derived by the harness | harness |
| `Cellular energy consumption (Wh)` | - | Derived by the harness | harness |
| `Ethernet energy consumption (Wh)` | - | Derived by the harness | harness |
| `Dead devices count` | - | Derived by the harness | harness |
| `Average remaining power (Wh)` | - | Derived by the harness | harness |
| `Average remaining power (%)` | - | Derived by the harness | harness |
| `First edge device death time (s)` | - | Derived by the harness | harness |
| `List of remaining power (%) (only battery powered devices / 0 = dead)` | - | Derived by the harness | harness |
| `List of the time when each device died (s)` | - | Derived by the harness | harness |
| `Tasks skipped because their optional input failed` | - | Derived by the harness | harness |
| `Requests generated` | - | Derived by the harness | harness |
| `Successful requests` | - | Derived by the harness | harness |
| `Average request makespan (s)` | - | Derived by the harness | harness |
| `task_success_fraction` | fraction | Per-task success fraction | task records |
| `task_delay_mean_s` | s | Mean task delivery delay | task records |
| `task_delay_p50_s` | - | Derived by the harness | harness |
| `task_delay_p95_s` | s | 95th percentile task delivery delay | task records |
| `task_delay_p99_s` | - | Derived by the harness | harness |
| `task_cpu_mean_s` | - | Derived by the harness | harness |
| `task_wait_mean_s` | - | Derived by the harness | harness |
| `task_wait_p95_s` | - | Derived by the harness | harness |
| `task_network_mean_s` | - | Derived by the harness | harness |
| `task_bytes_mean_kb` | - | Derived by the harness | harness |
| `task_retries_total` | - | Derived by the harness | harness |
| `task_recovered_total` | - | Derived by the harness | harness |
| `task_admitted_total` | - | Derived by the harness | harness |
| `task_admission_rate` | - | Derived by the harness | harness |
| `fail_delay` | - | Derived by the harness | harness |
| `fail_mobility` | - | Derived by the harness | harness |
| `fail_death` | - | Derived by the harness | harness |
| `fail_dependency` | - | Derived by the harness | harness |
| `fail_resources` | - | Derived by the harness | harness |
| `fail_no_destination` | - | Derived by the harness | harness |
| `task_overhead_mi_mean` | - | Derived by the harness | harness |
| `task_overhead_kb_mean` | - | Derived by the harness | harness |
| `task_poisoned_rate` | - | Derived by the harness | harness |
| `task_fidelity_mean` | - | Derived by the harness | harness |
| `task_compression_mean` | - | Derived by the harness | harness |
| `success_ratio` | fraction | Successfully executed tasks / generated tasks | aggregate row |
| `failure_ratio` | - | Derived by the harness | harness |
| `energy_per_success_wh` | Wh/task | Energy per successfully executed task | aggregate row |
| `success_per_wh` | tasks/Wh | Success throughput per watt-hour | aggregate row |
| `energy_per_task_wh` | Wh/task | Energy per generated task | aggregate row |
| `wan_usage_s` | s | Cumulative WAN usage time | aggregate row |
| `network_usage_s` | - | Derived by the harness | harness |
| `traffic_mb` | MB | Total network traffic | aggregate row |
| `request_count` | - | Derived by the harness | harness |
| `request_success_ratio` | fraction | Successful requests / generated requests | request records |
| `request_makespan_mean_s` | s | Mean successful end-to-end request makespan | request records |
| `request_makespan_p95_s` | s | 95th percentile request makespan | request records |
| `overhead_decisions` | - | Derived by the harness | harness |
| `overhead_cache_aware_picks` | - | Derived by the harness | harness |
| `overhead_overhead_mi` | - | Derived by the harness | harness |
| `overhead_overhead_kb` | - | Derived by the harness | harness |
| `overhead_mean_fidelity` | - | Derived by the harness | harness |
| `overhead_low_fidelity_rate` | - | Derived by the harness | harness |
| `overhead_estimated_deadline_rate` | - | Derived by the harness | harness |
| `overhead_generated` | - | Derived by the harness | harness |
| `overhead_success` | - | Derived by the harness | harness |
| `metadata` | - | Derived by the harness | harness |
| `grid` | - | Derived by the harness | harness |
| `cache_aware` | - | Derived by the harness | harness |
| `container_penalty_s` | - | Derived by the harness | harness |
| `overhead_flip_energy` | - | Derived by the harness | harness |
| `overhead_flip_container` | - | Derived by the harness | harness |
| `overhead_flip_locality` | - | Derived by the harness | harness |
| `overhead_flip_objective` | - | Derived by the harness | harness |
| `overhead_feasible` | - | Derived by the harness | harness |
| `overhead_fallback` | - | Derived by the harness | harness |
| `overhead_f1_feasible` | - | Derived by the harness | harness |
| `overhead_constrained_choice` | - | Derived by the harness | harness |
| `overhead_fid_hist_feasible` | - | Derived by the harness | harness |
| `overhead_fid_hist_fallback` | - | Derived by the harness | harness |
| `overhead_tier_picks` | - | Derived by the harness | harness |
| `overhead_locality_engaged` | - | Derived by the harness | harness |
| `overhead_warm_known` | - | Derived by the harness | harness |
| `overhead_no_destination` | - | Derived by the harness | harness |
| `overhead_term_delay_s` | - | Derived by the harness | harness |
| `overhead_term_energy_s` | - | Derived by the harness | harness |
| `overhead_term_container_s` | - | Derived by the harness | harness |
| `overhead_term_locality_s` | - | Derived by the harness | harness |
| `overhead_term_score_spread_s` | - | Derived by the harness | harness |
| `overhead_energy_share_of_score` | - | Derived by the harness | harness |
| `overhead_cache_hit_tasks` | - | Derived by the harness | harness |
| `overhead_cache_hit_rate` | - | Derived by the harness | harness |
| `cost_model` | - | Derived by the harness | harness |
| `fidelity_cost_gamma` | - | Derived by the harness | harness |
| `fidelity_cost_max` | - | Derived by the harness | harness |
| `fidelity_cost_min` | - | Derived by the harness | harness |
| `duration_min` | - | Derived by the harness | harness |
| `warmup_s` | - | Derived by the harness | harness |
| `fid_weighted` | fraction | Mean of success x fidelity over requests | task records |
| `mean_fidelity` | fraction | Mean fidelity level executed | task records |
