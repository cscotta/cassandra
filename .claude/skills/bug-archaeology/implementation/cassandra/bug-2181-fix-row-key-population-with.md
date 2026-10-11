# Bug 2181: Hadoop ColumnFamilyRecordReader Shares ByteBuffer Position Across Rows

**Commit**: d909fb4faf · **Ticket**: CASSANDRA-4834
**Subsystem**: src/java/org/apache/cassandra/hadoop/ColumnFamilyRecordReader.java
**Category**: State not cleaned up

---

## Root Cause

The old-style Hadoop `mapred` interface in `ColumnFamilyRecordReader` consumed `ByteBuffer` objects directly from the underlying row data without duplicating them first. Two locations were affected: `lastColumn` was set to the column name buffer of the next wide-row pair, and the row key was written into the output `key` buffer via `key.put(this.getCurrentKey())`. In both cases, advancing the `position` of the shared buffer (via `put` or by holding a reference that the reader later advanced) corrupted subsequent reads. The fix calls `.duplicate()` before using each buffer so that position state is not shared.

### Before / After

```java
// BEFORE
lastColumn = next.right.values().iterator().next().name();
// ...
key.put(this.getCurrentKey());
```

```java
// AFTER
lastColumn = next.right.values().iterator().next().name().duplicate();
// ...
key.put(this.getCurrentKey().duplicate());
```

---

## How to Spot

Whenever a `ByteBuffer` retrieved from a data structure is passed to a consumer that advances its position (e.g., `ChannelBuffer.writeBytes`, `ByteBuffer.put`, or retained as a cursor marker), call `.duplicate()` first to avoid mutating the original buffer's state.

---

## Test

No test added.
