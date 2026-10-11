# Bug 3005: Premature columnFamilies_.clear() in Memtable.flush() causes data loss

**Commit**: af47dc8476 · **Ticket**: CASSANDRA-9

## Detection Rule
When flushing a memtable to disk, do not clear the in-memory data structure until after the flush has fully completed and been acknowledged, so that concurrent readers can still access the data during the flush.

## Code Smell
`columnFamilies_.clear()` is called unconditionally at the end of the flush path, even before callers have had a chance to remove the memtable from the pending-flush set, making the memtable's data disappear while it may still be queried.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: premature-clear-before-flush-complete
- severity: data-loss
- subsystem: storage-engine

## Root Cause
After writing all column families to an SSTable, `Memtable.flush()` called `columnFamilies_.clear()` at the end of the method. If a reader accessed the memtable between the clear and the point where the memtable was removed from the pending-flush set, it would find an empty `columnFamilies_` map and conclude no data existed. This could cause recently written but not-yet-flushed data to silently disappear from read results. The fix removes the `columnFamilies_.clear()` call.

## Example
```java
// BEFORE: data cleared before readers stop consulting this memtable
flushForRandomPartitioner(ssTable, cfStore, cLogCtx);
break;
}
columnFamilies_.clear(); // BUG: readers may still be here

// AFTER: clear removed
flushForRandomPartitioner(ssTable, cfStore, cLogCtx);
break;
}
```
