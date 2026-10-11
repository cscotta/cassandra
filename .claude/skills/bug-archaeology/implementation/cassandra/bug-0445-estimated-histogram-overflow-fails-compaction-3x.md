# Bug 0445: EstimatedHistogram Overflow Causes Compaction Failure on Very Large Partitions (3.x Backport)

**Commit**: 5a39ff4078 · **Ticket**: CASSANDRA-15164
**Subsystem**: src/java/org/apache/cassandra/io/sstable/metadata/StatsMetadata.java, EstimatedHistogram.java, MetadataCollector.java
**Category**: Off-by-one

---

## Root Cause

This is the 3.x branch backport of the same fix as bug-0444. The root cause is identical: the `EstimatedHistogram` bucket count of 114 was insufficient for very large partitions, the overflow bucket was serialized without special handling, and downstream histogram arithmetic threw `ArrayIndexOutOfBoundsException` during compaction. The branch difference is that this version uses `ReplayPosition` instead of `CommitLogPosition` in `StatsMetadata`.

### Before / After

```java
// BEFORE — bucket count too small; no overflow guard on deserialize
return new EstimatedHistogram(114); // max ~2.4B cells

EstimatedHistogram columnCounts = EstimatedHistogram.serializer.deserialize(in);
// no overflow check
```

```java
// AFTER — larger bucket; overflow cleared with warning on deserialize
return new EstimatedHistogram(118); // max ~4.1B cells

EstimatedHistogram columnCounts = EstimatedHistogram.serializer.deserialize(in);
if (columnCounts.isOverflowed()) {
    logger.warn("Clearing the overflow bucket...");
    columnCounts.clearOverflow();
}
```

---

## How to Spot

Same heuristic as bug-0444: fixed-size histograms persisted to disk must handle overflow gracefully on deserialization and should have their maximum tracked value reviewed against realistic production data sizes.

---

## Test

`EstimatedHistogramTest` and `MetadataSerializerTest` add tests matching the 4.0 branch. The overflow/clear/round-trip scenarios are the same as in the 4.0 fix.
