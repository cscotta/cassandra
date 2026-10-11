# Bug 0900: forceFlush Race Allows Empty-Memtable SSTable During Index Build

**Commit**: 66c8f2b7f7 · **Ticket**: CASSANDRA-9669
**Subsystem**: Memtable / Index Build
**Category**: Race condition / missing synchronization

---

## Root Cause
`ColumnFamilyStore.forceFlush()` first read the current `Memtable` reference and then iterated over all CFS instances (including index CFSs) to decide whether a flush was necessary, all without holding any lock on `data`. A concurrent flush completion could switch the active memtable between the initial `getView().getCurrentMemtable()` read and the loop body, causing the check to operate on a stale reference. The result was an incorrect decision about whether a new flush needed to be triggered during index builds, producing an empty or mismatched SSTable. Wrapping the entire check-and-dispatch block inside `synchronized (data)` eliminates the window.

### Before / After
```java
// BEFORE — read and loop not atomic
public ListenableFuture<ReplayPosition> forceFlush()
{
    Memtable current = data.getView().getCurrentMemtable();
    for (ColumnFamilyStore cfs : concatWithIndexes())
        if (!cfs.data.getView().getCurrentMemtable().isClean())
            return switchMemtableIfCurrent(current);
    return waitForFlushes();
}

// AFTER — entire operation synchronized
public ListenableFuture<ReplayPosition> forceFlush()
{
    synchronized (data)
    {
        Memtable current = data.getView().getCurrentMemtable();
        for (ColumnFamilyStore cfs : concatWithIndexes())
            if (!cfs.data.getView().getCurrentMemtable().isClean())
                return switchMemtableIfCurrent(current);
        return waitForFlushes();
    }
}
```

---

## How to Spot
Any time a method reads a shared mutable reference and then acts on it across a loop without holding the lock, there is a TOCTOU window. Look for patterns where `getView()` or similar snapshot accessors are called once and then used in a subsequent loop that may span multiple CFS instances, each of which can independently flush.

---

## Test
No test added in this commit (it is a targeted regression fix for CASSANDRA-9669).
