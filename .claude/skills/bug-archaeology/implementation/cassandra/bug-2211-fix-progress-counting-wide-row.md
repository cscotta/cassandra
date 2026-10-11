# Bug 2211: Hadoop Wide-Row Iterator Over-Counts Rows in Progress Tracking

**Commit**: 8bab6febb5 · **Ticket**: CASSANDRA-4803
**Subsystem**: src/java/org/apache/cassandra/hadoop/ColumnFamilyRecordReader.java
**Category**: Off-by-one

---

## Root Cause

`ColumnFamilyRecordReader.WideRowIterator` incremented `totalRead` unconditionally every time a column-page was fetched from a wide row. Because a single wide row may span many pages (one per `batchSize` columns), `totalRead` was inflated proportionally to the number of column pages, making `getProgress()` report a value far higher than actual row progress. The fix introduces `lastCountedKey` to deduplicate counts: `totalRead` is incremented only when the row key changes from the previously counted key.

### Before / After

```java
// BEFORE
// In WideRowIterator.computeNext():
totalRead++;
Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> next = wideColumns.next();
```

```java
// AFTER
private ByteBuffer lastCountedKey = ByteBufferUtil.EMPTY_BYTE_BUFFER;
// ...
Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> next = wideColumns.next();
maybeCountRow(next);  // only increments if key != lastCountedKey

// getProgress() also gains an early-return:
if (!iter.hasNext())
    return 1.0F;
```

---

## How to Spot

When progress tracking increments a counter on every iteration of an inner loop (column pages) rather than on every instance of the outer logical unit (rows), the counter will over-count. Look for `totalRead++` inside iterators that may visit multiple pages per logical record.

---

## Test

No test added.
