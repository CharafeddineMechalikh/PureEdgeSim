"""LaTeX table generation with booktabs conventions."""
from __future__ import annotations

from pathlib import Path
from typing import Mapping, Sequence

import numpy as np
import pandas as pd

SPECIALS = {
    "\\": r"\textbackslash{}",
    "&": r"\&",
    "%": r"\%",
    "$": r"\$",
    "#": r"\#",
    "_": r"\_",
    "{": r"\{",
    "}": r"\}",
    "~": r"\textasciitilde{}",
    "^": r"\textasciicircum{}",
}


def escape(value: str) -> str:
    """Escape text for LaTeX while leaving math and existing escapes intact.

    Column headers and notes routinely carry inline math (``$z$``) or an
    already-escaped percent sign.  A naive per-character escape turns those
    into ``\\$z\\$`` and ``\\textbackslash{}\\%``, so math spans are copied
    verbatim and a backslash followed by a known escape character is kept.
    """
    out: list[str] = []
    index, in_math, size = 0, False, len(value)
    while index < size:
        char = value[index]
        if char == "$":
            in_math = not in_math
            out.append(char)
        elif in_math:
            out.append(char)
        elif char == "\\" and index + 1 < size and value[index + 1] in "\\&%$#_{}":
            out.append(value[index:index + 2])
            index += 1
        else:
            out.append(SPECIALS.get(char, char))
        index += 1
    return "".join(out)


def _format(value, digits: int, scientific: bool) -> str:
    if value is None:
        return "--"
    if isinstance(value, str):
        return escape(value)
    if isinstance(value, (bool, np.bool_)):
        return "yes" if value else "no"
    try:
        number = float(value)
    except (TypeError, ValueError):
        return escape(str(value))
    if not np.isfinite(number):
        return "--"
    if scientific:
        return f"{number:.{digits}e}"
    if abs(number) >= 10 and float(number).is_integer():
        return f"{number:,.0f}"
    if abs(number) >= 10_000:
        return f"{number:,.0f}"
    return f"{number:.{digits}f}"


def to_booktabs(frame: pd.DataFrame, caption: str, label: str, *,
                digits: int = 3, scientific: bool = False,
                headers: Mapping[str, str] | None = None,
                align: str | None = None,
                notes: str | None = None,
                size: str = r"\small",
                wide: bool | None = None,
                midrule_after: Sequence[int] = ()) -> str:
    """Render a DataFrame as a complete ``table`` environment.

    Tables with six or more columns default to the two-column ``table*``
    float, which is the only way a wide table is legible in an IEEEtran
    manuscript.  Pass ``wide=False`` to force a single-column float.
    """
    headers = headers or {}
    columns = list(frame.columns)
    alignment = align or ("l" + "c" * (len(columns) - 1))
    span = len(columns) >= 6 if wide is None else wide
    lines = [
        r"\begin{table*}" if span else r"\begin{table}[t]",
        r"\centering",
        size,
        rf"\caption{{{caption}}}",
        rf"\label{{{label}}}",
        rf"\begin{{tabular}}{{{alignment}}}",
        r"\toprule",
        " & ".join(escape(str(headers.get(column, column))) for column in columns) + r" \\",
        r"\midrule",
    ]
    for index, (_, row) in enumerate(frame.iterrows()):
        cells = [_format(row[column], digits, scientific) for column in columns]
        lines.append(" & ".join(cells) + r" \\")
        if index + 1 in midrule_after:
            lines.append(r"\midrule")
    lines += [r"\bottomrule", r"\end{tabular}"]
    if notes:
        lines.append(rf"\par\vspace{{2pt}}{{\footnotesize\raggedright {notes}\par}}")
    lines.append(r"\end{table*}" if span else r"\end{table}")
    return "\n".join(lines) + "\n"


def write(frame: pd.DataFrame, path: Path | str, caption: str, label: str, **kwargs) -> Path:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(to_booktabs(frame, caption, label, **kwargs), encoding="utf-8")
    return target


def stars(p_value: float) -> str:
    if not np.isfinite(p_value):
        return ""
    if p_value < 0.001:
        return r"$^{***}$"
    if p_value < 0.01:
        return r"$^{**}$"
    if p_value < 0.05:
        return r"$^{*}$"
    return ""


def comparison_table(comparisons: pd.DataFrame, caption: str, label: str,
                     metrics: Mapping[str, str] | None = None,
                     digits: int = 3,
                     effect_label: str = "Cliff's $\\delta$",
                     notes: str | None = None) -> str:
    metrics = metrics or {}
    rows = []
    for _, row in comparisons.iterrows():
        difference = row["difference"]
        significant = np.isfinite(difference)
        rows.append({
            "Metric": metrics.get(row["metric"], row["metric"]),
            "Control": _format(row["control_mean"], digits, False),
            "Treatment": _format(row["treatment_mean"], digits, False),
            "Difference": _format(difference, digits, False)
                          + (stars(row.get("p_adjusted", row.get("p_value", np.nan)))
                             if significant and difference != 0 else ""),
            "95\\% CI": f"[{_format(row['diff_ci_lo'], digits, False)}, "
                        f"{_format(row['diff_ci_hi'], digits, False)}]",
            effect_label: _format(row["cliffs_delta"], 2, False),
        })
    frame = pd.DataFrame(rows)
    return to_booktabs(frame, caption, label, align="lcccccc",
                       notes=(notes or
                              r"Holm--Bonferroni adjusted: $^{*}p<0.05$, $^{**}p<0.01$, $^{***}p<0.001$. "
                              r"CI = bootstrap 95\% confidence interval of the difference."))


def descriptive_table(frame: pd.DataFrame, caption: str, label: str,
                      columns: Mapping[str, str], digits: int = 3,
                      group: str | None = None) -> str:
    data = frame.copy()
    if group:
        data = data.rename(columns={group: "Setting"})
        order = ["Setting"] + [column for column in columns if column != "Setting"]
    else:
        order = [column for column in columns]
    subset = data[order].copy()
    for column in subset.columns:
        if pd.api.types.is_float_dtype(subset[column]):
            subset[column] = subset[column].map(lambda value: _format(value, digits, False))
    return to_booktabs(subset, caption, label, headers=columns, digits=digits)
