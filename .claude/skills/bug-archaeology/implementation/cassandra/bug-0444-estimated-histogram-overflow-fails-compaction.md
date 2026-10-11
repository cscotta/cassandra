# Bug 0444: EstimatedHistogram Overflow Causes Compaction Failure on Very Large Partitions

**Commit**: 0361d53f4c · **Ticket**: CASSANDRA-15164
**Subsystem**: src/java/org/apache/cassandra/io/sstable/metadata/StatsMetadata.java, EstimatedHistogram.java, MetadataCollector.java
**Category**: Off-by-one

---

## Root Cause

`EstimatedHistogram` used a fixed-size bucket array. When a value exceeded the largest bucket offset it was accumulated into the last "overflow" bucket. During SSTable metadata serialization and deserialization, the overflow bucket was written and read as a normal count with no special handling. Downstream code (index summary resizing, compaction score calculations) read percentile and mean values from the histogram; an overflowed histogram returned nonsensical statistics — specifically an `ArrayIndexOutOfBoundsException` when computing the column-index downsampling bounds — which caused compactions to abort rather than produce a new SSTable. The default histogram bucket count (114) could only track up to ~2.4 billion cells per partition; tables with larger partitions silently overflowed and then failed on the next compaction that touched them.

### Before / After

```java
// BEFORE — bucket count too small; overflow silently accumulated
return new EstimatedHistogram(114); // max ~2.4B cells

// BEFORE — no overflow handling on deserialize
EstimatedHistogram columnCounts = EstimatedHistogram.serializer.deserialize(in);
```

```java
// AFTER — larger bucket count; overflow cleared on deserialize with a warning
return new EstimatedHistogram(118); // max ~4.1B cells

EstimatedHistogram columnCounts = EstimatedHistogram.serializer.deserialize(in);
if (columnCounts.isOverflowed()) {
    logger.warn("Clearing the overflow bucket...");
    columnCounts.clearOverflow();
}
```

---

## How to Spot

Histogram implementations with a fixed bucket count are vulnerable to overflow when workloads grow beyond the original design assumptions. Reviewers should verify that (a) the maximum tracked value is documented and cross-checked against realistic data sizes, and (b) deserialization paths that read previously-persisted histograms degrade gracefully rather than propagating overflow state into downstream arithmetic that can throw exceptions.

---

## Test

`EstimatedHistogramTest` adds tests for `overflowCount()`, `clearOverflow()`, and `isOverflowed()`. `MetadataSerializerTest` adds a round-trip test that seeds an overflowed histogram and asserts the deserialized form has a cleared overflow bucket.
