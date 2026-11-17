"""Statistical utilities for the five research projects.

Everything is non-parametric by default: simulation outcomes are bounded, skewed,
and often bimodal (success/failure), so normal-theory intervals would be misleading.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Iterable, Sequence

import numpy as np
import pandas as pd
from scipy import stats as scipy_stats

DEFAULT_BOOTSTRAP = 10_000


def _clean(values: Iterable[float]) -> np.ndarray:
    array = np.asarray(list(values), dtype=float)
    return array[np.isfinite(array)]


def bootstrap_ci(values: Iterable[float], statistic: Callable[[np.ndarray], float] = np.mean,
                 alpha: float = 0.05, n_boot: int = DEFAULT_BOOTSTRAP, seed: int = 0) -> tuple[float, float]:
    """Percentile bootstrap confidence interval for any scalar statistic."""
    sample = _clean(values)
    if sample.size == 0:
        return float("nan"), float("nan")
    if sample.size == 1:
        return float(sample[0]), float(sample[0])
    rng = np.random.default_rng(seed)
    indices = rng.integers(0, sample.size, size=(n_boot, sample.size))
    draws = np.array([statistic(sample[index]) for index in indices])
    return float(np.quantile(draws, alpha / 2)), float(np.quantile(draws, 1 - alpha / 2))


def bootstrap_diff_ci(treatment: Iterable[float], control: Iterable[float],
                      statistic: Callable[[np.ndarray], float] = np.mean,
                      alpha: float = 0.05, n_boot: int = DEFAULT_BOOTSTRAP,
                      seed: int = 0) -> tuple[float, float]:
    """Bootstrap CI of ``statistic(treatment) - statistic(control)``."""
    a, b = _clean(treatment), _clean(control)
    if a.size == 0 or b.size == 0:
        return float("nan"), float("nan")
    rng = np.random.default_rng(seed)
    diffs = np.empty(n_boot)
    for i in range(n_boot):
        diffs[i] = statistic(a[rng.integers(0, a.size, a.size)]) - statistic(b[rng.integers(0, b.size, b.size)])
    return float(np.quantile(diffs, alpha / 2)), float(np.quantile(diffs, 1 - alpha / 2))


def mean_ci(values: Iterable[float], alpha: float = 0.05, n_boot: int = DEFAULT_BOOTSTRAP) -> dict:
    sample = _clean(values)
    lo, hi = bootstrap_ci(sample, np.mean, alpha, n_boot)
    return {
        "n": int(sample.size),
        "mean": float(sample.mean()) if sample.size else float("nan"),
        "sd": float(sample.std(ddof=1)) if sample.size > 1 else float("nan"),
        "ci_lo": lo,
        "ci_hi": hi,
    }


def wilson_interval(successes: int, total: int, z: float = 1.959963984540054) -> tuple[float, float]:
    """Wilson score interval for a binomial proportion (robust at 0 and 1)."""
    if total <= 0:
        return float("nan"), float("nan")
    p = successes / total
    denominator = 1 + z * z / total
    centre = (p + z * z / (2 * total)) / denominator
    margin = z * np.sqrt(p * (1 - p) / total + z * z / (4 * total * total)) / denominator
    return float(max(0.0, centre - margin)), float(min(1.0, centre + margin))


def cliffs_delta(a: Iterable[float], b: Iterable[float]) -> float:
    """Cliff's delta effect size in [-1, 1]; 0 means no stochastic ordering."""
    x, y = _clean(a), _clean(b)
    if x.size == 0 or y.size == 0:
        return float("nan")
    greater = sum(np.sum(x_i > y) for x_i in x)
    smaller = sum(np.sum(x_i < y) for x_i in x)
    return float((greater - smaller) / (x.size * y.size))


def mann_whitney(a: Iterable[float], b: Iterable[float]) -> dict:
    x, y = _clean(a), _clean(b)
    if x.size == 0 or y.size == 0:
        return {"u": float("nan"), "p_value": float("nan"), "delta": float("nan"), "n_a": int(x.size),
                "n_b": int(y.size)}
    result = scipy_stats.mannwhitneyu(x, y, alternative="two-sided")
    return {
        "u": float(result.statistic),
        "p_value": float(result.pvalue),
        "delta": cliffs_delta(x, y),
        "n_a": int(x.size),
        "n_b": int(y.size),
    }


