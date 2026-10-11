# Bug 1787: Fix SSTableScanner Potentially Skipping Rows During Cleanup

**Commit**: 81fcc33d9d · **Ticket**: CASSANDRA-6638
**Subsystem**: src/java/org/apache/cassandra/io/sstable
**Category**: Off-by-one

---

## Root Cause

`SSTableScanner.seekToCurrentRangeStart` returned early without seeking the underlying index and data file readers when the range start key mapped to index position `-1` (meaning the key falls before everything in the SSTable). The early return assumed the scanner was already positioned at the start of the file, but when a scanner was constructed to cover multiple ranges the cursor was left at whatever position the previous range had advanced it to. The subsequent range then began scanning from mid-file, silently skipping any rows it should have covered.

### Before / After

```java
// BEFORE
if (indexPosition == -1)
    return;
```

```java
// AFTER
if (indexPosition == -1)
{
    ifile.seek(0);
    dfile.seek(0);
    return;
}
```

---

## How to Spot

Any "start from the beginning" fast-path in a scanner that skips an explicit seek to position 0 is suspect. When iterating over multiple ranges, the file cursor is never guaranteed to be at position 0 for the second range onward.

---

## Test

`SSTableScannerTest.testSingleKeyMultipleRanges` — inserts one row, creates a scanner over two ranges where the first range precedes the row and the second range contains it, then asserts the row is returned.
