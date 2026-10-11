# Bug 0855: Compaction Disk-Space Check Reports Bogus Available-Space Value

**Commit**: f531c67d68 · **Ticket**: n/a
**Subsystem**: Compaction
**Category**: Race condition / missing synchronization

---

## Root Cause
`CompactionAwareWriter.getWriteDirectory()` called `d.getAvailableSpace()` twice: once in the `if` guard and again inside the exception message.  Between the two calls the filesystem state could change, so the error message could print a value that was different from (and potentially larger than) the value that triggered the threshold, making the error look spurious.  Additionally, the loop that identified a target directory iterated all input SSTables but never `break`ed when it found that two SSTables came from different disks; it continued iterating needlessly and the final `directory` value was correct only by accident.

### Before / After
```java
// BEFORE – getAvailableSpace() called twice; loop never breaks on first mismatch
if (d.getAvailableSpace() < estimatedWriteSize)
    throw new RuntimeException(String.format("Not enough space ... (%s available)",
        FBUtilities.prettyPrintMemory(d.getAvailableSpace())));  // ← second call, stale value

// BEFORE – loop body without break
if (!directory.equals(sstable.descriptor.directory))
    logger.trace("All sstables not from the same disk ...");

// AFTER – single snapshot of available space; loop breaks immediately on first mismatch
long availableSpace = d.getAvailableSpace();
if (availableSpace < estimatedWriteSize)
    throw new RuntimeException(String.format("Not enough space ... (%s available)",
        FBUtilities.prettyPrintMemory(availableSpace)));

if (!directory.equals(sstable.descriptor.directory))
{
    logger.trace("All sstables not from the same disk ...");
    break;
}
```

---

## How to Spot
Any time the same getter is called in a condition and again to format an error message, capture the result in a local variable so both uses are consistent.  This pattern is especially important for filesystem/resource queries whose values change asynchronously.  Also audit loops that react to a first-mismatch condition: if subsequent iterations cannot improve the result, add a `break` to avoid wasted work and potential confusion.

---

## Test
No test added.
