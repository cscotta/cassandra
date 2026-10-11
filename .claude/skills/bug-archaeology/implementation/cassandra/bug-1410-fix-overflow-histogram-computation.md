# Bug 1410: Fix Overflow on Histogram Computation

**Commit**: 9685622b32 · **Ticket**: CASSANDRA-8028
**Subsystem**: src/java/org/apache/cassandra/metrics/ColumnFamilyMetrics.java
**Category**: Missing null / bounds check

---

## Root Cause

The histogram gauge for estimated row size and column count accumulated per-SSTable bucket arrays by indexing into a fixed-size array of 90 elements (`new long[90]`). `EstimatedHistogram` can return bucket arrays longer than 90 when data overflows the default range, so the loop would read beyond the actual length of `rowSize[]` with an `ArrayIndexOutOfBoundsException`, or silently truncate data from larger histograms. Additionally, `NodeTool` always constructed a histogram with the default 90-bucket offsets, ignoring the actual length returned from the metric, causing mismatches between offsets and bucket values when histograms had grown.

### Before / After

```java
// BEFORE
long[] histogram = new long[90];
for (SSTableReader sstable : cfs.getSSTables())
{
    long[] rowSize = sstable.getEstimatedRowSize().getBuckets(false);
    for (int i = 0; i < histogram.length; i++)
        histogram[i] += rowSize[i];
}
return histogram;
```

```java
// AFTER
return combineHistograms(cfs.getSSTables(), new GetHistogram()
{
    public EstimatedHistogram getHistogram(SSTableReader reader)
    {
        return reader.getEstimatedRowSize();
    }
});
// combineHistograms() dynamically expands the accumulator to match the
// longest bucket array encountered, then sums only up to each array's length.
```

---

## How to Spot

Any place that merges variable-length arrays using a fixed-size accumulator is suspect. When accumulating histogram data from a dynamic source, the accumulator size must be derived from the actual data, not a hard-coded constant. Look for `new long[90]` (or any magic constant) used as a histogram accumulator alongside `getBuckets()` calls that may return arrays of differing lengths.

---

## Test

No test added.
