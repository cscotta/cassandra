# Bug 1351: Fix ArrayIndexOutOfBoundsException in nodetool cfhistograms

**Commit**: feda54f049 · **Ticket**: CASSANDRA-8514
**Subsystem**: src/java/org/apache/cassandra/tools/NodeTool.java
**Category**: Missing null / bounds check

---

## Root Cause

The `cfhistograms` command retrieved `EstimatedRowSizeHistogram` and `EstimatedColumnCountHistogram` from JMX and immediately passed them to `EstimatedHistogram` constructors to compute percentiles. When no SSTables existed for a table, the JMX attribute returned an empty array (`length == 0`). `EstimatedHistogram` derived its bucket offsets from the array length, producing a zero-length offset array, and then the percentile and min/max accessors indexed into those arrays unconditionally — throwing `ArrayIndexOutOfBoundsException`. The fix adds an upfront `isEmpty()` guard: when either histogram array is empty the command prints an informative message and fills all percentile slots with `NaN` rather than crashing.

### Before / After

```java
// BEFORE
long[] rowSizeBucketOffsets = new EstimatedHistogram(estimatedRowSize.length).getBucketOffsets();
EstimatedHistogram rowSizeHist = new EstimatedHistogram(rowSizeBucketOffsets, estimatedRowSize);
// ... immediately compute percentiles — crashes if estimatedRowSize is empty
```

```java
// AFTER
if (isEmpty(estimatedRowSize) || isEmpty(estimatedColumnCount))
{
    System.err.println("No SSTables exists, unable to calculate ...");
    for (int i = 0; i < 7; i++) {
        estimatedRowSizePercentiles[i] = Double.NaN;
        estimatedColumnCountPercentiles[i] = Double.NaN;
    }
}
else
{
    // construct histograms and compute percentiles only when data exists
}
```

---

## How to Spot

Any tool that consumes JMX histogram data must treat an empty array as a legitimate state (no data collected yet) rather than an error. Before constructing a histogram or accessing bucket arrays, check for emptiness. The general pattern: if a statistics object can be empty, every downstream operation that indexes into its internal arrays needs a guard or the empty case needs a dedicated code path.

---

## Test

No test added.
