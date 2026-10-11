# Bug 2217: Leveled Compaction Scanner Reports Stale Progress After Last SSTable

**Commit**: b8874ad1a8 · **Ticket**: CASSANDRA-4807
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledCompactionStrategy.java
**Category**: Wrong constant / default value

---

## Root Cause

`LeveledScanner` chains together scanners for multiple SSTables and tracks overall progress via `currentScanner.getCurrentPosition() + positionOffset`. When iteration exhausted the last SSTable, the code called `return endOfData()` without first nulling out `currentScanner`. Subsequent calls to `getCurrentPosition()` on the already-closed last scanner returned a stale or incorrect byte offset, causing the progress counter to report a wrong value (typically less than the total bytes scanned).

### Before / After

```java
// BEFORE
if (!sstableIterator.hasNext())
    return endOfData();
currentScanner = sstableIterator.next().getDirectScanner(range);
```

```java
// AFTER
if (!sstableIterator.hasNext())
{
    // reset to null so getCurrentPosition does not return wrong value
    currentScanner = null;
    return endOfData();
}
currentScanner = sstableIterator.next().getDirectScanner(range);
```

---

## How to Spot

When an iterator wraps a sequence of sub-iterators and computes a running position from the current sub-iterator, verify that the current sub-iterator reference is invalidated (set to null) before the outer iterator signals end-of-data, so that any subsequent position query does not read from a closed or stale source.

---

## Test

A new `testCompactionProgress` test verifies that after scanning all L1 SSTables to exhaustion, `scanner.getCurrentPosition()` equals `SSTable.getTotalBytes(sstables)`.
