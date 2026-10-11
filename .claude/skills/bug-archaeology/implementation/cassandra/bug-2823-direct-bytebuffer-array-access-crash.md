# Bug 2823: Direct ByteBuffer Array Access Assumes Heap Buffer, Fails for Direct Buffers

**Commit**: 17266e5fd2 · **Ticket**: CASSANDRA-1989

## Detection Rule
When code calls `byteBuffer.array()` or accesses `byteBuffer.array()[position + arrayOffset + i]` without first calling `byteBuffer.hasArray()`, it throws `UnsupportedOperationException` for direct (off-heap) `ByteBuffer` instances introduced by zero-copy I/O paths.

## Code Smell
`bytes.array()[bytes.position() + bytes.arrayOffset() + i]` in `FBUtilities.byteBufferToInt()` and `Arrays.copyOfRange(pc.array(), pc.position() + pc.arrayOffset(), pc.limit())` in `ColumnSerializer` — both assume the buffer has a backing array, crashing on direct buffers.

## Tags
- scope: java
- category: missing-null-check
- code-pattern: array-access-on-direct-bytebuffer
- severity: crash
- subsystem: storage-engine

## Root Cause
After CASSANDRA-1714 introduced zero-copy reads using direct `ByteBuffer` instances (which are allocated off-heap and have no backing array), several utility methods that assumed `ByteBuffer.hasArray() == true` began throwing `UnsupportedOperationException`. `FBUtilities.byteBufferToInt()` used `bytes.array()[bytes.position() + bytes.arrayOffset() + i]` which fails for direct buffers. The fix uses `bytes.get(bytes.position() + i)` which is position-relative and works for both heap and direct buffers. `ColumnSerializer` used `Arrays.copyOfRange(pc.array(), ...)` which is replaced with `ByteBufferUtil.getArray(pc)` that handles both buffer types. `CounterColumn.updateDigest()` also had a copy-paste bug: it used `name.arrayOffset()` for `value`'s offset; the fix uses `digest.update(buffer.duplicate())` which is cleaner and correct for both buffer types.

## Example
```java
// BEFORE: crashes on direct ByteBuffer (no backing array)
n |= bytes.array()[bytes.position() + bytes.arrayOffset() + i] & 0xFF;
// ColumnSerializer:
byte[] partitionedCounter = Arrays.copyOfRange(pc.array(), pc.position() + pc.arrayOffset(), pc.limit());
// CounterColumn (copy-paste bug: name.arrayOffset() used for value):
digest.update(value.array(), value.position() + name.arrayOffset(), value.remaining());
```
```java
// AFTER: works for both heap and direct buffers
n |= bytes.get(bytes.position() + i) & 0xFF;
byte[] partitionedCounter = ByteBufferUtil.getArray(pc);
digest.update(name.duplicate());
digest.update(value.duplicate());
```
