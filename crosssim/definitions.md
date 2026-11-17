# Timing definitions (fixed once, applied to every platform)

All quantities are in **simulation seconds**. One record is produced per task instance.
The five timestamps are the only primitives; every reported number is a derived difference.

| # | Name | Meaning | How it is obtained per platform |
|---|------|---------|---------------------------------|
| T0 | `release_s` | The instant the task is created at its generating device (job arrival). | **PureEdgeSim (reference):** `Task.getTime()` / `arrival_s` in the P1 trace. **YAFS:** `env.now` in the source process at the moment the message object is created (`core.py:416` sets `msg.timestamp` the same way). **EdgeCloudSim:** the `GET_NEW_TASK` / `generateTasks` event time in `BasicMobileDeviceManager` (`CloudSim.clock()`). |
| T1 | `decision_s` | The instant the orchestrator *commits* a destination for that task (the placement decision becomes irrevocable). | **YAFS:** time at which the periodic control loop admits the request out of its pending queue. **EdgeCloudSim:** time at which `EdgeOrchestrator` selects the VM and returns the assignment. |
| T2 | `dispatch_s` | The instant the request payload actually leaves the orchestrator/device towards the admitted destination (start of the uplink transfer). | **YAFS:** `env.now` when the message is injected into `sim.network_ctrl_pipe`. **EdgeCloudSim:** the send time of `TASK_SUBMISSION_TO_EDGE_SERVER`. |
| T3 | `exec_start_s` | The instant the destination begins executing the task (task leaves the destination's admission queue). | **YAFS:** `time_in` recorded by `SIM_CORE_CLASS.__update_node_metrics` (`core.py:502`). **EdgeCloudSim/CloudSim:** `Cloudlet.getExecStartTime()`. |
| T4 | `complete_s` | The instant the result is delivered back to the releasing device. | **YAFS:** `time_reception` of the returning message at the device-side sink module. **EdgeCloudSim:** `DOWNLINK_TRANSFER_END` / the returned-task event in the device manager. |

## Derived quantities

* `orchestration_latency_s` = **T3 − T0** (release-to-execution; the quantity P1 calls "Decision latency").
* `release_to_decision_s` = **T1 − T0** — this is the component the control interval buys.
* `decision_to_dispatch_s` = **T2 − T1** — the cost of the decision routine itself plus any orchestrator-side queueing.
* `dispatch_to_exec_s` = **T3 − T2** — uplink transmission/propagation plus destination admission queueing.
* `completion_delay_s` = **T4 − T0** — end-to-end service time used for the deadline test.
* `success` = 1 if `completion_delay_s ≤ deadline(class)`, else 0; `success_ratio` is the mean over the run.
* `architecture_effect(arch)` = `success_ratio(ALL)` − `success_ratio(EDGE_ONLY)` at fixed temporal setting.

## Platform caveats (which primitives a platform cannot expose, and why)

* **YAFS 3.1** has *no deadline concept anywhere in its source* (`grep -ri deadline src/yafs` → no match), so
  compliance is computed offline from `complete_s` versus the class deadline; YAFS itself reports only service
  and link latencies. YAFS also has **no orchestrator entity**: placement is either a one-shot
  `Placement.initial_allocation` or a periodic `Placement.run` (`core.py:379-381`) that is *not* on the task's
  message path, and per-message routing is `Selection.get_path`, called synchronously at send time
  (`core.py:205`). Therefore, in stock YAFS **T1 = T0 and T2 = T1 exactly**: `release_to_decision` and
  `decision_to_dispatch` are identically 0 by construction, not by measurement. Any non-zero decision latency
  reported for YAFS comes from the periodic control loop added by this replication (see `PORTING_NOTES.md`).
* **EdgeCloudSim 5.1**'s `BasicEdgeOrchestrator` makes its decision synchronously when the request arrives
  (`processEvent` is an empty method for scheduling purposes; the decision path is the `GET_TASK_RESULT_FROM_ORCH`
  exchange), so in the stock orchestrator T1 ≈ T0 and T2 ≈ T1. A periodic variant had to be added for the sweep.
* **CloudSim-family platforms report execution start but not "decision committed"**: `decision_s` is instrumented
  inside the custom orchestrator code, not read from the engine.
* T4 (result delivery) is measured, not assumed, on both platforms that ran.
