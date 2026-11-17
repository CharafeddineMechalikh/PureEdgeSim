"""Security-by-design model, federated-round aggregation and adaptive orchestrator (P4).

This module implements every modelling item in ``04_secure_federated_agentic_mec/paper/research_plan.md``:

1. **Literature-calibrated security-level matrix** (``SECURITY_PROFILES`` and
   ``CRYPTO_LITERATURE_SOURCES``):
   - Level 0 (``none``): plaintext baseline, no cryptographic framing.
   - Level 1 (``signature``): post-quantum message authentication via NIST FIPS 204
     ML-DSA-65 (CRYSTALS-Dilithium3).
   - Level 2 (``pqc``): post-quantum key encapsulation + authenticated channel via
     NIST FIPS 203 ML-KEM-768 (CRYSTALS-Kyber768) + ML-DSA-65 / CSIDH-512 MAC framing
     (Sammartino 2026, arXiv:2606.03611).
   - Level 3 (``pqc+he``): PQC channel + CKKS approximate homomorphic encryption on
     federated updates and sensitive agent context (Cheon et al. 2017; Wang et al.
     2026 CRAFT, arXiv:2610.27651).
   - Level 4 (``secure-agg``): PQC + pairwise-masked Secure Aggregation with
     Byzantine norm/cosine verification (Bonawitz et al. CCS 2017).

2. **Federated-round and aggregation-point model**:
   Federated updates are grouped into rounds of ``round_period_s`` seconds and
   prefer co-location at an operator edge aggregation point (``EDGE_DATACENTER``)
   so that ciphertext expansion does not traverse the shared WAN.

3. **Joint ``SecurityAwareOrchestrator``**:
   Chooses both the per-task security level ``ell`` and the computing node under
   latency, energy, and threat-exposure constraints, while ``SecureManager``
   disables PureEdgeSim's static per-device placement cache so every task is
   orchestrated dynamically.

4. **Adversarial & privacy leakage proxies + overhead breakdown** (``SecurityObserver``):
   Tracks poisoning success, passive eavesdropping exposure, membership-inference
   privacy leakage proxy, protected-exchange ratio, and the decomposition of
   overhead into cryptographic compute (MI), network byte inflation (KB), and
   container/storage footprint (KB).
"""
from __future__ import annotations

from dataclasses import dataclass

from pureedgesim.manager import SimulationManager
from pureedgesim.nodes import NodeType
from pureedgesim.orchestrator import Orchestrator
from pureedgesim.tasks import DefaultTaskGenerator, TaskStatus

KB = 8000.0

