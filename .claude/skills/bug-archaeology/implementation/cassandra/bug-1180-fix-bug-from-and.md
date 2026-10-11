# Bug 1180: Fix Commit Log Checksum Computed Over Wrong Buffer Slice

**Commit**: 77a6090b24 · **Ticket**: CASSANDRA-9787
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLog.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When writing a mutation to the commit log, the code computed a checksum over a `ByteBuffer` copy that was created and limited before the mutation bytes had been serialized into it. `buffer.duplicate()` captured the current position, and `copy.limit((int) size)` set the limit relative to the buffer's beginning — but the serialization had not yet occurred, so the copy contained only zeroes. The fix moves the `duplicate()` call to after serialization and limits the copy using `copy.position() + size` (relative to where the data actually starts) so the checksum covers the real mutation bytes.

### Before / After

```java
// BEFORE
Mutation.serializer.serialize(mutation, dos, MessagingService.current_version);
copy = buffer.duplicate();
copy.limit((int) size);
checksum.update(copy);
```

```java
// AFTER
copy = buffer.duplicate();
Mutation.serializer.serialize(mutation, dos, MessagingService.current_version);
copy.limit(copy.position() + (int) size);
checksum.update(copy);
```

---

## How to Spot

When a `ByteBuffer.duplicate()` is used to compute a checksum or hash over a region of bytes, the duplicate must be taken before the write (to capture the start position) and the limit must be set relative to that saved position, not as an absolute offset from zero. Any code that duplicates after writing, or limits with a bare `(int) size`, risks computing a checksum over the wrong or uninitialized range.

---

## Test

`RecoveryManagerFlushedTest` is extended to cover the scenario introduced by the original CASSANDRA-6717 and CASSANDRA-9650 changes that led to this regression.
