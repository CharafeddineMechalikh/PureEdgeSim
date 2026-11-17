from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from research_common.runs import run_once, KillSchedule, LinkDropSchedule, BatteryDrainSchedule

BASE = Path("03_feasibility_certified_fault_tolerant_edge/scenario/settings")
row = run_once(base_settings=BASE, out_dir=Path("research_common/_smoke") / "p3kill",
               architecture="ALL", algorithm="TRADE_OFF", devices=30, duration_s=120, seed=5,
               injectors=[lambda m, e: KillSchedule(m, e, [(30.0, "spread", 4), (60.0, "battery_first", 4)]),
                          lambda m, e: LinkDropSchedule(m, e, [(45.0, 10.0, "highest_load", 3)]),
                          lambda m, e: BatteryDrainSchedule(m, e, [(80.0, "random", 3, 0.5)])])
print("p3", row["success_ratio"], row["fail_death"], row["fail_mobility"], row["injections"]["kill"])

BASE5 = Path("05_crosslayer_iscc_mec_6g/scenario/settings")
row5 = run_once(base_settings=BASE5, out_dir=Path("research_common/_smoke") / "p5",
                architecture="MIST_AND_EDGE", algorithm="TRADE_OFF", devices=30, duration_s=120, seed=5)
print("p5", row5["task_success_fraction"], row5.get("request_success_ratio"),
      row5.get("request_makespan_mean_s"), row5["fail_dependency"])
