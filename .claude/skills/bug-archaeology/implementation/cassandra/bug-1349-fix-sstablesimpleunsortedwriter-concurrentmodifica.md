# Bug 1349: Fix SSTableSimpleUnsortedWriter ConcurrentModificationException (2nd)

**Commit**: 1650dd51d4 · **Ticket**: CASSANDRA-8619
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableSimpleUnsortedWriter.java
**Category**: Race condition / missing synchronization

---

## Root Cause

This is the same fix as bug-1348 applied to the 2.1 branch. The `SSTableSimpleUnsortedWriter.sync()` method handed a buffer to the background writer thread via `writeQueue.put(buffer)` without first clearing the `columnFamily` field, leaving an aliased reference that both threads could access concurrently. The background thread would iterate over the column family data while the main thread might still be writing to it, resulting in a `ConcurrentModificationException`. The 2.1 branch backport carries the identical two-part fix: null `columnFamily` before enqueueing, then re-acquire it from the fresh buffer, plus the `addColumn` guard in `CQLSSTableWriter`.

### Before / After

```java
// BEFORE
checkForWriterException();
try {
    writeQueue.put(buffer);
}
buffer = new Buffer();
currentSize = 0;
```

```java
// AFTER
checkForWriterException();
columnFamily = null;
try {
    writeQueue.put(buffer);
}
buffer = new Buffer();
currentSize = 0;
columnFamily = getColumnFamily();
```

---

## How to Spot

See bug-1348 for the general heuristic. In any multi-branch codebase, when a race is fixed in one branch it is worth verifying that equivalent branches carry the same fix — the identical root cause can produce the same crash independently on each branch.

---

## Test

No test added.
