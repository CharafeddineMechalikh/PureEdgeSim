# P5 cross-layer ISCC-MEC data dictionary

| Column | Unit | Meaning | Source |
|---|---|---|---|
| `job_id` | - | Derived by the harness | harness |
| `algorithm` | - | Derived by the harness | harness |
| `antenna_gain` | x | Reconfigurable / movable antenna array gain | job |
| `architecture` | - | Derived by the harness | harness |
| `bandwidth_factor` | x | PHY link bandwidth multiplier | job |
| `compression_ratio` | ratio | Semantic feature payload scaling ratio rho | job |
| `experiment` | - | Derived by the harness | harness |
| `fidelity` | ratio | Inference compute fidelity phi | job |
| `pipeline_depth` | stages | Number of sequential stages in the ISCC DAG (2, 4, or 6) | job |
| `policy` | - | agnostic or iscc_aware | job |
| `rate_factor` | - | Derived by the harness | harness |
| `seed` | - | Derived by the harness | harness |
| `run_id` | - | Derived by the harness | harness |
| `devices` | - | Derived by the harness | harness |
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
| `stage_sense_delay_s` | - | Derived by the harness | harness |
| `stage_sense_cpu_s` | - | Derived by the harness | harness |
| `stage_sense_wait_s` | - | Derived by the harness | harness |
| `stage_sense_network_s` | - | Derived by the harness | harness |
| `stage_sense_success_rate` | - | Derived by the harness | harness |
| `stage_sense_bytes_kb` | - | Derived by the harness | harness |
| `stage_encode_comm_delay_s` | - | Derived by the harness | harness |
| `stage_encode_comm_cpu_s` | - | Derived by the harness | harness |
| `stage_encode_comm_wait_s` | - | Derived by the harness | harness |
| `stage_encode_comm_network_s` | - | Derived by the harness | harness |
| `stage_encode_comm_success_rate` | - | Derived by the harness | harness |
| `stage_encode_comm_bytes_kb` | - | Derived by the harness | harness |
| `stage_edge_infer_delay_s` | - | Derived by the harness | harness |
| `stage_edge_infer_cpu_s` | - | Derived by the harness | harness |
| `stage_edge_infer_wait_s` | - | Derived by the harness | harness |
| `stage_edge_infer_network_s` | - | Derived by the harness | harness |
| `stage_edge_infer_success_rate` | - | Derived by the harness | harness |
| `stage_edge_infer_bytes_kb` | - | Derived by the harness | harness |
| `stage_actuate_delay_s` | - | Derived by the harness | harness |
| `stage_actuate_cpu_s` | - | Derived by the harness | harness |
| `stage_actuate_wait_s` | - | Derived by the harness | harness |
| `stage_actuate_network_s` | - | Derived by the harness | harness |
| `stage_actuate_success_rate` | - | Derived by the harness | harness |
| `stage_actuate_bytes_kb` | - | Derived by the harness | harness |
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
| `iscc_pipeline_request_count` | - | Derived by the harness | harness |
| `iscc_pipeline_request_success_ratio` | fraction | Requests completing all stages within deadline | observer |
| `iscc_pipeline_makespan_mean_s` | s | Mean end-to-end ISCC pipeline makespan | observer |
| `iscc_pipeline_makespan_p95_s` | - | Derived by the harness | harness |
| `iscc_dependency_failure_rate` | fraction | Tasks failed due to upstream stage failure | observer |
| `iscc_bytes_per_request_kb` | - | Derived by the harness | harness |
| `iscc_energy_per_successful_request_wh` | - | Derived by the harness | harness |
| `iscc_semantic_quality_mean` | - | Derived by the harness | harness |
| `iscc_effective_semantic_utility` | - | Derived by the harness | harness |
| `iscc_mean_compression_ratio` | - | Derived by the harness | harness |
| `iscc_mean_fidelity` | - | Derived by the harness | harness |
| `iscc_colocation_ratio` | fraction | Dependent stages co-located on same node/tier | observer |
| `iscc_edge_share` | - | Derived by the harness | harness |
| `iscc_mist_share` | - | Derived by the harness | harness |
| `iscc_cloud_share` | - | Derived by the harness | harness |
| `metadata` | - | Derived by the harness | harness |
| `grid` | - | Derived by the harness | harness |
| `stage_preproc_filter_delay_s` | - | Derived by the harness | harness |
| `stage_preproc_filter_cpu_s` | - | Derived by the harness | harness |
| `stage_preproc_filter_wait_s` | - | Derived by the harness | harness |
| `stage_preproc_filter_network_s` | - | Derived by the harness | harness |
| `stage_preproc_filter_success_rate` | - | Derived by the harness | harness |
| `stage_preproc_filter_bytes_kb` | - | Derived by the harness | harness |
| `stage_feature_align_delay_s` | - | Derived by the harness | harness |
| `stage_feature_align_cpu_s` | - | Derived by the harness | harness |
| `stage_feature_align_wait_s` | - | Derived by the harness | harness |
| `stage_feature_align_network_s` | - | Derived by the harness | harness |
| `stage_feature_align_success_rate` | - | Derived by the harness | harness |
| `stage_feature_align_bytes_kb` | - | Derived by the harness | harness |
