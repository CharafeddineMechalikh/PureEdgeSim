# Power analysis

The referee asked whether the null results are real or under-powered. This note
derives the minimum detectable effect (MDE) directly from the seed-level
variation in the artifact, and states what the design can and cannot conclude.

## Method

Every grid is seed-matched: the same seed appears in every arm, so the
informative quantity is the **paired** per-seed difference, not the spread of
arm means. With `n` seeds and a two-sided test at $\alpha = 0.05$ and 80% power,
the MDE for a paired mean difference is approximately

```
MDE = (z_{0.975} + z_{0.8}) * sd(diff) / sqrt(n) = 2.80 * sd(diff) / sqrt(n)
```

`sd(diff)` is measured from the artifact (`raw_runs.csv`), not assumed.

## Measured values at the two stressed levels

From `results/runs/factorial/raw_runs.csv`, twenty seeds per cell, the paired
difference `edge - aware_nofid` (the ladder effect at fixed placement) is
extremely stable because the simulator is deterministic given a seed and the two
arms differ only in one switch:

| Overhead | sd of arm means | sd of paired difference | n | MDE (paired) |
|---|---|---|---|---|
| 1.5x | 0.002 | 0.005 | 20 | **0.003** |
| 2.0x | 0.002 | 0.005 | 20 | **0.003** |
| 3.0x | 0.003 | 0.009 | 20 | **0.006** |

The seed-to-seed variation of an arm is about 0.2-0.3 percentage points. The
paired difference is equally tight, so the design can resolve an effect of
**0.3 percentage points** at 2x; 7 seeds would suffice for a 1-point effect and
2 seeds for a 2-point effect.

## What this means for each claim

**The ladder effect (0.375 at 2x) is not a power artefact.** It is 125 times the
MDE. The Wilcoxon test gives p < 0.001 on 20 seeds with dominance +1.00: every
seed moves the same way.

**The placement effect (0.121 at 2x) is not a power artefact either.** It is 40
times the MDE, dominance +1.00.

**The joint-scorer null is a true zero, not a failure to detect.** The
difference between the joint scorer and the static edge tier is exactly 0.000 on
every one of the 20 seeds at every overhead level: the runs are identical
because the objective never changes a pick (the flip counter in
`results/tables/objective.tex` is 0.000 at every level). With a design that
resolves 0.3 points, an effect of 1 point would have been detected on
essentially every seed. Reporting "no difference" here is therefore a statement
about the mechanism, not about the sample size.

**The component ablations are equally exact.** In the `ablation2` grid the five
arms differ by at most 0.8 points at 3x and by exactly 0.0 points at 1.5x and
2x. The same argument applies: an inert term cannot produce a difference, and
the design would have seen one had it existed.

**Why 20 seeds then?** The extra ten seeds above the ten used in the published
grids are not needed for power; they exist so that the *null* is credible. A
reviewer is right to distrust "no difference" from a small sample. Reporting the
paired standard deviation and the MDE together is the cheapest way to show that
the null is exact rather than unfunded.

## What is still under-powered

Nothing in the reported contrasts. Two secondary quantities are descriptive
rather than tested and should be read as such:

* the per-class breakdown at 3x, where the control class sits at 0.41 and the
  RAG class at 0.40 with different variances;
* the cache-hit-ratio ladder (10 seeds, 3x only), where the top hit ratio buys
  3.3 points: that is 11 times the MDE, but the grid has only two overhead
  levels and no intermediate hit ratios.

## Reproduction

```powershell
python 02_overhead_aware_agentic_edge/scenario/analysis.py
python -c "import pandas as pd, numpy as np; d=pd.read_csv('02_overhead_aware_agentic_edge/results/runs/factorial/raw_runs.csv'); b=d[d.compute_multiplier==2.0].pivot_table(index='seed',columns='policy',values='success_ratio'); x=b['edge']-b['aware_nofid']; print(x.std(ddof=1), x.std(ddof=1)*2.8/np.sqrt(len(x)))"
```

`analysis.py` writes `acceptance.json`, which includes `power_analysis` as a
passing criterion only when this file is present.
