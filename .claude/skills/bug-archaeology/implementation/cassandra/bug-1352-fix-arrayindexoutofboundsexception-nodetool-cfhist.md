# Bug 1352: ArrayIndexOutOfBoundsException in nodetool cfhistograms When No SSTables Exist

**Commit**: c468c8b436 · **Ticket**: CASSANDRA-8514
**Subsystem**: src/java/org/apache/cassandra/tools/NodeProbe.java
**Category**: Missing null / bounds check

---

## Root Cause

`metricPercentilesAsArray` in `NodeProbe` received an empty (zero-length) `counts` array when no SSTables exist for a column family, then passed it to `EstimatedHistogram(counts.length)` which produced bucket offsets of length 0. Constructing the histogram with zero buckets caused `ArrayIndexOutOfBoundsException` when percentile calculations tried to index into the array. The callers in `NodeTool.java` also had no guard against this case and would attempt to print misleading NaN values rather than a meaningful "no data" message.

### Before / After

```java
// BEFORE — no guard, crashes with AIOOBE when counts is empty
public double[] metricPercentilesAsArray(long[] counts)
{
    double[] offsetPercentiles = new double[] { 0.5, 0.75, 0.95, 0.98, 0.99 };
    long[] offsets = new EstimatedHistogram(counts.length).getBucketOffsets();
    EstimatedHistogram metric = new EstimatedHistogram(offsets, counts);
    double[] result = new double[7];
    ...
```

```java
// AFTER — early return with NaN when input is empty
public double[] metricPercentilesAsArray(long[] counts)
{
    double[] result = new double[7];

    if (isEmpty(counts))
    {
        Arrays.fill(result, Double.NaN);
        return result;
    }

    double[] offsetPercentiles = new double[] { 0.5, 0.75, 0.95, 0.98, 0.99 };
    long[] offsets = new EstimatedHistogram(counts.length).getBucketOffsets();
    EstimatedHistogram metric = new EstimatedHistogram(offsets, counts);
    ...
```

---

## How to Spot

Any method that feeds an externally-sourced array into a data structure constructor or index operation without first checking for emptiness is a candidate for this class of bug. When JMX attributes can legitimately return empty arrays (for instance when no data has been written), callers should check `isEmpty` before indexing or constructing histogram objects whose size is derived from the array length. Code review should treat bare array-length-derived constructions as a red flag whenever the input can have zero length.

---

## Test

No test added. The fix is exercised indirectly by running `nodetool cfhistograms` against a keyspace with no SSTables, but no automated unit test was included in this commit.
