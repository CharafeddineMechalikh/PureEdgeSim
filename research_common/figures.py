"""Publication-grade matplotlib helpers.

Every figure is written as both PDF (vector, for submission) and PNG (300 dpi,
for review).  The style follows the conventions of IEEE/ACM/Elsevier two-column
papers: serif text, thin spines, no chartjunk, colour-blind safe palette and
distinct markers/hatches so that figures remain readable in grayscale.
"""
from __future__ import annotations

from pathlib import Path
from typing import Sequence

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.ticker import MaxNLocator

PALETTE = ["#0072B2", "#D55E00", "#009E73", "#CC79A7", "#E69F00", "#56B4E9", "#F0E442", "#000000"]
MARKERS = ["o", "s", "^", "D", "v", "P", "X", "*"]
HATCHES = ["", "//", "\\\\", "..", "xx", "++", "oo", "--"]
LINESTYLES = ["-", "--", "-.", ":", (0, (5, 1)), (0, (3, 1, 1, 1))]


def setup(font_size: float = 9.0, columns: int = 1) -> None:
    """Configure a deterministic, two-column-friendly style."""
    width = 3.5 if columns == 1 else 7.16
    plt.rcParams.update({
        "figure.figsize": (width, width * 0.62),
        "figure.dpi": 120,
        "savefig.dpi": 300,
        "savefig.bbox": "tight",
        "font.family": "serif",
        "font.serif": ["DejaVu Serif", "Times New Roman", "Times"],
        "font.size": font_size,
        "axes.titlesize": font_size + 1,
        "axes.labelsize": font_size,
        "axes.linewidth": 0.6,
        "axes.grid": True,
        "grid.linewidth": 0.3,
        "grid.alpha": 0.35,
        "grid.linestyle": ":",
        "legend.fontsize": font_size - 1,
        "legend.frameon": False,
        "xtick.labelsize": font_size - 1,
        "ytick.labelsize": font_size - 1,
        "xtick.direction": "out",
        "ytick.direction": "out",
        "xtick.major.width": 0.6,
        "ytick.major.width": 0.6,
        "lines.linewidth": 1.2,
        "lines.markersize": 4,
        "pdf.fonttype": 42,
        "ps.fonttype": 42,
    })


def display(text):
    """Turn a LaTeX-escaped caption fragment into plain matplotlib text.

    Analysis scripts are shared with the LaTeX table writer, where a percent
    sign must be written as a backslash-escaped percent.  Matplotlib has no such
    escape, so it would print the backslash literally.  Only the escapes that
    have a plain-text equivalent are removed; inline math such as ``$z$`` is
    left untouched.
    """
    if not isinstance(text, str):
        return text
    for source, target in (("\\%", "%"), ("\\_", "_"), ("\\&", "&"), ("\\#", "#")):
        text = text.replace(source, target)
    return text

def _prepare(path: Path | str) -> tuple[Path, Path]:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    return target.with_suffix(".pdf"), target.with_suffix(".png")


def save(fig, path: Path | str) -> list[Path]:
    pdf, png = _prepare(path)
    fig.savefig(pdf)
    fig.savefig(png)
    plt.close(fig)
    return [pdf, png]


def _style_axis(ax) -> None:
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.yaxis.set_major_locator(MaxNLocator(nbins=5))
    ax.xaxis.set_major_locator(MaxNLocator(nbins=6))


