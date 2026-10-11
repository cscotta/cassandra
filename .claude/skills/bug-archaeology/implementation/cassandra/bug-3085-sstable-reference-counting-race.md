# Bug 3085: Race condition in SSTable reference counting causes use-after-free

**Commit**: 8fd5ed8014 · **Ticket**: CASSANDRA-3085

## Detection Rule
When reading SSTables, if the code obtains a data-tracker view, then separately calls `acquireReferences()` on sstables from that view and retries on failure, the retry loop is racy and should be replaced with a single atomic `markReferenced()` operation.

## Code Smell
A `while (true)` loop that calls `cfs.getDataTracker().getView()`, builds a list of sstables, and then calls `SSTableReader.acquireReferences(sstables)` with a `continue` on failure is prone to races between view observation and reference acquisition.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: view-acquire-retry-loop-racy
- severity: crash
- subsystem: storage-engine

## Root Cause
`CollationController` used a spin loop: get the data tracker view, extract sstables, try to acquire references, and retry if acquisition failed. Between getting the view and acquiring references an sstable could be compacted away, making the retry necessary but still leaving a window where a use-after-free was possible. The fix introduces `cfs.markReferenced(filter.key)` which atomically returns a `ViewFragment` with references already held, eliminating the retry loop entirely.

## Example
```java
// BEFORE
while (true) {
    DataTracker.View dataview = cfs.getDataTracker().getView();
    List<SSTableReader> sstables = dataview.intervalTree.search(...);
    if (!SSTableReader.acquireReferences(sstables))
        continue; // retry w/ new view
    try { /* read */ }
    finally { SSTableReader.releaseReferences(sstables); }
    break;
}
```
```java
// AFTER
ColumnFamilyStore.ViewFragment view = cfs.markReferenced(filter.key);
try {
    for (SSTableReader sstable : view.sstables) { /* read */ }
} finally {
    SSTableReader.releaseReferences(view.sstables);
}
```