def wilcoxon(a: Iterable[float], b: Iterable[float]) -> dict:
    """Paired test for matched seeds; falls back to nan when all differences are zero."""
    x, y = _clean(a), _clean(b)
    if x.size != y.size or x.size == 0:
        return {"statistic": float("nan"), "p_value": float("nan"), "n": int(min(x.size, y.size))}
    difference = x - y
    if np.allclose(difference, 0):
        return {"statistic": 0.0, "p_value": 1.0, "n": int(x.size)}
    result = scipy_stats.wilcoxon(x, y, zero_method="wilcox")
    return {"statistic": float(result.statistic), "p_value": float(result.pvalue), "n": int(x.size)}


def holm_bonferroni(p_values: Sequence[float]) -> list[float]:
    """Holm step-down correction, returned in the input order."""
    values = np.asarray(p_values, dtype=float)
    order = np.argsort(values)
    adjusted = np.empty_like(values)
    running = 0.0
    m = len(values)
    for rank, index in enumerate(order):
        candidate = (m - rank) * values[index]
        running = max(running, candidate)
        adjusted[index] = min(1.0, running)
    return adjusted.tolist()


def cohen_d(a: Iterable[float], b: Iterable[float]) -> float:
    x, y = _clean(a), _clean(b)
    if x.size < 2 or y.size < 2:
        return float("nan")
    pooled = np.sqrt(((x.size - 1) * x.var(ddof=1) + (y.size - 1) * y.var(ddof=1)) / (x.size + y.size - 2))
    return float((x.mean() - y.mean()) / pooled) if pooled > 0 else float("nan")


def summarize(frame: pd.DataFrame, value: str, by: Sequence[str],
              n_boot: int = 2000, alpha: float = 0.05) -> pd.DataFrame:
    """Mean, SD and bootstrap CI per group."""
    rows = []
    for keys, group in frame.groupby(list(by), dropna=False, sort=True):
        keys = keys if isinstance(keys, tuple) else (keys,)
        sample = group[value].to_numpy(dtype=float)
        sample = sample[np.isfinite(sample)]
        lo, hi = bootstrap_ci(sample, np.mean, alpha, n_boot)
        rows.append({
            **dict(zip(by, keys)),
            "n": int(sample.size),
            "mean": float(sample.mean()) if sample.size else float("nan"),
            "sd": float(sample.std(ddof=1)) if sample.size > 1 else float("nan"),
            "ci_lo": lo,
            "ci_hi": hi,
        })
    return pd.DataFrame(rows)


@dataclass
class Comparison:
    value: str
    treatment: str
    control: str
    treatment_mean: float
    control_mean: float
    difference: float
    difference_ci: tuple[float, float]
    p_value: float
    p_adjusted: float
    delta: float
    n_treatment: int
    n_control: int

    def as_dict(self) -> dict:
        return {
            "metric": self.value,
            "treatment": self.treatment,
            "control": self.control,
            "treatment_mean": self.treatment_mean,
            "control_mean": self.control_mean,
            "difference": self.difference,
            "diff_ci_lo": self.difference_ci[0],
            "diff_ci_hi": self.difference_ci[1],
            "p_value": self.p_value,
            "p_adjusted": self.p_adjusted,
            "cliffs_delta": self.delta,
            "n_treatment": self.n_treatment,
            "n_control": self.n_control,
        }


