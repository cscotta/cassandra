# Bug 2212: Hadoop Wide-Row Iterator Over-Counts Rows in Progress Tracking (Branch Port)

**Commit**: 0bb3a064f3 · **Ticket**: CASSANDRA-4803
**Subsystem**: src/java/org/apache/cassandra/hadoop/ColumnFamilyRecordReader.java
**Category**: Off-by-one

---

## Root Cause

This commit is the same fix as bug-2211 applied to a different branch of the codebase. `ColumnFamilyRecordReader.WideRowIterator` incremented `totalRead` on every column-page fetch rather than once per distinct row key, causing `getProgress()` to report inflated row counts for wide rows. The fix deduplicates row counting with a `lastCountedKey` field and adds an early 1.0 return to `getProgress()` when the iterator is exhausted.

### Before / After

```java
// BEFORE
totalRead++;
Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> next = wideColumns.next();
```

```java
// AFTER
private ByteBuffer lastCountedKey = ByteBufferUtil.EMPTY_BYTE_BUFFER;
// ...
Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> next = wideColumns.next();
maybeCountRow(next); // increments totalRead only when key changes
```

---

## How to Spot

When progress tracking increments a counter on every page fetch rather than every unique logical key, wide rows will cause the progress counter to grow much faster than the actual number of rows processed. Ensure row-progress counters are gated on key identity, not on iteration step.

---

## Test

No test added.
