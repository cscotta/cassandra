# Bug 0766: checkAvailableDiskSpace uses stale size estimate after scope reduction (3.0 backport)

**Commit**: b3ffdf8c4a · **Ticket**: CASSANDRA-12979
**Subsystem**: Compaction / disk space management
**Category**: Logic error in condition

---

## Root Cause
Same root cause as bug-0765: `CompactionTask.checkAvailableDiskSpace` was passed a pre-computed expected write size and SSTable count that were never updated after `reduceScopeForLimitedSpace()` dropped SSTables from the task. This is the 3.0 branch backport of the same fix applied in commit e9251e1fbc (bug-0765).

### Before / After
```java
// BEFORE — size computed once before the loop; never refreshed after scope reduction
long expectedWriteSize = cfs.getExpectedCompactedFileSize(transaction.originals(), compactionType);
long earlySSTableEstimate = Math.max(1, expectedWriteSize / strategy.getMaxSSTableBytes());
checkAvailableDiskSpace(earlySSTableEstimate, expectedWriteSize);

// AFTER — recomputed inside loop on every iteration
protected void checkAvailableDiskSpace() {
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
See bug-0765. Any loop that shrinks a work set and re-checks a capacity bound must recompute the bound from the shrunken set on every iteration, not use a value calculated before the loop.

---

## Test
No test added.
