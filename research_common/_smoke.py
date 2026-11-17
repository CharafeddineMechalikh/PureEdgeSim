from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from research_common.runs import run_once

BASE = Path("01_sim2deploy_sagin_uav_benchmark/scenario/settings")
for tag, props, inj in [
    ("a", {"realistic_network_model": "true"}, None),
    ("b", {"realistic_network_model": "true"}, None),
]:
    row = run_once(base_settings=BASE, out_dir=Path("research_common/_smoke") / tag,
                   architecture="ALL", algorithm="TRADE_OFF", devices=40,
                   duration_s=120, seed=3, keep_tasks=True)
    print(tag, row["success_ratio"], row["task_delay_p95_s"], row["wall_clock_s"],
          row["task_delay_mean_s"], row.get("request_success_ratio"))
