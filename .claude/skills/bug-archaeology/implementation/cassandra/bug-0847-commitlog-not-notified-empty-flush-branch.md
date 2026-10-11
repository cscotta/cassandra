# Bug 0847: CommitLog not notified when empty memtable is flushed (branch variant)

**Commit**: 5cef78af92 · **Ticket**: CASSANDRA-12436
**Subsystem**: CommitLog / Memtable
**Category**: State not cleaned up

---

## Root Cause
This is a branch-parallel fix for the same root cause as bug 0846 (CASSANDRA-12436). The earlier branch variant (0846) refactored the clean-memtable check into a separate `flushMemtable()` helper. This variant instead keeps the flush loop inline: the `Iterator.remove()` that silently skipped `replaceFlushed` for clean or truncated memtables is removed; all memtables are now marked as flushing first, and the clean/truncate guard is placed directly inside the loop before calling `memtable.flush()`, ensuring `replaceFlushed` is always called and the commit log is always notified.

### Before / After
```java
// BEFORE – clean memtables removed before replaceFlushed; commit log never notified
Iterator<Memtable> iter = memtables.iterator();
while (iter.hasNext()) {
    Memtable memtable = iter.next();
    memtable.cfs.data.markFlushing(memtable);
    if (memtable.isClean() || truncate) {
        memtable.cfs.replaceFlushed(memtable, Collections.emptyList());
        reclaim(memtable);
        iter.remove();
    }
}
for (Memtable memtable : memtables) {
    Collection<SSTableReader> readers = memtable.flush();
    memtable.cfs.replaceFlushed(memtable, readers);
    reclaim(memtable);
}

// AFTER – check moved inside flush loop; replaceFlushed always reached
for (Memtable memtable : memtables)
    memtable.cfs.data.markFlushing(memtable);
...
for (Memtable memtable : memtables) {
    Collection<SSTableReader> readers = Collections.emptyList();
    if (!memtable.isClean() && !truncate)
        readers = memtable.flush();
    memtable.cfs.replaceFlushed(memtable, readers);
    reclaim(memtable);
}
```

---

## How to Spot
Identical to bug 0846: any early-exit path in a flush routine that bypasses `replaceFlushed` prevents commit log segments from being recycled. When reviewing flush code, verify that every code path through the loop — including the clean/empty/truncate shortcut — still invokes `replaceFlushed`, because that call is the only hook that tells the commit log a table's data is safe on disk.

---

## Test
`CommitLogCQLTest` was added (same test suite as the 0846 variant) to verify that empty memtable flushes correctly trigger commit log segment cleanup.