def line_with_ci(frame: pd.DataFrame, x: str, y: str, hue: str, path: Path | str,
                 ci_lo: str | None = "ci_lo", ci_hi: str | None = "ci_hi",
                 xlabel: str | None = None, ylabel: str | None = None, title: str | None = None,
                 ylim: tuple[float, float] | None = None, logx: bool = False,
                 annotate_last: bool = False, columns: int = 1) -> list[Path]:
    setup(columns=columns)
    fig, ax = plt.subplots()
    for index, (label, group) in enumerate(frame.groupby(hue, sort=False)):
        group = group.sort_values(x)
        colour = PALETTE[index % len(PALETTE)]
        marker = MARKERS[index % len(MARKERS)]
        ax.plot(group[x], group[y], label=str(label), color=colour, marker=marker,
                linestyle=LINESTYLES[index % len(LINESTYLES)])
        if ci_lo and ci_hi and ci_lo in group and ci_hi in group:
            ax.fill_between(group[x], group[ci_lo], group[ci_hi], color=colour, alpha=0.16, linewidth=0)
        if annotate_last and len(group):
            last = group.iloc[-1]
            ax.annotate(f"{last[y]:.2f}", (last[x], last[y]), textcoords="offset points",
                        xytext=(4, 3), fontsize=7, color=colour)
    if logx:
        ax.set_xscale("log")
    if ylim:
        ax.set_ylim(*ylim)
    ax.set_xlabel(display(xlabel or x))
    ax.set_ylabel(display(ylabel or y))
    if title:
        ax.set_title(display(title))
    _style_axis(ax)
    try:
        values = sorted(pd.unique(frame[x]))
    except TypeError:
        values = []
    numeric = all(isinstance(v, (int, float, np.integer, np.floating)) for v in values)
    if not logx and 1 < len(values) <= 8 and numeric:
        ax.set_xticks(values)
        ax.set_xticklabels([f"{float(v):g}" for v in values])
    ax.legend(loc="best")
    return save(fig, path)


def bar_with_ci(frame: pd.DataFrame, x: str, y: str, hue: str | None, path: Path | str,
                ci_lo: str | None = "ci_lo", ci_hi: str | None = "ci_hi",
                xlabel: str | None = None, ylabel: str | None = None, title: str | None = None,
                horizontal: bool = False, columns: int = 1) -> list[Path]:
    setup(columns=columns)
    fig, ax = plt.subplots()
    groups = list(frame[hue].unique()) if hue else [None]
    labels = list(frame[x].unique())
    width = 0.8 / max(1, len(groups))
    positions = np.arange(len(labels))
    for index, group in enumerate(groups):
        subset = frame if group is None else frame[frame[hue] == group]
        subset = subset.set_index(x).reindex(labels)
        offset = (index - (len(groups) - 1) / 2) * width
        values = subset[y].to_numpy(dtype=float)
        lower = upper = None
        if ci_lo and ci_hi and ci_lo in subset and ci_hi in subset:
            lower = values - subset[ci_lo].to_numpy(dtype=float)
            upper = subset[ci_hi].to_numpy(dtype=float) - values
            lower = np.nan_to_num(lower)
            upper = np.nan_to_num(upper)
        kwargs = dict(label=str(group) if group is not None else None,
                      color=PALETTE[index % len(PALETTE)], edgecolor="black", linewidth=0.4,
                      hatch=HATCHES[index % len(HATCHES)], width=width * 0.92)
        if horizontal:
            ax.barh(positions + offset, values, xerr=[lower, upper] if lower is not None else None,
                    error_kw={"elinewidth": 0.7, "capsize": 2}, **kwargs)
        else:
            ax.bar(positions + offset, values, yerr=[lower, upper] if lower is not None else None,
                   error_kw={"elinewidth": 0.7, "capsize": 2}, **kwargs)
    if horizontal:
        ax.set_xlabel(display(ylabel or y))
        ax.set_ylabel(display(xlabel or x))
    else:
        ax.set_ylabel(display(ylabel or y))
        ax.set_xlabel(display(xlabel or x))
    if title:
        ax.set_title(display(title))
    _style_axis(ax)
    # Categorical ticks must be installed *after* _style_axis, whose locator
    # otherwise discards a fixed labelling and leaves only one visible label.
    rotated = max((len(str(label)) for label in labels), default=0) > 10
    if horizontal:
        ax.set_yticks(positions)
        ax.set_yticklabels(labels)
    else:
        ax.set_xticks(positions)
        ax.set_xticklabels(labels, rotation=15 if rotated else 0,
                           ha="right" if rotated else "center")
    if hue:
        ax.legend(loc="best", ncols=2)
    return save(fig, path)


