# Bug 0689: SSTable Read Count Reported Incorrectly Per Read

**Commit**: e22cb278b6 · **Ticket**: CASSANDRA-13120
**Subsystem**: src/java/org/apache/cassandra/db, src/java/org/apache/cassandra/io/sstable/format
**Category**: Logic error in condition

---

## Root Cause

`SinglePartitionReadCommand` called `sstable.incrementReadCount()` eagerly at the point each SSTable iterator was constructed, and maintained a parallel `sstablesIterated` counter. The problem was that `incrementReadCount()` was also called for SSTables whose partition tombstone did not meet the timestamp threshold and were immediately discarded — so SSTables that did not contribute any merged data were counted as "accessed". Furthermore, for range reads via `PartitionRangeReadCommand`, the scanner created by `BigTableScanner` never called `incrementReadCount()` at all. A new observer interface `SSTableReadsListener` was introduced so that the SSTable itself could call back into the command when it was definitively selected (after bloom filter, key cache, and index lookup), ensuring the count tracks only SSTables that truly participated in the merge and that scan reads are also counted.

### Before / After

```java
// BEFORE — count incremented before confirming SSTable is useful
sstable.incrementReadCount();
UnfilteredRowIterator iter = filter.filter(
    sstable.iterator(partitionKey(), columnFilter(), filter.isReversed(), isForThrift()));
sstablesIterated++;
```

```java
// AFTER — count incremented inside getPosition() only when selected
SSTableReadMetricsCollector metricsCollector = new SSTableReadMetricsCollector();
// onSSTableSelected() -> sstable.incrementReadCount() + mergedSSTables++
UnfilteredRowIterator iter = filter.filter(
    sstable.iterator(partitionKey(), columnFilter(), filter.isReversed(), isForThrift(), metricsCollector));
// ...
cfs.metric.updateSSTableIterated(metricsCollector.getMergedSSTables());
```

---

## How to Spot

Any place that increments a metric counter before checking the result of an early-exit operation (bloom filter, key cache, predicate evaluation) is susceptible to over-counting. Review sites where `incrementReadCount()` or a similar side-effecting call appears before conditional `continue`/`return` paths; if those paths skip the SSTable, the counter has already been inflated. The fix is to push the increment into the earliest definitive "we will read this" decision point.

---

## Test

`SSTablesIteratedTest` was added; it creates tables with controlled numbers of SSTables and asserts the exact value reported by `metric.updateSSTableIterated` for single-partition and range reads, including cache-hit scenarios.
