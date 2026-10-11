# Bug 2110: Commit Log Append Serializes Mutation Twice via Byte Array Intermediate

**Commit**: baca0bc7fe · **Ticket**: CASSANDRA-5199
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLogSegment.java, src/java/org/apache/cassandra/io/util/ByteBufferOutputStream.java, src/java/org/apache/cassandra/io/util/ChecksummedOutputStream.java
**Category**: Resource leak

---

## Root Cause

The commit log append path first serialized a `RowMutation` into a temporary `byte[]` via `FBUtilities.serialize`, then copied that array into the memory-mapped commit log buffer. This meant every write allocated and populated a full heap byte array even though the target buffer was already memory-mapped. For write-heavy workloads this created significant GC pressure from short-lived byte arrays. The fix introduces `ByteBufferOutputStream` and `ChecksummedOutputStream` wrappers so the serializer writes directly into the mapped buffer while simultaneously updating the checksum, eliminating the intermediate allocation entirely.

### Before / After

```java
// BEFORE
byte[] serializedRow = FBUtilities.serialize(rowMutation, RowMutation.serializer, MessagingService.current_version);
checksum.update(serializedRow.length);
buffer.putInt(serializedRow.length);
buffer.putLong(checksum.getValue());
buffer.put(serializedRow);
checksum.update(serializedRow, 0, serializedRow.length);
buffer.putLong(checksum.getValue());
```

```java
// AFTER
int length = (int) RowMutation.serializer.serializedSize(mutation, MessagingService.current_version);
bufferStream.writeInt(length);
buffer.putLong(checksum.getValue());
// checksummed mutation written directly via bufferStream (ChecksummedOutputStream wrapping ByteBufferOutputStream)
RowMutation.serializer.serialize(mutation, bufferStream, MessagingService.current_version);
buffer.putLong(checksum.getValue());
```

---

## How to Spot

Whenever serialization goes through an intermediate `byte[]` before being written to a final destination, look for an opportunity to serialize directly to the destination stream/buffer. Intermediate byte arrays waste memory and increase GC frequency; the pattern `serialize -> byte[] -> buffer.put(bytes)` should be replaced with streaming serialization.

---

## Test

No test added.