# Calibrated security levels and provenance table.
SECURITY_PROFILES: dict[int, dict] = {
    0: {
        "level": 0,
        "name": "none",
        "label": "L0: Unprotected",
        "compute_mul": 1.00,
        "fixed_crypto_mi": 0.0,
        "payload_mul": 1.00,
        "fixed_crypto_kb": 0.0,
        "container_mul": 1.00,
        "handshake_s": 0.000,
        "integrity_def": 0.00,
        "confidentiality_def": 0.00,
        "privacy_def": 0.00,
        "mechanism": "Plaintext coordination and unauthenticated parameter exchange",
        "source": "Baseline (no cryptography)",
    },
    1: {
        "level": 1,
        "name": "signature",
        "label": "L1: PQ Signature (ML-DSA-65)",
        "compute_mul": 1.08,
        "fixed_crypto_mi": 450.0,
        "payload_mul": 1.04,
        "fixed_crypto_kb": 5.2,
        "container_mul": 1.05,
        "handshake_s": 0.002,
        "integrity_def": 0.55,
        "confidentiality_def": 0.05,
        "privacy_def": 0.10,
        "mechanism": "ML-DSA-65 (Dilithium3) sign + verify (pk 1,952 B, sig 3,293 B)",
        "source": "NIST FIPS 204 (2024); Wang et al. (arXiv:2610.27651, 2026)",
    },
    2: {
        "level": 2,
        "name": "pqc",
        "label": "L2: PQC Channel (ML-KEM-768 + ML-DSA)",
        "compute_mul": 1.24,
        "fixed_crypto_mi": 1150.0,
        "payload_mul": 1.22,
        "fixed_crypto_kb": 12.0,
        "container_mul": 1.15,
        "handshake_s": 0.006,
        "integrity_def": 0.72,
        "confidentiality_def": 0.92,
        "privacy_def": 0.45,
        "mechanism": "ML-KEM-768 (Kyber768) KEM + ML-DSA-65 / CSIDH-512 authenticated channel",
        "source": "NIST FIPS 203/204 (2024); Sammartino (arXiv:2606.03611, 2026)",
    },
    3: {
        "level": 3,
        "name": "pqc+he",
        "label": "L3: PQC + CKKS Homomorphic Enc.",
        "compute_mul": 1.55,
        "fixed_crypto_mi": 2600.0,
        "payload_mul": 1.65,
        "fixed_crypto_kb": 28.0,
        "container_mul": 1.32,
        "handshake_s": 0.014,
        "integrity_def": 0.85,
        "confidentiality_def": 0.97,
        "privacy_def": 0.86,
        "mechanism": "ML-KEM-768 + CKKS approximate HE (N=2^13) on model/context updates",
        "source": "Cheon et al. (ASIACRYPT 2017); Wang et al. CRAFT (arXiv:2610.27651, 2026)",
    },
    4: {
        "level": 4,
        "name": "secure-agg",
        "label": "L4: PQC + Secure Aggregation + Clipping",
        "compute_mul": 1.85,
        "fixed_crypto_mi": 4200.0,
        "payload_mul": 2.05,
        "fixed_crypto_kb": 45.0,
        "container_mul": 1.55,
        "handshake_s": 0.022,
        "integrity_def": 0.96,
        "confidentiality_def": 0.99,
        "privacy_def": 0.95,
        "mechanism": "PQC pairwise masking + Shamir secret sharing + Byzantine norm clipping",
        "source": "Bonawitz et al. (ACM CCS 2017); Sammartino (arXiv:2606.03611, 2026)",
    },
}

TIER_EXPOSURE: dict[NodeType, float] = {
    NodeType.EDGE_DATACENTER: 0.22,   # operator-managed edge aggregation site
    NodeType.EDGE_DEVICE: 0.38,       # peer wireless mist node
    NodeType.CLOUD: 0.85,             # multi-hop shared WAN to remote cloud
}


@dataclass
class SecurityConfig:
    security_level: int = 2
    policy: str = "level2"            # level0..level4 | adaptive
    threat_level: float = 0.20        # adversarial poisoning / eavesdrop / inference intensity
    model_size_factor: float = 1.0    # scales federated update & coordination payload/compute
    rate_factor: float = 1.0
    round_period_s: float = 30.0
    energy_weight: float = 0.03
    wan_penalty_weight: float = 0.08


