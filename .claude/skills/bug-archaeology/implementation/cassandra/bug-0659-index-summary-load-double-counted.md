# Bug 0659: IndexSummaryRedistribution Double-Counts Load Metric

**Commit**: 4e834c53ca · **Ticket**: CASSANDRA-13738
**Subsystem**: src/java/org/apache/cassandra/io/sstable/format/SSTableReader.java
**Category**: Logic error in condition

---

## Root Cause

After resampling the index summary, `SSTableReader.buildSummaryAtLevel()` computed the new SSTable size via `bytesOnDisk()` and incremented the load metric by `newSize - oldSize`. However, when the resampled reader is committed as part of a `Transactional`, the framework independently records the new SSTable size as an addition. This caused the delta to be counted twice: once here explicitly, and once implicitly through the transactional commit pathway. The fix removes the explicit new-size increment and only decrements by `oldSize`, letting the transaction lifecycle add the updated size at commit time.

### Before / After

```java
// BEFORE
long newSize = bytesOnDisk();
StorageMetrics.load.inc(newSize - oldSize);
parent.metric.liveDiskSpaceUsed.inc(newSize - oldSize);
```

```java
// AFTER
// The new size will be added in Transactional.commit() as an updated SSTable
StorageMetrics.load.dec(oldSize);
parent.metric.liveDiskSpaceUsed.dec(oldSize);
```

---

## How to Spot

Metrics that track resource usage (disk space, memory) are especially prone to double-counting when a resource transitions through a lifecycle framework that itself fires accounting callbacks. Reviewers should trace all the paths that increment/decrement a metric around a lifecycle event and confirm that no single real-world change is counted more than once. Pay particular attention to transactional commit hooks.

---

## Test

The added test creates multiple SSTables, triggers index summary redistribution, and asserts that `StorageMetrics.load` reflects the correct total after redistribution without inflation.
