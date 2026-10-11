# Bug 1201: StreamSession Uses Non-Canonical SSTable Instances During Streaming (2.0 Backport)

**Commit**: 98ac45a8a4 · **Ticket**: CASSANDRA-9700
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamSession.java
**Category**: Race condition / missing synchronization

---

## Root Cause

This is the 2.0 branch backport of the same defect fixed in bug 1200. `StreamSession` used `view.sstablesInBounds()` to enumerate candidates and then checked membership against a separately obtained canonical list. Because the bounds view can surface "early opened" SSTable instances that are not the same Java objects as the canonical ones, the `List.contains` call could fail for SSTables that should have been included. The fix replaces the list-based membership test with a `HashMap` keyed on canonical instances so that any early-opened instance is swapped out for its canonical counterpart.

### Before / After

```java
// BEFORE
List<SSTableReader> filteredSSTables = ColumnFamilyStore.CANONICAL_SSTABLES.apply(view);
for (AbstractBounds<RowPosition> rowBounds : rowBoundsList)
    for (SSTableReader sstable : view.sstablesInBounds(rowBounds))
        if (filteredSSTables.contains(sstable) && (!isIncremental || !sstable.isRepaired()))
            sstables.add(sstable);
```

```java
// AFTER
Map<SSTableReader, SSTableReader> permittedInstances = new HashMap<>();
for (SSTableReader reader : ColumnFamilyStore.CANONICAL_SSTABLES.apply(view))
    permittedInstances.put(reader, reader);

for (AbstractBounds<RowPosition> rowBounds : rowBoundsList) {
    sstable = permittedInstances.get(sstable);
    if (sstable != null) sstables.add(sstable);
}
```

---

## How to Spot

Same heuristic as bug 1200: any cross-check between a range-query result set and a "canonical" allowed set must confirm that both sets contain the identical object instances. Discrepancies arise when a subsystem can produce shadow or early-opened variants of the same logical entity. Canonical-map lookups are the robust solution.

---

## Test

No test added.
