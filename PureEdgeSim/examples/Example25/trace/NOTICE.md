# Azure LLM inference trace (code completion workload)

Replayed by [Example 25](../README.md).

## Source

- File published by Microsoft: `data/AzureLLMInferenceTrace_code.csv` of the
  [`Azure/AzurePublicDataset`](https://github.com/Azure/AzurePublicDataset)
  repository, 320,117 bytes, retrieved from `master` on 2026-10-28.
- Described in `AzureLLMInferenceDataset2023.md` of the same repository as a
  sample of the traces of "multiple LLM inference services in Azure", with the
  columns `TIMESTAMP`, `ContextTokens` and `GeneratedTokens`.
- The dataset is the one characterized in:

  > Pratyush Patel, Esha Choukse, Chaojie Zhang, Aashaka Shah, Íñigo Goiri,
  > Saeed Maleki, Ricardo Bianchini. "Splitwise: Efficient generative LLM
  > inference using phase splitting", Proceedings of the International
  > Symposium on Computer Architecture (ISCA), ACM, Buenos Aires, Argentina,
  > 2024.

## License

The data is made available by Microsoft under the Creative Commons Attribution
4.0 licence (`LICENSE` of the source repository). Redistribution of this
derived subset keeps the same terms and the attribution above. Only the three
published columns are used; no prompt text is included in the trace, because
the published dataset does not contain any.

## Transformation applied to the published file

1. Rows are sorted by `TIMESTAMP` (they already are in the published file).
2. `TIMESTAMP` is replaced by `arrival_ms`, the integer number of milliseconds
   between the invocation and the first invocation of the sample. The absolute
   date is dropped: the simulation replays relative offsets, so the run does
   not depend on the wall clock of the day the trace was taken. The published
   description gives 2023-11-11 as the collection day, while the timestamps of
   the released file run from 18:17:03 to 19:14:19 on 2023-11-16; the offsets
   are taken from the file as published.
3. `ContextTokens` and `GeneratedTokens` are copied without any change.

No row was removed, added or resampled. The file keeps the 8,819 invocations of
the published sample, spanning 3,435.948 s (mean of 2.57 requests per second),
which is the whole point of using it: the arrival process is bursty in a way
that a random generator does not reproduce.

| | mean | median | p90 | p99 | max |
|--|--|--|--|--|--|
| inter-arrival (s) | 0.390 | 0.064 | 0.273 | 2.205 | 217.2 |
| context tokens | 2047.8 | 1469 | 5194 | 7436 | 7437 |
| generated tokens | 27.9 | 13 | 55 | 252 | 1899 |

The three rows are quantiles of the 8818 gaps and of the two token columns, taken
from the millisecond offsets of this file. The coefficient of variation of the
inter-arrival time is 13.15, against 1 for an exponential (Poisson) process.

The burstiness, measured on the whole the example replays (each arrival rounded to
its second, the 58 minutes of a `simulation_time=58` run):

| Measure | Value | Against the hourly mean |
| --- | --- | --- |
| Requests in the busiest sliding 10-second window | 412 | 16 times the 25.7 expected |
| Requests in the busiest sliding 60-second window | 722 | 4.7 times the 154 expected |
| Requests in the busiest single second | 68 | |
| Minutes of the run that contain nothing | 12 of 58 | 12 gaps exceed 60 s, the longest is 217 s |
| Distinct seconds occupied by the 8819 requests | 945 | 99.6 percent of requests share their second |
