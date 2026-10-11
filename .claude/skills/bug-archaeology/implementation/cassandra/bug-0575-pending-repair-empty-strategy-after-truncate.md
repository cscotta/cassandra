# Bug 0575: Empty Compaction Strategy Entries Persist After Truncate

**Commit**: 44cffc0b16 · **Ticket**: CASSANDRA-14780
**Subsystem**: `src/java/org/apache/cassandra/db/compaction/PendingRepairManager.java`
**Category**: State not cleaned up

---

## Root Cause

`PendingRepairManager` maintains a map of pending-repair session IDs to `AbstractCompactionStrategy` instances. When SSTables were removed (e.g., on truncate), `removeSSTable` iterated the map and removed the SSTable from each strategy but never pruned strategies that became empty as a result. Similarly, `replaceSSTables` could leave an empty strategy entry after replacing SSTables. A subsequent call to `getNextBackgroundTask` would find an empty strategy and attempt to create a compaction task for it, producing an empty (no-op) task. The fix adds a `removeSessionIfEmpty` call after each mutation so entries are cleaned up immediately when their last SSTable is removed.

### Before / After

```java
// BEFORE
synchronized void removeSSTable(SSTableReader sstable)
{
    for (AbstractCompactionStrategy strategy : strategies.values())
        strategy.removeSSTable(sstable);
}
```

```java
// AFTER
synchronized void removeSSTable(SSTableReader sstable)
{
    for (Map.Entry<UUID, AbstractCompactionStrategy> entry : strategies.entrySet())
    {
        entry.getValue().removeSSTable(sstable);
        removeSessionIfEmpty(entry.getKey());
    }
}
```

---

## How to Spot

When a manager class owns a map from session/key to a collection-backed object, removing items from the inner collection must also check whether the outer entry should be evicted. Review all removal paths (remove, replace, swap) in such managers and ensure each one calls the cleanup guard. A method named `removeSession` that only fires from one code path (session completion) is a sign that SSTable-level removal paths were overlooked.

---

## Test

The new `noEmptyCompactionTask` test creates a pending-repair SSTable, registers it with `PendingRepairManager`, truncates the table (removing the SSTable), and asserts that `getNextBackgroundTask` returns `null` rather than an empty compaction task.
