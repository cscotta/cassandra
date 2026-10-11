# Bug 0846: CommitLog not notified when empty memtable is flushed

**Commit**: ca556c4420 · **Ticket**: CASSANDRA-12436
**Subsystem**: CommitLog / Memtable
**Category**: State not cleaned up

---

## Root Cause
`ColumnFamilyStore.Flush` used to skip calling `replaceFlushed` (and therefore the commit-log clean-interval notification) for memtables that were already clean or were being flushed as part of a truncate. The skipping happened by removing those memtables from the flush list _before_ iterating and flushing, meaning the call to `CommitLog.instance.discardCompletedSegments` was never made for the corresponding `cfId`. Commit log segments covering ranges already reflected in those empty memtables could never be marked as safe-to-discard, so they accumulated on disk until the node ran out of commit-log space.

### Before / After
```java
// BEFORE – clean/truncated memtables removed from flush list; replaceFlushed never called
Iterator<Memtable> iter = memtables.iterator();
while (iter.hasNext()) {
    Memtable memtable = iter.next();
    memtable.cfs.data.markFlushing(memtable);
    if (memtable.isClean() || truncate) {
        memtable.cfs.replaceFlushed(memtable, Collections.emptyList());
        reclaim(memtable);
        iter.remove();   // removed before flushMemtable is called
    }
}

// AFTER – all memtables are marked flushing; clean/truncated check moved into flushMemtable
for (Memtable memtable : memtables)
    memtable.cfs.data.markFlushing(memtable);
...
// Inside flushMemtable():
if (memtable.isClean() || truncate) {
    memtable.cfs.replaceFlushed(memtable, Collections.emptyList());
    reclaim(memtable);
    return Collections.emptyList();
}
```

---

## How to Spot
Any early-exit path in a flush routine that skips `replaceFlushed` will silently prevent commit log segments from being recycled. Look for places where `isClean()` (or similar) causes a memtable to be removed from the iteration before `replaceFlushed` is invoked. The symptom is commit log segments accumulating on disk without bound, often only visible under low-write-rate conditions where empty flushes are common.

---

## Test
`CommitLogCQLTest` was added with a test that triggers empty memtable flushes and asserts that the commit log segment is correctly cleaned up afterward.