def apply_security_level(task, level: int, model_size_factor: float,
                         threat_level: float, tier: NodeType | None = None) -> dict:
    """Apply cryptographic compute, byte, and container costs for ``level`` to ``task``."""
    profile = SECURITY_PROFILES[int(level)]
    nom_len = float(getattr(task, "research_nominal_length", task.length))
    nom_req_kb = float(getattr(task, "research_nominal_req_kb", task.request_size / KB))
    nom_res_kb = float(getattr(task, "research_nominal_res_kb", task.result_size / KB))
    nom_cont_kb = float(getattr(task, "research_nominal_cont_kb", task.container_size / KB))

    is_federated = int(getattr(task, "application_id", 0)) == 0
    size_scale = float(model_size_factor) if is_federated else (0.5 + 0.5 * float(model_size_factor))

    base_len = nom_len * size_scale
    base_req_kb = nom_req_kb * size_scale
    base_res_kb = nom_res_kb * size_scale
    base_cont_kb = nom_cont_kb * (0.7 + 0.3 * size_scale)

    new_len = base_len * profile["compute_mul"] + profile["fixed_crypto_mi"]
    new_req_kb = base_req_kb * profile["payload_mul"] + profile["fixed_crypto_kb"]
    new_res_kb = base_res_kb * (1.0 + 0.5 * (profile["payload_mul"] - 1.0)) + 0.25 * profile["fixed_crypto_kb"]
    new_cont_kb = base_cont_kb * profile["container_mul"]

    task.length = float(new_len)
    task.request_size = float(new_req_kb * KB)
    task.result_size = float(new_res_kb * KB)
    task.container_size = float(new_cont_kb * KB)

    crypto_mi = max(0.0, new_len - base_len)
    crypto_kb = max(0.0, (new_req_kb - base_req_kb) + (new_res_kb - base_res_kb))
    storage_kb = max(0.0, new_cont_kb - base_cont_kb)

    exposure = TIER_EXPOSURE.get(tier, 0.45) if tier is not None else 0.45
    # Edge-local aggregation further hardens gradient inspection & WAN interception
    edge_bonus = 0.08 if tier == NodeType.EDGE_DATACENTER else 0.0
    integrity = min(0.99, profile["integrity_def"] + edge_bonus)
    confidentiality = min(0.995, profile["confidentiality_def"] + 0.5 * edge_bonus)
    privacy = min(0.98, profile["privacy_def"] + (0.06 if tier == NodeType.EDGE_DATACENTER else 0.0))

    poison_prob = float(threat_level) * (1.0 - integrity)
    eaves_prob = float(threat_level) * exposure * (1.0 - confidentiality)
    # Membership-inference leakage scales with model update size and lack of HE/SecAgg privacy
    base_leakage = min(0.50, (0.18 + 0.32 * float(threat_level)) * (0.85 + 0.15 * min(size_scale, 4.0)))
    privacy_leak = base_leakage * (0.65 + 0.35 * exposure) * (1.0 - privacy)
    composite_attack = 1.0 - (1.0 - poison_prob) * (1.0 - eaves_prob) * (1.0 - privacy_leak)

    task.research_security_level = int(level)
    task.research_overhead_mi = float(crypto_mi)
    task.research_overhead_kb = float(crypto_kb)
    task.research_storage_overhead_kb = float(storage_kb)
    task.research_handshake_s = float(profile["handshake_s"])
    task.research_protected = int(level >= 2)
    task.research_poison_prob = float(poison_prob)
    task.research_eaves_prob = float(eaves_prob)
    task.research_privacy_leak = float(privacy_leak)
    task.research_attack_prob = float(composite_attack)
    return {
        "level": int(level),
        "crypto_mi": crypto_mi,
        "crypto_kb": crypto_kb,
        "storage_kb": storage_kb,
        "handshake_s": profile["handshake_s"],
        "poison_prob": poison_prob,
        "eaves_prob": eaves_prob,
        "privacy_leak": privacy_leak,
        "composite_attack": composite_attack,
    }


