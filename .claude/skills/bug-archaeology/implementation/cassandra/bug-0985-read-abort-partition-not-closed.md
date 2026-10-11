# Bug 0985: Read Abort Leaves Open Range Tombstone Bound After Partition Close

**Commit**: 3c6dfa4aa0 · **Ticket**: CASSANDRA-10951
**Subsystem**: Read / Query execution
**Category**: State not cleaned up

---

## Root Cause
`StopIterator.maybeAbort()` called `stop()` to signal abort but then returned `void`, so the caller continued iterating and returned the current partition/row instead of stopping. The abort signal was issued too late and the partition was not closed, leaving the iterator in an inconsistent open state. This was a regression introduced by CASSANDRA-7392; the fix makes `maybeAbort()` return a boolean and immediately closes the partition on abort so downstream consumers see `null` rather than a live partition reference.

### Before / After
```java
// BEFORE
protected BaseRowIterator<?> applyToPartition(BaseRowIterator partition)
{
    maybeAbort();
    return partition;  // returned even when aborted
}

private void maybeAbort()
{
    if (isAborted())
        stop();
    if (TEST_ITERATION_DELAY_MILLIS > 0)
        maybeDelayForTesting();
}

// AFTER
protected BaseRowIterator<?> applyToPartition(BaseRowIterator partition)
{
    if (maybeAbort())
    {
        partition.close();
        return null;
    }
    return partition;
}

private boolean maybeAbort()
{
    if (TEST_ITERATION_DELAY_MILLIS > 0)
        maybeDelayForTesting();
    if (isAborted())
    {
        stop();
        return true;
    }
    return false;
}
```

---

## How to Spot
Any `applyToPartition` / `applyToRow` callback that checks an abort or stop condition but still returns the live object is a latent bug. Review every transformation that calls `stop()` and verify the return value is `null` so the iterator framework discards the entry.

---

## Test
No test added.
