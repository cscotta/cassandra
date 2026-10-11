# Bug 1969: Assertion Error in SSTableScanner During Repair with Wrapping Ranges

**Commit**: 59c6e500de · **Ticket**: CASSANDRA-5801
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableScanner.java
**Category**: Logic error in condition

---

## Root Cause

`SSTableScanner` supports "wrapping" token ranges that start near the end of the ring and continue from the beginning (i.e., the scan wraps around). The `isDone()` helper method contained a flawed two-phase seek logic: it checked the position against `stopAt` before seeking and re-checked afterward, but the surrounding code also had an assertion `assert currentEntry.position <= stopAt` that could fire if the scanner had not yet seeked to the start of the second segment. The assertion fired during repair because the scanner would reach the end-of-first-segment position while the seek had not yet happened, producing a false failure.

### Before / After

```java
// BEFORE - assertion could fire before seek, isDone encapsulated unreliable two-step logic
assert currentEntry.position <= stopAt;
if (isDone(currentEntry.position))
    return endOfData();
```

```java
// AFTER - assertion removed; wrapping seek handled inline with state reset
if (currentEntry.position >= stopAt)
{
    if (!hasSeeked)
    {
        seekToStart(); // This sets hasSeeked
        stopAt = dfile.length();
        // reset currentKey and nextKey since we have seeked
        currentKey = null;
        nextKey = null;
        return computeNext();
    }
    return endOfData();
}
```

---

## How to Spot

Assertions that fire inside iterators handling wrapping ranges are a red flag: a position/offset comparison that was valid for a linear scan may be violated mid-transition in a two-phase wrap. Verify that assertions cannot trigger during intermediate states of seek transitions, or remove them and handle the transition inline.

---

## Test

No test added.
