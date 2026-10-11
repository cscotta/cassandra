# Bug 3006: Memtable flush submitted to global MemtableManager, racing with concurrent flushes of same CF

**Commit**: 74dcfb263c · **Ticket**: CASSANDRA-9

## Detection Rule
When submitting a memtable flush, submit it to a per-column-family executor rather than a shared global flusher, so flushes for the same column family are serialized and do not race against each other.

## Code Smell
`MemtableManager.instance().submit(cfName, memtable, cLogCtx)` is called directly in `put()`, submitting flushes to a single global executor that may process concurrent flushes for the same column family out of order.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: global-executor-per-resource-races
- severity: data-loss
- subsystem: storage-engine

## Root Cause
Memtable flush submission called `MemtableManager.instance().submit()` which used a shared global thread pool. Multiple column families could therefore have their flushes interleaved or reordered. The fix introduces a per-CF `apartments_` map of `ExecutorService` instances and a `Flusher` inner `Runnable`. When a memtable becomes full, a `Flusher` is submitted to the CF-specific executor, ensuring all flushes for a given CF are serialized in order.

## Example
```java
// BEFORE: shared global flusher, unordered across CFs
MemtableManager.instance().submit(cfStore.getColumnFamilyName(), this, cLogCtx);

// AFTER: per-CF executor serializes flushes
Runnable flusher = new Flusher(cLogCtx);
apartments_.get(cfName_).submit(flusher);
cfStore.switchMemtable(key, columnFamily, cLogCtx);
```