def stacked_bars(frame: pd.DataFrame, x: str, y: str, stack: str, path: Path | str,
                 xlabel: str | None = None, ylabel: str | None = None, title: str | None = None,
                 normalise: bool = False, columns: int = 1) -> list[Path]:
    setup(columns=columns)
    fig, ax = plt.subplots()
    pivot = frame.pivot_table(index=x, columns=stack, values=y, aggfunc="mean", fill_value=0.0)
    if normalise:
        pivot = pivot.div(pivot.sum(axis=1).replace(0, np.nan), axis=0).fillna(0.0)
    labels = [str(value) for value in pivot.index]
    positions = np.arange(len(labels))
    bottom = np.zeros(len(labels))
    for index, column in enumerate(pivot.columns):
        values = pivot[column].to_numpy(dtype=float)
        ax.bar(positions, values, bottom=bottom, label=str(column), color=PALETTE[index % len(PALETTE)],
               edgecolor="black", linewidth=0.35, hatch=HATCHES[index % len(HATCHES)])
        bottom += values
    ax.set_xlabel(display(xlabel or x))
    ax.set_ylabel(display(ylabel or y))
    if normalise:
        ax.set_ylim(0, 1)
        ax.yaxis.set_major_formatter(matplotlib.ticker.PercentFormatter(1.0))
    if title:
        ax.set_title(display(title))
    _style_axis(ax)
    rotated = max((len(str(label)) for label in labels), default=0) > 10
    ax.set_xticks(positions)
    ax.set_xticklabels(labels, rotation=15 if rotated else 0,
                       ha="right" if rotated else "center")
    ax.legend(loc="upper center", bbox_to_anchor=(0.5, -0.34),
              ncols=3 if len(pivot.columns) > 4 else min(3, len(pivot.columns)),
              fontsize=7, frameon=False)
    return save(fig, path)


def cdf(data: dict[str, Sequence[float]], path: Path | str, xlabel: str, ylabel: str = "P(X \u2264 x)",
        title: str | None = None, logx: bool = False, columns: int = 1) -> list[Path]:
    setup(columns=columns)
    fig, ax = plt.subplots()
    for index, (label, values) in enumerate(data.items()):
        sample = np.sort(np.asarray(values, dtype=float))
        sample = sample[np.isfinite(sample)]
        if sample.size == 0:
            continue
        y = np.arange(1, sample.size + 1) / sample.size
        ax.step(sample, y, where="post", label=str(label), color=PALETTE[index % len(PALETTE)],
                linestyle=LINESTYLES[index % len(LINESTYLES)])
    if logx:
        ax.set_xscale("log")
    ax.set_xlabel(display(xlabel))
    ax.set_ylabel(display(ylabel))
    if title:
        ax.set_title(display(title))
    _style_axis(ax)
    ax.legend(loc="lower right")
    return save(fig, path)


def heatmap(frame: pd.DataFrame, x: str, y: str, value: str, path: Path | str,
            xlabel: str | None = None, ylabel: str | None = None, title: str | None = None,
            cmap: str = "viridis", annotate: bool = True, columns: int = 1) -> list[Path]:
    setup(columns=columns)
    fig, ax = plt.subplots()
    pivot = frame.pivot_table(index=y, columns=x, values=value, aggfunc="mean")
    image = ax.imshow(pivot.to_numpy(dtype=float), aspect="auto", cmap=cmap, origin="lower")
    ax.set_xticks(range(len(pivot.columns)))
    ax.set_xticklabels([str(value) for value in pivot.columns], rotation=0)
    ax.set_yticks(range(len(pivot.index)))
    ax.set_yticklabels([str(value) for value in pivot.index])
    ax.set_xlabel(display(xlabel or x))
    ax.set_ylabel(display(ylabel or y))
    if annotate:
        values = pivot.to_numpy(dtype=float)
        threshold = np.nanmean(values) if np.isfinite(values).any() else 0
        for row in range(values.shape[0]):
            for column in range(values.shape[1]):
                ax.text(column, row, f"{values[row, column]:.2f}", ha="center", va="center", fontsize=6,
                        color="white" if values[row, column] < threshold else "black")
    colourbar = fig.colorbar(image, ax=ax)
    colourbar.set_label(value)
    if title:
        ax.set_title(display(title))
    ax.grid(False)
    return save(fig, path)