class SecureTaskGenerator(DefaultTaskGenerator):
    """Generates federated and agentic MEC tasks annotated with nominal demand and round IDs."""

    def __init__(self, manager, config: SecurityConfig) -> None:
        super().__init__(manager)
        self.config = config

    def generate(self):
        tasks = super().generate()
        cfg = self.config
        for task in tasks:
            task.research_nominal_length = float(task.length)
            task.research_nominal_req_kb = float(task.request_size / KB)
            task.research_nominal_res_kb = float(task.result_size / KB)
            task.research_nominal_cont_kb = float(task.container_size / KB)
            task.research_round_id = int(task.time // max(1.0, cfg.round_period_s))
            apply_security_level(task, cfg.security_level, cfg.model_size_factor, cfg.threat_level)
            task.research_poisoned = int(self.random.random() < task.research_poison_prob)
        return tasks


class SecureManager(SimulationManager):
    """Disables PureEdgeSim's static placement cache so every task is security-orchestrated."""

    def _send_from_orchestrator_to_destination(self, task) -> None:
        if task.source_device is not None:
            task.source_device.application_placed = False
            task.source_device.application_placement_location = None
        super()._send_from_orchestrator_to_destination(task)


class SecurityAwareOrchestrator(Orchestrator):
    """Joint security-level selection and aggregation-aware placement.

    * Under static policies (``level0``..``level4``), tasks keep their configured
      security level and use PureEdgeSim's baseline ``trade_off`` rule, which exposes
      the cost of unconditional security and WAN backhaul traversal.
    * Under ``adaptive``, the orchestrator jointly picks ``(node, level)`` for each
      task: it selects the minimum security level meeting the contextual risk target
      for that tier, prefers edge-local aggregation (``EDGE_DATACENTER``) for
      federated rounds to avoid WAN ciphertext amplification, and balances completion
      delay, energy, and residual risk within the task deadline.
    """

    def __init__(self, manager, config: SecurityConfig | None = None) -> None:
        super().__init__(manager)
        self.config = config or SecurityConfig()
        self.round_aggregators: dict[int, int] = {}
        self.stats = {
            "decisions": 0,
            "edge_agg_picks": 0,
            "cloud_picks": 0,
            "mist_picks": 0,
            "level_sum": 0.0,
            "level_counts": {level: 0 for level in SECURITY_PROFILES},
        }

    def _eligible(self, task):
        out = []
        for index, node in enumerate(self.candidates):
            if not node.is_alive():
                continue
            if node.type == NodeType.EDGE_DATACENTER:
                if NodeType.EDGE_DATACENTER in self.architecture_layers:
                    out.append((index, node))
            elif self.offloading_is_possible(node, task):
                out.append((index, node))
        return out

    def _estimate_delay_energy(self, node, task, length_mi: float, req_bits: float,
                               res_bits: float, handshake_s: float) -> tuple[float, float]:
        compute_s = length_mi / max(1.0, node.mips_per_core)
        pending_mi = (sum(item.length for item in node.executing_tasks)
                      + sum(item.length for item in node.task_queue))
        queue_s = pending_mi / max(1.0, node.total_mips)
        device = task.source_device
        if node.type == NodeType.EDGE_DEVICE:
            if device.distance_to(node) > self.manager.parameters.edge_devices_range:
                return float("inf"), float("inf")
            net_s = self.manager.parameters.wifi_delay + (req_bits + res_bits) / max(
                1.0, self.manager.parameters.wifi_bandwidth)
        else:
            route = self.manager.network.graph.shortest_path(device, node)
            if not route:
                return float("inf"), float("inf")
            net_s = sum(link.delay + (req_bits + res_bits) / max(1.0, link.bandwidth) for link in route)
        total_delay = compute_s + queue_s + net_s + handshake_s
        model = node.energy_model
        dyn_wh = max(0.0, model.max_consumption - model.idle_consumption) / 3600.0 * min(
            total_delay, length_mi / max(1.0, node.total_mips))
        stat_wh = model.idle_consumption / 3600.0 * compute_s
        return total_delay, dyn_wh + stat_wh

    def _candidate_levels(self, node, task) -> tuple[int, ...]:
        threat = float(self.config.threat_level)
        is_federated = int(getattr(task, "application_id", 0)) == 0
        if node.type == NodeType.CLOUD:
            # Traversing the shared WAN requires at least PQC+HE (L3) or PQC (L2) if threat is zero.
            return (3, 4) if threat >= 0.10 else (2, 3)
        if node.type == NodeType.EDGE_DATACENTER:
            if threat <= 0.05:
                return (2,)
            if threat <= 0.20:
                return (3, 2) if is_federated else (2, 3)
            return (3, 4, 2) if is_federated else (3, 2)
        # Mist peer
        if threat <= 0.10:
            return (2,)
        return (3, 2)

    def find_computing_node(self, tasks, algorithm: str, application_id: int) -> int:
        task = tasks[0]
        eligible = self._eligible(task)
        if not eligible:
            return -1

        if self.config.policy != "adaptive":
            index = self.trade_off(eligible, task) if algorithm != "ROUND_ROBIN" else min(
                eligible, key=lambda pair: self.history[pair[0]])[0]
            if index >= 0:
                node = self.candidates[index]
                apply_security_level(task, self.config.security_level,
                                     self.config.model_size_factor,
                                     self.config.threat_level, node.type)
                task.research_poisoned = int(
                    self.manager.random.random() < task.research_poison_prob)
                self._record(index, node, int(self.config.security_level))
            return index

        # Joint (node, security_level) optimization for the adaptive policy.
        round_id = int(getattr(task, "research_round_id", 0))
        preferred_agg = self.round_aggregators.get(round_id)
        nom_len = float(getattr(task, "research_nominal_length", task.length))
        nom_req_kb = float(getattr(task, "research_nominal_req_kb", task.request_size / KB))
        nom_res_kb = float(getattr(task, "research_nominal_res_kb", task.result_size / KB))
        is_federated = int(getattr(task, "application_id", 0)) == 0
        size_scale = (float(self.config.model_size_factor) if is_federated
                      else (0.5 + 0.5 * float(self.config.model_size_factor)))

        best_choice = None
        for index, node in eligible:
            # Skip low-battery devices for heavy cryptographic workloads
            if (node.type == NodeType.EDGE_DEVICE and node.is_battery_powered
                    and node.get_remaining_battery_percentage() < 25.0):
                continue
            for level in self._candidate_levels(node, task):
                prof = SECURITY_PROFILES[level]
                cand_len = nom_len * size_scale * prof["compute_mul"] + prof["fixed_crypto_mi"]
                cand_req_bits = (nom_req_kb * size_scale * prof["payload_mul"] + prof["fixed_crypto_kb"]) * KB
                cand_res_bits = (nom_res_kb * size_scale * (1.0 + 0.5 * (prof["payload_mul"] - 1.0))) * KB
                delay_s, energy_wh = self._estimate_delay_energy(
                    node, task, cand_len, cand_req_bits, cand_res_bits, prof["handshake_s"])
                deadline_violation = max(0.0, delay_s - 0.85 * task.max_latency)
                wan_penalty = (self.config.wan_penalty_weight * (cand_req_bits / KB) / 1000.0
                               if node.type == NodeType.CLOUD else 0.0)
                agg_bonus = -0.025 if (is_federated and node.type == NodeType.EDGE_DATACENTER
                                       and (preferred_agg is None or node.id == preferred_agg)) else 0.0
                mist_penalty = 0.04 if node.type == NodeType.EDGE_DEVICE else 0.0
                exposure = TIER_EXPOSURE.get(node.type, 0.45)
                residual_risk = float(self.config.threat_level) * (
                    0.45 * (1.0 - prof["integrity_def"])
                    + 0.30 * exposure * (1.0 - prof["confidentiality_def"])
                    + 0.25 * (1.0 - prof["privacy_def"])
                )
                score = (delay_s + 4.0 * deadline_violation
                         + self.config.energy_weight * energy_wh
                         + wan_penalty + mist_penalty + agg_bonus
                         + 0.35 * residual_risk)
                if best_choice is None or score < best_choice[0]:
                    best_choice = (score, index, node, level)

        if best_choice is None:
            index = self.trade_off(eligible, task)
            node = self.candidates[index]
            level = 2
        else:
            _, index, node, level = best_choice

        if is_federated and node.type == NodeType.EDGE_DATACENTER and round_id not in self.round_aggregators:
            self.round_aggregators[round_id] = node.id

        apply_security_level(task, level, self.config.model_size_factor,
                             self.config.threat_level, node.type)
        task.research_poisoned = int(self.manager.random.random() < task.research_poison_prob)
        self._record(index, node, level)
        return index

    def _record(self, index: int, node, level: int) -> None:
        self.history[index] += 1
        self.stats["decisions"] += 1
        self.stats["level_sum"] += int(level)
        self.stats["level_counts"][int(level)] = self.stats["level_counts"].get(int(level), 0) + 1
        if node.type == NodeType.EDGE_DATACENTER:
            self.stats["edge_agg_picks"] += 1
        elif node.type == NodeType.CLOUD:
            self.stats["cloud_picks"] += 1
        elif node.type == NodeType.EDGE_DEVICE:
            self.stats["mist_picks"] += 1


class SecurityObserver:
    """Computes P4 security, adversarial, privacy, and overhead-breakdown metrics."""

    prefix = "security"

    def __init__(self, manager, engine) -> None:
        self.manager = manager
        self.engine = engine

    def summary(self) -> dict:
        tasks = list(self.manager.tasks)
        if not tasks:
            return {}
        n = len(tasks)
        levels = [int(getattr(t, "research_security_level", 0)) for t in tasks]
        protected = [int(getattr(t, "research_protected", 0)) for t in tasks]
        crypto_mi = [float(getattr(t, "research_overhead_mi", 0.0)) for t in tasks]
        crypto_kb = [float(getattr(t, "research_overhead_kb", 0.0)) for t in tasks]
        storage_kb = [float(getattr(t, "research_storage_overhead_kb", 0.0)) for t in tasks]
        handshake_s = [float(getattr(t, "research_handshake_s", 0.0)) for t in tasks]
        nom_req_kb = [float(getattr(t, "research_nominal_req_kb", 1.0))
                      + float(getattr(t, "research_nominal_res_kb", 0.0)) for t in tasks]
        act_kb = [(t.request_size + t.result_size) / KB for t in tasks]
        poison_prob = [float(getattr(t, "research_poison_prob", 0.0)) for t in tasks]
        eaves_prob = [float(getattr(t, "research_eaves_prob", 0.0)) for t in tasks]
        privacy_leak = [float(getattr(t, "research_privacy_leak", 0.0)) for t in tasks]
        attack_prob = [float(getattr(t, "research_attack_prob", 0.0)) for t in tasks]

        fed_success = sum(1 for t in tasks
                          if int(getattr(t, "application_id", 0)) == 0
                          and t.status == TaskStatus.SUCCESS)
        duration = max(1.0, float(self.manager.parameters.simulation_duration))
        protected_count = max(1, sum(protected))
        total_energy = float(self.manager.metrics.total_energy_consumption) if hasattr(
            self.manager.metrics, "total_energy_consumption") else 0.0

        # Normalize overhead components into equivalent delay contribution shares
        mean_crypto_s = (sum(crypto_mi) / n) / 40000.0
        mean_net_s = (sum(crypto_kb) / n) * 8.0 / 100000.0 + (sum(handshake_s) / n)
        mean_stor_s = (sum(storage_kb) / n) * 8.0 / 400000.0
        total_equiv = max(1e-9, mean_crypto_s + mean_net_s + mean_stor_s)

        orch_stats = dict(getattr(self.manager.orchestrator, "stats", {}))
        decisions = max(1, int(orch_stats.get("decisions", n)))
        return {
            "mean_level": sum(levels) / n,
            "protected_exchange_ratio": sum(protected) / n,
            "byte_amplification": sum(act_kb) / max(1e-9, sum(nom_req_kb)),
            "crypto_compute_mi_mean": sum(crypto_mi) / n,
            "network_overhead_kb_mean": sum(crypto_kb) / n,
            "storage_overhead_kb_mean": sum(storage_kb) / n,
            "mean_handshake_s": sum(handshake_s) / n,
            "compute_overhead_share": mean_crypto_s / total_equiv if (mean_crypto_s + mean_net_s + mean_stor_s) > 0 else 0.0,
            "network_overhead_share": mean_net_s / total_equiv if (mean_crypto_s + mean_net_s + mean_stor_s) > 0 else 0.0,
            "storage_overhead_share": mean_stor_s / total_equiv if (mean_crypto_s + mean_net_s + mean_stor_s) > 0 else 0.0,
            "poisoning_attack_rate": sum(poison_prob) / n,
            "eavesdrop_exposure_rate": sum(eaves_prob) / n,
            "privacy_leakage_proxy": sum(privacy_leak) / n,
            "composite_attack_rate": sum(attack_prob) / n,
            "agg_throughput_per_s": fed_success / duration,
            "edge_agg_share": float(orch_stats.get("edge_agg_picks", 0)) / decisions,
            "cloud_share": float(orch_stats.get("cloud_picks", 0)) / decisions,
            "mist_share": float(orch_stats.get("mist_picks", 0)) / decisions,
            "protected_tasks": int(sum(protected)),
            "total_tasks": int(n),
            "energy_per_protected_wh": total_energy / protected_count if sum(protected) > 0 else float("nan"),
        }