def compare(frame: pd.DataFrame, value: str, treatment_filter: dict, control_filter: dict,
            treatment_label: str, control_label: str, n_boot: int = 2000) -> Comparison:
    def select(filters: dict) -> np.ndarray:
        subset = frame
        for column, expected in filters.items():
            if isinstance(expected, (list, tuple, set)):
                subset = subset[subset[column].isin(expected)]
            else:
                subset = subset[subset[column] == expected]
        return subset[value].to_numpy(dtype=float)

    a, b = select(treatment_filter), select(control_filter)
    a, b = a[np.isfinite(a)], b[np.isfinite(b)]
    test = mann_whitney(a, b)
    return Comparison(
        value=value, treatment=treatment_label, control=control_label,
        treatment_mean=float(a.mean()) if a.size else float("nan"),
        control_mean=float(b.mean()) if b.size else float("nan"),
        difference=float(a.mean() - b.mean()) if a.size and b.size else float("nan"),
        difference_ci=bootstrap_diff_ci(a, b, n_boot=n_boot),
        p_value=test["p_value"], p_adjusted=test["p_value"], delta=test["delta"],
        n_treatment=int(a.size), n_control=int(b.size),
    )


def compare_paired(frame: pd.DataFrame, value: str, treatment_filter: dict, control_filter: dict,
                   treatment_label: str, control_label: str, n_boot: int = 2000,
                   seed_column: str = "seed") -> "Comparison":
    """Paired comparison: seeds are matched, Wilcoxon signed-rank on differences.

    Mann--Whitney assumes independent samples and is therefore the wrong test
    when the same seed appears in both arms.  This function aligns the arms by
    seed, tests the per-seed differences with Wilcoxon, and reports a paired
    dominance statistic (share of seeds favouring the treatment minus the share
    favouring the control) in the `delta` slot, labelled as such in the tables.
    """
    def select(filters: dict) -> pd.Series:
        subset = frame
        for column, expected in filters.items():
            if isinstance(expected, (list, tuple, set)):
                subset = subset[subset[column].isin(expected)]
            else:
                subset = subset[subset[column] == expected]
        return subset.groupby(seed_column)[value].mean()

    treatment = select(treatment_filter).rename("treatment")
    control = select(control_filter).rename("control")
    joined = pd.concat([treatment, control], axis=1).dropna()
    differences = (joined["treatment"] - joined["control"]).to_numpy(dtype=float)
    n = differences.size
    if n >= 6 and np.any(differences != 0):
        p_value = float(scipy_stats.wilcoxon(differences, zero_method="wilcox").pvalue)
    else:
        p_value = float("nan")
    if n:
        dominance = (float(np.mean(differences > 0)) - float(np.mean(differences < 0)))
    else:
        dominance = float("nan")
    ci = bootstrap_ci(differences, np.mean, n_boot=n_boot)
    return Comparison(
        value=value, treatment=treatment_label, control=control_label,
        treatment_mean=float(joined["treatment"].mean()) if n else float("nan"),
        control_mean=float(joined["control"].mean()) if n else float("nan"),
        difference=float(differences.mean()) if n else float("nan"),
        difference_ci=ci, p_value=p_value, p_adjusted=p_value, delta=dominance,
        n_treatment=int(n), n_control=int(n),
    )

def compare_many(frame: pd.DataFrame, values: Sequence[str], treatment_filter: dict, control_filter: dict,
                 treatment_label: str, control_label: str, n_boot: int = 2000) -> pd.DataFrame:
    comparisons = [compare(frame, value, treatment_filter, control_filter, treatment_label, control_label,
                           n_boot) for value in values]
    adjusted = holm_bonferroni([comparison.p_value for comparison in comparisons])
    rows = []
    for comparison, p_adjusted in zip(comparisons, adjusted):
        comparison.p_adjusted = p_adjusted
        rows.append(comparison.as_dict())
    return pd.DataFrame(rows)


def _dummy_block(values: np.ndarray) -> np.ndarray:
    """Full-rank dummy coding with the first level as the reference."""
    levels = sorted(set(values.tolist()))
    if len(levels) < 2:
        return np.zeros((len(values), 0))
    matrix = np.zeros((len(values), len(levels) - 1))
    for column, level in enumerate(levels[1:]):
        matrix[:, column] = (values == level).astype(float)
    return matrix


