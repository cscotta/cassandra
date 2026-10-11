# Bug 0279: Out-of-Range Exception on Column Index Downsampling from Overflowed Histogram

**Commit**: 0e855c4b7c · **Ticket**: CASSANDRA-17839
**Subsystem**: io/sstable/format/big/BigTableWriter
**Category**: Missing null / bounds check

---

## Root Cause

`BigTableWriter` computes the optimal I/O buffer size for a new SSTable by calling
`EstimatedHistogram.percentile()` on the partition-size histogram.  When all observed partition
sizes exceeded the histogram's largest bucket the overflow counter was incremented but the bucket
array itself was not updated; calling `percentile()` on an overflowed histogram produced an
index beyond the array bounds, throwing an `ArrayIndexOutOfBoundsException`.  The histogram was
also sized at 150 buckets (max trackable value ~1.5 PB), which is unreachably large and wasted
memory; the fix raised it to 155 buckets (~3.5 TB) and added an explicit overflow guard that
clears the overflow bucket before the percentile query.

### Before / After

```java
// BEFORE – no overflow guard; percentile() on overflow throws AIOOBE
int dataBufferSize = optimizationStrategy.bufferSize(
    stats.estimatedPartitionSize.percentile(
        DatabaseDescriptor.getDiskOptimizationEstimatePercentile()));
```

```java
// AFTER – overflow detected and cleared before percentile call
EstimatedHistogram partitionSizeHistogram = stats.estimatedPartitionSize;
if (partitionSizeHistogram.isOverflowed())
{
    logger.warn("Estimated partition size histogram ... is overflowed ...");
    partitionSizeHistogram.clearOverflow();
}
int dataBufferSize = optimizationStrategy.bufferSize(
    partitionSizeHistogram.percentile(
        DatabaseDescriptor.getDiskOptimizationEstimatePercentile()));
```

---

## How to Spot

Calls to statistical methods such as `percentile()` or `mean()` on `EstimatedHistogram` must be
preceded by an overflow check whenever the histogram may have received values larger than its
maximum bucket.  During review, flag any direct `histogram.percentile(...)` call that lacks a
prior `isOverflowed()` guard—especially in write or compaction paths where partition sizes are
user-controlled and may be arbitrarily large.

---

## Test

No new test added for the overflow guard specifically; the accompanying histogram bucket-count
change from 150 to 155 is covered by existing histogram unit tests.
