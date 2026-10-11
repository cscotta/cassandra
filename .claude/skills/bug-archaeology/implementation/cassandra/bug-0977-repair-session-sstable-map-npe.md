# Bug 0977: NPE in ParentRepairSession.addSSTables on Incremental Repair Failure

**Commit**: 98cc2c8d6c · **Ticket**: CASSANDRA-10909
**Subsystem**: Repair
**Category**: Missing null / bounds check

---

## Root Cause
`ParentRepairSession` is constructed with a list of `ColumnFamilyStore` objects and is expected to maintain a per-CF set of `SSTableReader` objects in `sstableMap`. The original code initialised `sstableMap` as an empty `HashMap` in the constructor and only added per-CF entries inside `addSSTables()` using a get-or-create pattern. However, if an incremental repair failed partway through (before `addSSTables()` was ever called for a given CF), any code that subsequently called `addSSTables()` could encounter a `null` from `sstableMap.get(cfId)` and attempt to call `.addAll()` on it, producing an NPE.

The fix pre-populates `sstableMap` with an empty `HashSet` for every CF at construction time, so `sstableMap.get(cfId)` always returns a valid set and the null-check code in `addSSTables()` can be removed entirely.

### Before / After
```java
// BEFORE – sstableMap was empty at construction; addSSTables guarded against null
public void addSSTables(UUID cfId, Collection<SSTableReader> sstables)
{
    Set<SSTableReader> existingSSTables = this.sstableMap.get(cfId);
    if (existingSSTables == null)
        existingSSTables = new HashSet<>();
    existingSSTables.addAll(sstables);
    this.sstableMap.put(cfId, existingSSTables);  // guard was broken: new set was local
}

// AFTER – pre-populated in constructor, addSSTables is trivial
// constructor:
sstableMap.put(cfs.metadata.cfId, new HashSet<SSTableReader>());
// addSSTables:
sstableMap.get(cfId).addAll(sstables);
```

---

## How to Spot
The original `addSSTables` guard was actually broken in a subtle way: if `get()` returned `null`, it created a local `HashSet`, added to it, then re-put it — which is correct logic, but the fact that it was needed at all indicated the map was supposed to be pre-seeded at construction. Any time a map is supposed to contain one entry per object built in a constructor loop, look for missing pre-seeding in the constructor rather than relying on lazy creation in mutator methods.

---

## Test
No test added.
