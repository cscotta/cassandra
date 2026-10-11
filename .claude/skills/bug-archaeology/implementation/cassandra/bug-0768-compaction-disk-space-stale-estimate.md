# Bug 0768: Compaction Disk Space Check Uses Stale Size Estimate

**Commit**: dbe039f628 · **Ticket**: CASSANDRA-12979
**Subsystem**: Compaction
**Category**: Logic error in condition

---

## Root Cause
`checkAvailableDiskSpace` computed `expectedWriteSize` once before the loop that repeatedly drops the largest SSTable from the compaction set to reduce scope. After each scope reduction the set of input SSTables changed, but the size estimate was never recalculated, so subsequent iterations of the space-check loop still used the original inflated estimate. This caused the compaction to either throw a spurious `RuntimeException` (not enough space) or skip necessary reductions because the stale estimate still cleared the threshold.

The fix restructures the check into an unconditional loop that recalculates both `expectedWriteSize` and `estimatedSSTables` from the *current* (already-reduced) transaction set on every iteration before calling `hasAvailableDiskSpace`.

### Before / After
```java
// BEFORE: size computed once outside the loop; loop can never converge correctly
long expectedWriteSize = cfs.getExpectedCompactedFileSize(transaction.originals(), compactionType);
long earlySSTableEstimate = Math.max(1, expectedWriteSize / strategy.getMaxSSTableBytes());
checkAvailableDiskSpace(earlySSTableEstimate, expectedWriteSize);

protected void checkAvailableDiskSpace(long estimatedSSTables, long expectedWriteSize)
{
    while (!cfs.directories.hasAvailableDiskSpace(estimatedSSTables, expectedWriteSize))
    {
        if (!reduceScopeForLimitedSpace())
            throw new RuntimeException(...);
    }
}

// AFTER: size recomputed inside the loop from the current (shrunk) originals set
protected void checkAvailableDiskSpace()
{
    while(true)
    {
        long expectedWriteSize = cfs.getExpectedCompactedFileSize(transaction.originals(), compactionType);
        long estimatedSSTables = Math.max(1, expectedWriteSize / strategy.getMaxSSTableBytes());
        if (cfs.directories.hasAvailableDiskSpace(estimatedSSTables, expectedWriteSize))
            break;
        if (!reduceScopeForLimitedSpace(expectedWriteSize))
            throw new RuntimeException(...);
    }
}
```

---

## How to Spot
Any time a loop checks a resource constraint and also modifies the inputs that determine that constraint, verify that the constraint value is recomputed after each modification. Here, the disk-space estimate is a function of `transaction.originals()`, which `reduceScopeForLimitedSpace` shrinks; lifting the computation outside the loop broke the invariant that the check always reflects the actual remaining work.

---

## Test
No test added (integration-level behavior requiring actual disk-space pressure).
