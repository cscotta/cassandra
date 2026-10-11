# Bug 0965: DTCS getMaximalTask Mixes Repaired and Unrepaired SSTables

**Commit**: dd23adf141 · **Ticket**: CASSANDRA-11113
**Subsystem**: Compaction / DateTieredCompactionStrategy
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`DateTieredCompactionStrategy.getMaximalTask` used `cfs.markAllCompacting()`, which marks and returns all SSTables managed by the `ColumnFamilyStore` — including both repaired and unrepaired ones — bypassing the strategy's own `sstables` set that tracks only the SSTables DTCS is responsible for. This caused major compactions to mix repaired and unrepaired SSTables in the same task, violating the invariant that repair status boundaries must be respected. The fix replaces the `markAllCompacting()` call with the strategy's own `sstables` field (filtered through `filterSuspectSSTables`) and then manually marks only those SSTables as compacting via `cfs.getDataTracker().markCompacting(...)`.

### Before / After
```java
// BEFORE
public synchronized Collection<AbstractCompactionTask> getMaximalTask(int gcBefore)
{
    Iterable<SSTableReader> sstables = cfs.markAllCompacting();   // all SSTables, ignores repair status
    if (sstables == null)
        return null;
    return Collections.singleton(new CompactionTask(cfs, sstables, gcBefore, false));
}

// AFTER
public synchronized Collection<AbstractCompactionTask> getMaximalTask(int gcBefore)
{
    Iterable<SSTableReader> filteredSSTables = filterSuspectSSTables(sstables); // strategy-owned set, no suspects
    if (Iterables.isEmpty(filteredSSTables))
        return null;
    if (!cfs.getDataTracker().markCompacting(ImmutableList.copyOf(filteredSSTables)))
        return null;
    return Collections.singleton(new CompactionTask(cfs, filteredSSTables, gcBefore, false));
}
```

---

## How to Spot
Any `getMaximalTask` or similar "compact everything" override that calls a CFS-level "mark all" helper rather than iterating the strategy's own SSTable set is likely to bypass repair-boundary isolation. During review, verify that strategies with a private `sstables` field use that field (not a CFS-wide accessor) in `getMaximalTask` and that the subsequent `markCompacting` call is explicit — using `markAllCompacting()` as a shortcut silently ignores every filter the strategy would normally apply.

---

## Test
No test added.
