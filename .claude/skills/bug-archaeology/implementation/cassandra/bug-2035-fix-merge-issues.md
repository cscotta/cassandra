# Bug 2035: IndexSummaryBuilder API Mismatch After Merge Left Missing indexInterval Parameter

**Commit**: 385ce13f5a · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/io/sstable/IndexSummaryBuilder.java, SSTableReader.java, SSTableWriter.java
**Category**: Wrong constant / default value

---

## Root Cause

A merge introduced `IndexSummaryBuilder` as a standalone class but left call sites in `SSTableReader` and `SSTableWriter` using an older two-argument signature for `maybeAddEntry()` and `build()` that did not accept `indexInterval`. The `indexInterval` was therefore silently ignored (or defaulted internally), meaning the index sampling rate was not respected during SSTable construction and loading. The fix introduces the correct three-argument `maybeAddEntry(key, indexInterval, position)` and two-argument `build(partitioner, indexInterval)` signatures and updates all call sites.

### Before / After

```java
// BEFORE
summaryBuilder.maybeAddEntry(decoratedKey, indexPosition);
indexSummary = summaryBuilder.build(partitioner);
```

```java
// AFTER
summaryBuilder.maybeAddEntry(decoratedKey, metadata.getIndexInterval(), indexPosition);
indexSummary = summaryBuilder.build(partitioner, metadata.getIndexInterval());
```

---

## How to Spot

After merging a refactored builder class, verify that all call sites pass all required parameters rather than relying on overloads that may silently drop configuration values like sampling intervals.

---

## Test

No test added.