def factorial_anova(frame: pd.DataFrame, response: str, factors: Sequence[str],
                    covariates: Sequence[str] = ()) -> pd.DataFrame:
    """Sequential sums of squares for a (balanced) factorial design.

    In a perfectly balanced layout the effect subspaces are orthogonal, so the
    sequential sum of squares of each term is the unique ANOVA sum of squares:
    the table does not depend on the order in which factors enter, and the
    terms add up to the total.  Interactions are built as the element-wise
    products of the dummy blocks of their parent factors, and each model in
    the hierarchy contains every lower-order term, as it must.
    """
    import itertools

    data = frame.dropna(subset=[response, *factors, *covariates])
    y = data[response].to_numpy(dtype=float)
    n_obs = len(y)
    if n_obs < 2:
        return pd.DataFrame()

    arrays = {factor: data[factor].astype(str).to_numpy() for factor in factors}
    blocks: dict[tuple[str, ...], np.ndarray] = {}
    for size in range(1, len(factors) + 1):
        for combination in itertools.combinations(factors, size):
            product = None
            for factor in combination:
                block = _dummy_block(arrays[factor])
                if block.shape[1] == 0:
                    product = None
                    break
                if product is None:
                    product = block
                else:
                    product = (product[:, :, None] * block[:, None, :]).reshape(n_obs, -1)
            if product is not None and product.shape[1] > 0:
                blocks[tuple(combination)] = product

    covariates = list(covariates)
    terms = sorted(blocks, key=lambda key: (len(key), key))
    if not terms:
        return pd.DataFrame()

    def design(included: Sequence[tuple[str, ...]]) -> np.ndarray:
        parts = [np.ones((n_obs, 1))]
        for name in covariates:
            parts.append(data[name].to_numpy(dtype=float).reshape(-1, 1))
        for key in included:
            parts.append(blocks[key])
        return np.column_stack(parts)

    def rss(included: Sequence[tuple[str, ...]]) -> tuple[float, int]:
        matrix = design(included)
        beta, *_ = np.linalg.lstsq(matrix, y, rcond=None)
        residual = y - matrix @ beta
        return float(np.sum(residual ** 2)), int(np.linalg.matrix_rank(matrix))

    null_rss, null_rank = rss([])
    total_ss = float(np.sum((y - y.mean()) ** 2))

    rows = []
    if covariates:
        covariate_rss, covariate_rank = rss([])
        with_cov = design([])
        matrix = np.column_stack([with_cov] + [data[name].to_numpy(dtype=float).reshape(-1, 1)
                                               for name in covariates])
        beta, *_ = np.linalg.lstsq(matrix, y, rcond=None)
        covariate_rss = float(np.sum((y - matrix @ beta) ** 2))
        covariate_rank = int(np.linalg.matrix_rank(matrix))
        rows.append({"term": " + ".join(covariates) + " (covariates)",
                     "df": covariate_rank - null_rank,
                     "sum_sq": null_rss - covariate_rss})
        previous = (covariate_rss, covariate_rank)
    else:
        previous = (null_rss, null_rank)

    cumulative: list[tuple[str, ...]] = []
    for key in terms:
        cumulative.append(key)
        current = rss(cumulative)
        degrees = current[1] - previous[1]
        sum_squares = previous[0] - current[0]
        if degrees > 0:
            rows.append({"term": " x ".join(key), "df": int(degrees),
                         "sum_sq": float(sum_squares)})
        previous = current

    residual_ss, residual_rank = previous
    residual_df = n_obs - residual_rank
    if residual_df <= 0:
        return pd.DataFrame()
    denominator = residual_ss / residual_df
    for row in rows:
        row["mean_sq"] = row["sum_sq"] / row["df"]
        row["F"] = row["mean_sq"] / denominator if denominator > 0 else float("nan")
        row["p_value"] = (float(scipy_stats.f.sf(row["F"], row["df"], residual_df))
                          if np.isfinite(row["F"]) else float("nan"))
        total = row["sum_sq"] + residual_ss
        row["partial_eta_sq"] = row["sum_sq"] / total if total > 0 else float("nan")

    table = pd.DataFrame(rows, columns=["term", "df", "sum_sq", "mean_sq", "F",
                                        "p_value", "partial_eta_sq"])
    table.loc[len(table)] = {"term": "Residual", "df": residual_df, "sum_sq": residual_ss,
                             "mean_sq": denominator, "F": float("nan"),
                             "p_value": float("nan"), "partial_eta_sq": float("nan")}
    table.loc[len(table)] = {"term": "Total", "df": n_obs - 1, "sum_sq": total_ss,
                             "mean_sq": float("nan"), "F": float("nan"),
                             "p_value": float("nan"), "partial_eta_sq": float("nan")}
    cell_sizes = data.groupby(list(factors)).size() if factors else None
    table.attrs["n_obs"] = n_obs
    table.attrs["balanced"] = bool(cell_sizes.nunique() == 1) if cell_sizes is not None else True
    return table


