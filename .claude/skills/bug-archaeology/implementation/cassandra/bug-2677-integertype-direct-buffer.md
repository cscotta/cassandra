# Bug 2677: IntegerType.getString throws on direct ByteBuffers

**Commit**: ed7f7e8f57 · **Ticket**: CASSANDRA-2684

## Detection Rule
When reading bytes from a `ByteBuffer` that may be a direct (off-heap) buffer, never call `ByteBuffer.array()` directly; use a utility that handles both heap and direct buffers, such as `ByteBufferUtil.getArray()`.

## Code Smell
`TBaseHelper.byteBufferToByteArray(bytes)` (which calls `bytes.array()`) used inside `getString()` on a `ByteBuffer` that can be a direct buffer, throwing `UnsupportedOperationException` at runtime.

## Tags
- scope: java
- category: wrong-serialization
- code-pattern: direct-buffer-array-access
- severity: crash
- subsystem: cql-query

## Root Cause
`IntegerType.getString()` converted a `ByteBuffer` to a `byte[]` via `TBaseHelper.byteBufferToByteArray()`, which internally calls `ByteBuffer.array()`. Direct (off-heap) `ByteBuffer` instances do not have a backing array and throw `UnsupportedOperationException`. The fix replaces the call with `ByteBufferUtil.getArray()`, which correctly copies from both heap and direct buffers.

## Example
```java
// BEFORE
return new java.math.BigInteger(TBaseHelper.byteBufferToByteArray(bytes)).toString(10);
```
```java
// AFTER
return new java.math.BigInteger(ByteBufferUtil.getArray(bytes)).toString(10);
```
