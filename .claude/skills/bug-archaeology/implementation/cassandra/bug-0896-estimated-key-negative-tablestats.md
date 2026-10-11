# Bug 0896: Estimated Partition Count Shows -1 in tablestats

**Commit**: 39b86e3792 · **Ticket**: CASSANDRA-11587
**Subsystem**: Nodetool / Metrics
**Category**: Wrong constant / default value

---

## Root Cause
`EstimatedRowCount` is a Gauge backed by an `EstimatedHistogram`. When a table has no SSTables (e.g. immediately after creation or after a truncate), the histogram is empty and `percentile(0.5)` returns `-1L` rather than `0`. The `TableStats` nodetool command printed this sentinel value directly, confusing operators into thinking the table had a negative partition count.

### Before / After
```java
// BEFORE
System.out.println("\t\tNumber of keys (estimate): " +
    probe.getColumnFamilyMetric(keyspaceName, tableName, "EstimatedRowCount"));

// AFTER
Object estimatedRowCount = probe.getColumnFamilyMetric(keyspaceName, tableName, "EstimatedRowCount");
if (Long.valueOf(-1L).equals(estimatedRowCount))
{
    estimatedRowCount = 0L;
}
System.out.println("\t\tNumber of keys (estimate): " + estimatedRowCount);
```

---

## How to Spot
Whenever a metric is sourced from an `EstimatedHistogram` and displayed in a human-facing tool, check what the histogram returns for empty input. The `-1` sentinel that `percentile()` emits on an empty histogram is an internal "no data" marker; any display path that does not guard against it will print a nonsensical negative value. Look for raw `.getColumnFamilyMetric(...)` calls in nodetool commands that are not filtered before printing.

---

## Test
No test added.
