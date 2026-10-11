# Bug 0765: checkAvailableDiskSpace uses stale size estimate after scope reduction

**Commit**: e9251e1fbc · **Ticket**: CASSANDRA-12979
**Subsystem**: Compaction / disk space management
**Category**: Logic error in condition

---

## Root Cause
`CompactionTask.checkAvailableDiskSpace` was passed the expected write size and estimated SSTable count computed once before the loop. Each iteration of the loop could drop the largest SSTable from the task via `reduceScopeForLimitedSpace()`, but the disk-space check on the next iteration still used the original (larger) size values. This meant that after one SSTable was dropped, the space check would still see the un-reduced estimate and might drop additional SSTables unnecessarily — or conversely might not realize enough space had been freed. The fix moves the size calculation inside the loop so it is recomputed against the current (reduced) set of SSTables on every iteration.

### Before / After
```java
// BEFORE — size computed once, passed in, never updated after scope reduction
long expectedWriteSize = cfs.getExpectedCompactedFileSize(transaction.originals(), compactionType);
long earlySSTableEstimate = Math.max(1, expectedWriteSize / strategy.getMaxSSTableBytes());
checkAvailableDiskSpace(earlySSTableEstimate, expectedWriteSize);

protected void checkAvailableDiskSpace(long estimatedSSTables, long expectedWriteSize) {
    while (!getDirectories().hasAvailableDiskSpace(estimatedSSTables, expectedWriteSize)) {
        if (!reduceScopeForLimitedSpace())
            throw new RuntimeException(...);
    }
}

// AFTER — size recomputed inside loop after each scope reduction
protected void checkAvailableDiskSpace() {
    CompactionStrategyManager strategy = cfs.getCompactionStrategyManager();
    while (true) {
        long expectedWriteSize = cfs.getExpectedCompactedFileSize(transaction.originals(), compactionType);
        long estimatedSSTables = Math.max(1, expectedWriteSize / strategy.getMaxSSTableBytes());
        if (cfs.getDirectories().hasAvailableDiskSpace(estimatedSSTables, expectedWriteSize))
            break;
        if (!reduceScopeForLimitedSpace(expectedWriteSize))
            throw new RuntimeException(...);
    }
}
```

---

## How to Spot
Any loop that shrinks a work set and then re-checks a capacity bound must recompute the capacity estimate from the reduced set on each iteration. Precomputing a bound outside the loop and referencing it unchanged inside is a classic "stale estimate" bug when the basis of the estimate can be mutated by the loop body.

---

## Test
No test added (fix is in production path; NEWS.txt documents the behavioral change).