def ols_with_cluster_robust(frame: pd.DataFrame, formula_y: str, predictors: Sequence[str],
                            cluster: str | None = None) -> pd.DataFrame:
    """Least squares with cluster-robust (CR1) standard errors.

    A tiny, dependency-free implementation used for the mixed-effects style
    analysis reported in the papers (seed is absorbed as a cluster).
    """
    data = frame.dropna(subset=[formula_y, *predictors]).copy()
    columns = []
    design = [np.ones(len(data))]
    columns.append("const")
    for name in predictors:
        series = data[name]
        if series.dtype == object or str(series.dtype).startswith("category"):
            dummies = pd.get_dummies(series, prefix=name, drop_first=True, dtype=float)
            for column in dummies.columns:
                design.append(dummies[column].to_numpy(dtype=float))
                columns.append(column)
        else:
            design.append(series.to_numpy(dtype=float))
            columns.append(name)
    X = np.column_stack(design)
    y = data[formula_y].to_numpy(dtype=float)
    beta, *_ = np.linalg.lstsq(X, y, rcond=None)
    residual = y - X @ beta
    if cluster is not None and cluster in data:
        meat = np.zeros((X.shape[1], X.shape[1]))
        groups = data[cluster].to_numpy()
        for group in np.unique(groups):
            mask = groups == group
            xg = X[mask]
            ug = residual[mask]
            score = xg.T @ ug
            meat += np.outer(score, score)
        g = len(np.unique(groups))
        n, k = X.shape
        correction = (g / (g - 1)) * ((n - 1) / max(1, n - k))
        covariance = correction * np.linalg.inv(X.T @ X) @ meat @ np.linalg.inv(X.T @ X)
    else:
        sigma2 = float(residual @ residual) / max(1, X.shape[0] - X.shape[1])
        covariance = sigma2 * np.linalg.inv(X.T @ X)
    errors = np.sqrt(np.diag(covariance))
    t_values = beta / errors
    p_values = 2 * (1 - scipy_stats.t.cdf(np.abs(t_values), max(1, X.shape[0] - X.shape[1])))
    return pd.DataFrame({
        "term": columns,
        "coef": beta,
        "std_error": errors,
        "t": t_values,
        "p_value": p_values,
        "ci_lo": beta - 1.96 * errors,
        "ci_hi": beta + 1.96 * errors,
    })


def feasibility_boundary(frame: pd.DataFrame, value: str, threshold: float,
                         by: Sequence[str], higher_is_better: bool = True) -> pd.DataFrame:
    """Largest setting for which the mean of ``value`` still meets ``threshold``."""
    rows = []
    for keys, group in frame.groupby(list(by), sort=True):
        keys = keys if isinstance(keys, tuple) else (keys,)
        ordered = group.sort_values(by[0])
        if higher_is_better:
            feasible = ordered[ordered[value] >= threshold]
        else:
            feasible = ordered[ordered[value] <= threshold]
        rows.append({
            **dict(zip(by, keys)),
            "boundary": float(feasible[by[0]].max()) if len(feasible) else float("nan"),
            "best_value": float(ordered[value].max() if higher_is_better else ordered[value].min()),
        })
    return pd.DataFrame(rows)
