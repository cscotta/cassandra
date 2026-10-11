# Bug 0426: Histogram Merge in ColumnFamilyMetrics Throws AIOOBE When New SSTable Has Fewer Buckets

**Commit**: e96b60bd9f · **Ticket**: CASSANDRA-16259
**Subsystem**: src/java/org/apache/cassandra/metrics/ColumnFamilyMetrics.java
**Category**: Missing null / bounds check

---

## Root Cause

The SSTable histogram merge loop iterated over `values.length` (the accumulated array) unconditionally, even when the newly-read histogram `nextBucket` was shorter. The "new bucket is larger" branch correctly allocated a new array and copied, but it erroneously iterated over `newValues.length` (the larger new size) while indexing `nextBucket[i]`, causing an `ArrayIndexOutOfBoundsException` when `nextBucket` was smaller. The "new bucket not larger" branch also iterated over `values.length` and indexed `nextBucket[i]`, which would throw whenever any subsequent SSTable histogram had fewer buckets than the accumulated array. The fix extracts a standalone `addHistogram` helper that always expands `sums` to at least `buckets.length` before iterating only up to `buckets.length`.

### Before / After

```java
// BEFORE (merge loop - broken when nextBucket is shorter)
if (nextBucket.length > values.length)
{
    long[] newValues = new long[nextBucket.length];
    System.arraycopy(firstBucket, 0, newValues, 0, firstBucket.length);
    for (int i = 0; i < newValues.length; i++)  // iterates past nextBucket end
        newValues[i] += nextBucket[i];
    values = newValues;
}
else
{
    for (int i = 0; i < values.length; i++)  // iterates past nextBucket end
        values[i] += nextBucket[i];
}
```

```java
// AFTER
public static long[] addHistogram(long[] sums, long[] buckets)
{
    if (buckets.length > sums.length)
        sums = Arrays.copyOf(sums, buckets.length);
    for (int i = 0; i < buckets.length; i++)
        sums[i] += buckets[i];
    return sums;
}
```

---

## How to Spot

When merging two arrays of different lengths, the iteration bound must be the length of the shorter (source) array, not the longer (destination) array. Any loop that iterates `destination.length` while reading from `source` without a prior length equality check is a candidate for this class of bug. Reviewers should also flag any loop that was written assuming both arrays are the same size when the surrounding context permits them to differ.

---

## Test

`ColumnFamilyMetricTest.testAddHistogram` directly tests `addHistogram` with a smaller array, equal-length array, empty array, and larger array, verifying both the returned values and whether a new array was allocated.
