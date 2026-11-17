# P3 feasibility-certificate data dictionary

| Column | Unit | Meaning | Source |
|---|---|---|---|
| `job_id` | - | Derived by the harness | harness |
| `algorithm` | - | Derived by the harness | harness |
| `architecture` | - | Derived by the harness | harness |
| `base_sigma` | - | Derived by the harness | harness |
| `burst` | - | Derived by the harness | harness |
| `devices` | - | Derived by the harness | harness |
| `experiment` | - | Derived by the harness | harness |
| `failure_intensity` | % | Share of devices affected by kill/link/battery injections | job |
| `max_retries` | - | Derived by the harness | harness |
| `mobility_sigma` | - | Derived by the harness | harness |
| `outage` | - | Derived by the harness | harness |
| `policy` | - | naive, ft, certified or certified_ft | job |
| `queue_sigma` | - | Derived by the harness | harness |
| `rate_factor` | - | Derived by the harness | harness |
| `safety_margin` | fraction | Relative slack subtracted from the deadline | job |
| `seed` | - | Derived by the harness | harness |
| `z` | - | One-sided uncertainty multiplier of the certificate | job |
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
| `certificate_true_positive` | - | Derived by the harness | harness |
| `certificate_false_positive` | - | Derived by the harness | harness |
| `certificate_true_negative` | - | Derived by the harness | harness |
| `certificate_false_negative` | - | Derived by the harness | harness |
| `certificate_precision` | - | Derived by the harness | harness |
| `certificate_recall` | - | Derived by the harness | harness |
| `certificate_false_admission_rate` | - | Derived by the harness | harness |
| `certificate_false_rejection_rate` | - | Derived by the harness | harness |
| `certificate_admission_rate` | - | Derived by the harness | harness |
| `certificate_rejection_rate` | - | Derived by the harness | harness |
| `certificate_mean_predicted_s` | - | Derived by the harness | harness |
| `certificate_mean_margin_s` | - | Derived by the harness | harness |
| `faults_retries` | - | Derived by the harness | harness |
| `faults_recovered` | - | Derived by the harness | harness |
| `faults_exhausted` | - | Derived by the harness | harness |
| `faults_retry_event_count` | - | Derived by the harness | harness |
| `faults_retry_event_sample` | - | Derived by the harness | harness |
| `faults_generated` | - | Derived by the harness | harness |
| `faults_dead_devices` | - | Derived by the harness | harness |
| `metadata` | - | Derived by the harness | harness |
| `grid` | - | Derived by the harness | harness |
| `task_poisoned_rate` | - | Derived by the harness | harness |
| `task_fidelity_mean` | - | Derived by the harness | harness |
| `task_compression_mean` | - | Derived by the harness | harness |
| `injections` | - | Derived by the harness | harness |
| `success_pct` | - | Derived by the harness | harness |
| `delay_failure_rate` | - | Derived by the harness | harness |
| `death_failure_rate` | - | Derived by the harness | harness |
| `rejection_rate` | fraction | Certificate refusals / generated tasks | aggregate row |
| `affected_tasks` | - | Derived by the harness | harness |
| `recovery_ratio` | fraction | Recovered / retried tasks | task records |
| `attempted_tasks` | - | Derived by the harness | harness |
| `violation_rate_attempted` | fraction | Deadline failures / attempted tasks | aggregate row |
