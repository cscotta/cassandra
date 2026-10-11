# Bug 1772: Fix Mean Cells and Mean Row Size per SSTable Calculation

**Commit**: beefd0b8cc · **Ticket**: CASSANDRA-6667
**Subsystem**: src/java/org/apache/cassandra/db/DataTracker.java, metrics/ColumnFamilyMetrics.java
**Category**: Wrong constant / default value

---

## Root Cause

`getMeanColumns()` and the `meanRowSize` gauge computed an unweighted arithmetic mean of per-SSTable means: they summed the mean value for each SSTable and divided by the SSTable count. This ignores the fact that different SSTables hold vastly different numbers of rows, so a tiny SSTable with an extreme mean value would skew the result just as much as a large SSTable. The fix replaces the per-SSTable count with the total sample count from each SSTable's `EstimatedHistogram`, producing a proper weighted average across all rows.

### Before / After

```java
// BEFORE – unweighted: counts SSTables, not rows
long sum = 0;
int count = 0;
for (SSTableReader sstable : getSSTables()) {
    sum += sstable.getEstimatedColumnCount().mean();
    count++;
}
return count > 0 ? (int) (sum / count) : 0;
```

```java
// AFTER – weighted by number of rows in each SSTable
long sum = 0;
long count = 0;
for (SSTableReader sstable : getSSTables()) {
    long n = sstable.getEstimatedColumnCount().count();
    sum += sstable.getEstimatedColumnCount().mean() * n;
    count += n;
}
return count > 0 ? (int) (sum / count) : 0;
```

---

## How to Spot

Whenever computing a "mean of means", ask whether the sub-groups have equal weight. If they don't, the aggregate must be weighted by the group size, not the group count. This pattern appears in any metric aggregation over heterogeneous partitions or SSTables.

---

## Test

No test added.
