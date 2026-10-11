# Bug 2070: NPE in SizeTieredCompactionStrategy When No SSTables Are Available for Major Compaction

**Commit**: 55dda732bd · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/SizeTieredCompactionStrategy.java
**Category**: Missing null / bounds check

---

## Root Cause

`SizeTieredCompactionStrategy.getMaximalTask()` calls `cfs.markAllCompacting()` to claim all SSTables for a major compaction. That method returns `null` when there are no SSTables to mark (e.g. the table is empty or all SSTables are already being compacted). The result was passed directly into the `CompactionTask` constructor without a null check, causing an NPE. The fix returns `null` early, which the caller in the compaction manager already handles as "nothing to do."

### Before / After

```java
// BEFORE – NPE when markAllCompacting() returns null
public AbstractCompactionTask getMaximalTask(final int gcBefore)
{
    Collection<SSTableReader> sstables = cfs.markAllCompacting();
    return new CompactionTask(cfs, sstables, gcBefore);
}
```

```java
// AFTER – early return on null
public AbstractCompactionTask getMaximalTask(final int gcBefore)
{
    Collection<SSTableReader> sstables = cfs.markAllCompacting();
    if (sstables == null)
        return null;
    return new CompactionTask(cfs, sstables, gcBefore);
}
```

---

## How to Spot

Methods that "mark" or "claim" a set of resources for exclusive use commonly return null or an empty collection to signal that nothing is available. The return value must always be null-checked before constructing a task or passing the collection to downstream code that assumes it is non-null.

---

## Test

No test added.