def scatter_pareto(frame: pd.DataFrame, x: str, y: str, hue: str, path: Path | str,
                   xlabel: str | None = None, ylabel: str | None = None, title: str | None = None,
                   columns: int = 1, leave_front: bool = True) -> list[Path]:
    setup(columns=columns)
    fig, ax = plt.subplots()
    for index, (label, group) in enumerate(frame.groupby(hue, sort=False)):
        ax.scatter(group[x], group[y], s=18, label=str(label), color=PALETTE[index % len(PALETTE)],
                   marker=MARKERS[index % len(MARKERS)], edgecolor="black", linewidth=0.3, alpha=0.9)
    if leave_front and len(frame):
        front = _pareto_front(frame, x, y)
        if len(front) > 1:
            front = front.sort_values(x)
            ax.plot(front[x], front[y], color="grey", linewidth=0.8, linestyle="--", label="Pareto front")
    ax.set_xlabel(display(xlabel or x))
    ax.set_ylabel(display(ylabel or y))
    if title:
        ax.set_title(display(title))
    _style_axis(ax)
    ax.legend(loc="best")
    return save(fig, path)


def _pareto_front(frame: pd.DataFrame, x: str, y: str) -> pd.DataFrame:
    ordered = frame[[x, y]].sort_values([x, y], ascending=[True, False])
    best = -np.inf
    keep = []
    for index, row in ordered.iterrows():
        if row[y] > best:
            best = row[y]
            keep.append(index)
    return frame.loc[keep]


def grouped_bars(frame: pd.DataFrame, x: str, y: str, hue: str, path: Path | str, **kwargs) -> list[Path]:
    return bar_with_ci(frame, x, y, hue, path, **kwargs)


def boxplot(frame: pd.DataFrame, x: str, y: str, hue: str | None, path: Path | str,
            xlabel: str | None = None, ylabel: str | None = None, title: str | None = None,
            columns: int = 1) -> list[Path]:
    setup(columns=columns)
    fig, ax = plt.subplots()
    labels = list(frame[x].unique())
    groups = list(frame[hue].unique()) if hue else [None]
    width = 0.8 / max(1, len(groups))
    positions = np.arange(len(labels))
    for index, group in enumerate(groups):
        subset = frame if group is None else frame[frame[hue] == group]
        data = [subset[subset[x] == label][y].to_numpy(dtype=float) for label in labels]
        offset = (index - (len(groups) - 1) / 2) * width
        box = ax.boxplot(data, positions=positions + offset, widths=width * 0.8, patch_artist=True,
                         showfliers=False, medianprops={"color": "black", "linewidth": 0.8})
        for patch in box["boxes"]:
            patch.set_facecolor(PALETTE[index % len(PALETTE)])
            patch.set_edgecolor("black")
            patch.set_linewidth(0.4)
            patch.set_alpha(0.85)
    ax.set_xticks(positions)
    ax.set_xticklabels([str(label) for label in labels])
    ax.set_xlabel(display(xlabel or x))
    ax.set_ylabel(display(ylabel or y))
    if title:
        ax.set_title(display(title))
    _style_axis(ax)
    if hue:
        handles = [matplotlib.patches.Patch(facecolor=PALETTE[i % len(PALETTE)], label=str(group))
                   for i, group in enumerate(groups)]
        ax.legend(handles=handles, loc="best")
    return save(fig, path)
