# Bug 0446: EstimatedHistogram Overflow Causes Compaction Failure on Very Large Partitions (2.2/3.0 Backport)

**Commit**: 4782fd399d · **Ticket**: CASSANDRA-15164
**Subsystem**: src/java/org/apache/cassandra/io/sstable/metadata/StatsMetadata.java, EstimatedHistogram.java, MetadataCollector.java
**Category**: Off-by-one

---

## Root Cause

This is an earlier-branch backport of the same fix as bugs 0444 and 0445. The root cause is identical: the `EstimatedHistogram` default bucket count of 114 overflowed for very large partitions, the overflow bucket was serialized as a normal count, and histogram-based arithmetic threw `ArrayIndexOutOfBoundsException` during compaction. The `StatsMetadata` serialization format in this branch differs from the 4.0/3.x branches (different fields are included based on the sstable version), but the histogram overflow logic is the same.

### Before / After

```java
// BEFORE
return new EstimatedHistogram(114); // overflow silently accumulates

// StatsMetadata deserializer had no overflow check
```

```java
// AFTER
return new EstimatedHistogram(118); // larger range; overflow handled

// Added to StatsMetadata deserializer:
if (partitionSizes.isOverflowed()) {
    logger.warn("Clearing the overflow bucket...");
    partitionSizes.clearOverflow();
}
```

---

## How to Spot

Same heuristic as bugs 0444 and 0445. Persisted fixed-size histogram data must be validated on load; the overflow sentinel bucket should be treated as exceptional and cleared with a diagnostic warning rather than silently propagating into arithmetic.

---

## Test

`EstimatedHistogramTest` and `MetadataSerializerTest` add overflow and round-trip coverage matching the other branch fixes.
