# Bug 0962: Empty Hint Files Created on Idle Flush

**Commit**: 1f626087c8 · **Ticket**: CASSANDRA-11090
**Subsystem**: Hints
**Category**: Non-atomic file write

---

## Root Cause
`HintsWriteExecutor`'s flush loop called `flushInternal(iterator, store)` unconditionally at the top of every iteration, before checking whether the iterator had any data. When the hints buffer was empty (e.g., during a periodic flush triggered with no pending hints), `flushInternal` would create and open a new hint file, write zero records, and close it — producing empty hint files on disk that wasted space and cluttered hint directories.

### Before / After
```java
// BEFORE
while (true)
{
    flushInternal(iterator, store);   // always called, even when iterator is empty

    if (!iterator.hasNext())
        break;
    ...
}

// AFTER
while (true)
{
    if (iterator.hasNext())            // guard: only flush if there is data
        flushInternal(iterator, store);

    if (!iterator.hasNext())
        break;
    ...
}
```

---

## How to Spot
Flush loops that unconditionally invoke write/open operations and only check for emptiness after the fact are prone to this pattern. During review, look for any loop where a file-creation call appears before the `hasNext()` / `isEmpty()` guard; those two should be swapped or the guard should gate the entire write.

---

## Test
No test added.
