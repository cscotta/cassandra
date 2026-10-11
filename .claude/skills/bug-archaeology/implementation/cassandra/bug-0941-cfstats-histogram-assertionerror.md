# Bug 0941: AssertionError in nodetool cfstats from wrong histogram construction

**Commit**: 6e0395e5a8 · **Ticket**: CASSANDRA-11297 (backport of CASSANDRA-10859)
**Subsystem**: Tools / nodetool
**Category**: Missing null / bounds check

---

## Root Cause
`TableHistograms` received raw bucket-count arrays from JMX and tried to construct an `EstimatedHistogram` by first allocating a *new, empty* histogram of the same length to get its bucket offsets, then passing those offsets together with the data to a second constructor. That two-step approach produced a mismatch: `new EstimatedHistogram(n)` generates offsets for `n` buckets, while the data array length was `n+1` (data arrays are always one element longer than the offset array). The resulting `assert bucketData.length == offsets.length + 1` fired as an `AssertionError`. The fix adds a single-array constructor that derives offsets directly from the data length, and exposes `getLargestBucketOffset()` to avoid re-accessing the now-correctly-sized offset array by index.

### Before / After
```java
// BEFORE — separate offset/data arrays with length mismatch
long[] rowSizeBucketOffsets = new EstimatedHistogram(estimatedRowSize.length).getBucketOffsets();
EstimatedHistogram rowSizeHist = new EstimatedHistogram(rowSizeBucketOffsets, estimatedRowSize);
// crashes: offsets.length + 1 != estimatedRowSize.length in some cases

// AFTER — single-array constructor infers offsets correctly
EstimatedHistogram rowSizeHist = new EstimatedHistogram(estimatedRowSize);
```

---

## How to Spot
When constructing a histogram from an externally supplied bucket array, avoid deriving offsets from a freshly allocated histogram of a related length — the relationship between data-array length and offset-array length is n+1 vs n and any off-by-one in the source length triggers an assertion. Prefer a constructor that accepts the raw data and computes offsets internally.

---

## Test
No new test added (fix is in histogram construction logic validated by nodetool integration).
