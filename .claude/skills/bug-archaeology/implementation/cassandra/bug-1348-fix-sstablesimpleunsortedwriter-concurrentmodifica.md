# Bug 1348: Fix SSTableSimpleUnsortedWriter ConcurrentModificationException

**Commit**: 31324eefbd · **Ticket**: CASSANDRA-8619
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableSimpleUnsortedWriter.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`SSTableSimpleUnsortedWriter` used a producer/consumer model where the main thread appended columns to a `buffer` and a background writer thread drained completed buffers from a `writeQueue`. When the main thread called `sync()` to flush a row, it placed the current buffer onto `writeQueue` and immediately allocated a new buffer — but the field `columnFamily` (which pointed into the buffer just handed off) was not nulled out before the handoff. The background thread could therefore still be iterating over the `ColumnFamily` at the same time the main thread continued to write into `columnFamily`, producing a `ConcurrentModificationException`. The fix nulls `columnFamily` before the handoff and re-acquires a fresh one from the new buffer only after the handoff completes.

Additionally, `CQLSSTableWriter`'s inner `AbstractSSTableSimpleWriter` subclass needed an `addColumn` override that throws `UnsupportedOperationException` to prevent accidental direct-column writes from bypassing the CQL layer.

### Before / After

```java
// BEFORE
checkForWriterException();
try {
    writeQueue.put(buffer);
}
buffer = new Buffer();
currentSize = 0;
// columnFamily still points into handed-off buffer
```

```java
// AFTER
checkForWriterException();
columnFamily = null;            // sever reference before handoff
try {
    writeQueue.put(buffer);
}
buffer = new Buffer();
currentSize = 0;
columnFamily = getColumnFamily(); // fresh reference into new buffer
```

---

## How to Spot

In producer/consumer designs with shared mutable objects, look for any field that aliases into a "handed-off" object. After the object crosses the queue boundary it belongs to the consumer; any continued access by the producer is a data race. The fix pattern is always the same: null out (or re-point) the aliasing field before the handoff, and re-establish it from the new object after allocation.

---

## Test

No test added.
