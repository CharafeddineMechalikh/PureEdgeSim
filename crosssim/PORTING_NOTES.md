# How faithfully could each platform express the scenario?

## Single source of truth

`scenario.json` defines the three workload classes (2000 MI / 0.2 s,
12000 MI / 0.8 s, 20000 MI / 60 s at rates 20, 15, 5 per minute and device
shares 52.8/35.2/12.1 %), 15 devices, two device types, one edge datacenter and
one cloud, and the three architectures. Both the YAFS driver and the
PureEdgeSim derivation read the same numbers by construction; the YAFS driver
reads the file directly.

## YAFS: what had to be added

| PureEdgeSim feature | YAFS equivalent | Fidelity |
|---|---|---|
| Periodic global orchestrator | A SimPy process registered by the driver, offloading at the next tick | **Exact in semantics**; the tick is the driver's parameter, not YAFS's |
| Mist (device-to-device) placement | A DES module per device core; local requests skip the tick | Equivalent |
| Edge/cloud replica per core | One DES server per core (`deploy_module(..., [node]*n)`) | Equivalent; YAFS has no core count, so the driver multiplies replicas |
| Node capacity in MIPS | `IPT` attribute per node; service time = instructions/IPT | Equivalent for a single task |
| WAN / MAN / WiFi links | Topology links with bandwidth and propagation | Equivalent |
| Deadline-aware selection | Driver chooses the least expected completion time among candidates | **Simplified**: PureEdgeSim's `TRADE_OFF` weighs more terms |
| Energy | Not modelled | **Missing**: no energy comparison is made in this replication |
| Mobility | Not modelled | **Missing**: all devices are static |
| Poisson arrivals | Deterministic period per device | **Different**: differences in burstiness are not captured |

## Where the port could bias the result

1. **Deterministic arrivals** make the queueing milder than in PureEdgeSim,
   which biases *towards* success at every clock setting. The replication still
   shows the collapse at a 1 s tick, so the bias is conservative with respect to
   the claim.
2. **Least-expected-completion selection** is not `TRADE_OFF`. It is closer to
   `MCT` in P1, an algorithm that P1 showed to be the best at 50 ms and among
   the worst at 1 s. This is why the replication is read as a test of the
   *mechanism* (wait one tick, then miss) and not of the policy ranking.
3. **One deployment, no containers.** PureEdgeSim charges container transfer
   and deployment time; YAFS here pre-installs modules. This removes one source
   of delay, again biasing towards success.
4. **No mobility** removes handover failures, which PureEdgeSim reports as a
   small but non-zero cause. This also biases towards success.

All four biases operate in the same direction, against the observed effect: the
replication reports the artefact *despite* modelling choices that should
suppress it. That is the strongest form the evidence could take with a port of
this size.

## EdgeCloudSim

Not ported. Placement is a synchronous per-task function
(`BasicEdgeOrchestrator.java:89,115`) with no global period, so the parameter
under test does not exist. Its shipped tutorial was run to confirm the platform
works in this environment; no success ratios from it are used, because they
belong to a different scenario.

## What would strengthen this further

* Port the scenario to iFogSim2 and record that it, too, has no global period —
  turning "we could not test it" into "two platforms lack the parameter".
* Add Poisson arrivals and container transfer to the YAFS driver so that the
  absolute success ratios become comparable.
* Run YAFS with mobility so that the mist tier can fail for the reason it does
  in PureEdgeSim.
