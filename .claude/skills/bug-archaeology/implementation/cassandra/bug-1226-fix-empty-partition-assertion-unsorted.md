# Bug 1226: Empty Partition Assertion in Unsorted SSTable Writing

**Commit**: 66a48e7ab1 · **Ticket**: CASSANDRA-9071
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableSimpleUnsortedWriter.java
**Category**: Logic error in condition

---

## Root Cause

`SSTableSimpleUnsortedWriter` buffers rows in a `TreeMap` keyed by `DecoratedKey`. When the buffer is flushed by the background `DiskWriter` thread, it skips a row if its `ColumnFamily` has no columns, but asserts that this can only happen for the very first key in the map—the rationale being that a freshly-switched buffer starts with one empty `ColumnFamily` placeholder for the current key. The emptiness check used a `boolean first` flag that was true only for the first iteration of the `entrySet()` loop. However, because the buffer is a `TreeMap` sorted by token, the first key returned by the iterator is the _smallest token_, not the key that was active when `sync()` was called. If the current key happened not to be the smallest token in the buffer, the `first` flag would be `false` for that entry and the assertion would fire incorrectly.

### Before / After

```java
// BEFORE
boolean first = true;
for (Map.Entry<DecoratedKey, ColumnFamily> entry : b.entrySet()) {
    if (entry.getValue().getColumnCount() > 0)
        writer.append(entry.getKey(), entry.getValue());
    else if (!first)
        throw new AssertionError("Empty partition");
    first = false;
}
```

```java
// AFTER
// Buffer now tracks the key that was current when sync() was called
for (Map.Entry<DecoratedKey, ColumnFamily> entry : b.entrySet()) {
    if (entry.getValue().getColumnCount() > 0)
        writer.append(entry.getKey(), entry.getValue());
    else if (!entry.getKey().equals(b.getFirstInsertedKey()))
        throw new AssertionError("Empty partition");
}
```

---

## How to Spot

Whenever a "skip the first element" sentinel is implemented with an iteration-order boolean flag against a sorted collection, ask whether iteration order and insertion order are the same. Here, the buffer is a `TreeMap` whose iteration order is by token, not by the order entries were added, so any assumption that "first in iteration == first inserted" is fragile. The safe pattern is to record the identity of the entry that should be treated specially at the time it is created, rather than relying on its position in a subsequent traversal.

---

## Test

`CQLSSTableWriterTest.testSyncNoEmptyRows` added: writes 50 000 rows with random UUIDs (which are therefore randomly distributed across tokens) and calls `close()`, verifying no spurious empty-partition assertion is thrown.
