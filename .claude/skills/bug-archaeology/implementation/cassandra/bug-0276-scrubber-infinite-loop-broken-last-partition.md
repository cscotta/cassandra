# Bug 0276: Scrubber Infinite Loop on Broken Last Partition with Compression

**Commit**: a78db628b0 · **Ticket**: CASSANDRA-17862
**Subsystem**: db/compaction/Scrubber
**Category**: Logic error in condition

---

## Root Cause

`seekToNextRow()` iterates through index positions looking for a valid seek target in the data
file, but when the last partition in a compressed data file is broken there are no further index
positions available; the method exhausted the position list and returned `void`.  The callers
ignored this outcome and looped unconditionally to the next scrub iteration, which immediately
tried to read from the same broken position again.  Because the position never advanced past the
end of file, the outer `while` loop ran forever.

### Before / After

```java
// BEFORE – returns void; callers never know the end was reached
private void seekToNextRow()
{
    while(nextRowPositionFromIndex < dataFile.length())
    {
        try { dataFile.seek(nextRowPositionFromIndex); return; }
        catch (Throwable th) { ... }
        updateIndexKey();
    }
    // falls off the end silently
}
```

```java
// AFTER – returns false when no further position exists; callers break
private boolean seekToNextRow()
{
    while(nextRowPositionFromIndex < dataFile.length())
    {
        try { dataFile.seek(nextRowPositionFromIndex); return true; }
        catch (Throwable th) { ... }
        updateIndexKey();
    }
    return false;
}

// caller:
if (!seekToNextRow())
    break;
```

---

## How to Spot

When a navigation helper that advances a cursor can legitimately reach "no more data" but returns
`void`, callers cannot distinguish "advanced successfully" from "nothing left to advance to".
During review, any loop that calls such a method should be asked: what happens if the method
silently fails to move the cursor?  Returning a boolean or throwing a sentinel exception is the
standard fix; the presence of a `while` loop whose termination depends on an advancing side
effect is a reliable smell.

---

## Test

`ScrubTest#testScrubLastBrokenPartition` writes a single row, then overwrites the SSTable data
file with 10 zero bytes to simulate a fully-truncated file, and asserts that
`performScrub` completes without hanging and that zero rows are recovered.
