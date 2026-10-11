# Bug 1017: Avoid writing range tombstones after END_OF_ROW marker

**Commit**: 0b26ca6874 · **Ticket**: CASSANDRA-10791
**Subsystem**: SSTable / Storage Engine
**Category**: Wrong serialization / deserialization

---

## Root Cause
`ColumnIndex.Builder.build()` was responsible for both flushing any unwritten ("open") range tombstones to disk and constructing the final index object. When `SSTableWriter` iterated all atoms and called `build()`, any range tombstones that were still "open" at end-of-row were written to the data file inside `build()` — but this happened *after* `SSTableWriter` had already written the `END_OF_ROW` marker. The tombstone bytes thus appeared after the end-of-row sentinel, corrupting the SSTable format. The fix separates flushing tombstones (`finishAddingAtoms()`) from constructing the index object (`build()`), and asserts via `hasUnwrittenTombstones()` that no flush is pending at build time.

### Before / After
```java
// BEFORE — build() flushed tombstones and then built the index in one step;
//           SSTableWriter called build() AFTER writing END_OF_ROW
public ColumnIndex build() throws IOException
{
    long size = tombstoneTracker.writeUnwrittenTombstones(output, atomSerializer);
    endPosition += size;
    blockSize += size;
    if (lastColumn == null)
        return ColumnIndex.EMPTY;
    ...
}

// AFTER — tombstone flush split into its own method, called BEFORE END_OF_ROW
public void finishAddingAtoms() throws IOException
{
    long size = tombstoneTracker.writeUnwrittenTombstones(output, atomSerializer);
    endPosition += size;
    blockSize += size;
}

public ColumnIndex build()
{
    assert !tombstoneTracker.hasUnwrittenTombstones(); // must call finishAddingAtoms first
    if (lastColumn == null)
        return ColumnIndex.EMPTY;
    ...
}
```

---

## How to Spot
Whenever a two-phase write (data bytes followed by a sentinel/marker) delegates the last bytes of the data phase into a method that is also used for bookkeeping, there is a risk of writing out-of-order. In review, verify that any `flush()` or `finish()` call that emits bytes to an output stream is sequenced strictly before the surrounding code writes a terminator. Also watch for method names like `build()` that sound purely constructive but actually perform I/O as a side effect — this conflation of concerns is an indicator of the bug class.

---

## Test
`ScrubTest` and `StreamingTransferTest` were extended to cover SSTable data written with trailing range tombstones; scrubber tests verify that corrupted files are detected and repaired.
