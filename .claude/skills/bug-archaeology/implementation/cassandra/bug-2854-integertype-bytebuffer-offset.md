# Bug 2854: IntegerType.getString ignores ByteBuffer offset, reads wrong bytes

**Commit**: ab8da9552c · **Ticket**: CASSANDRA-1681

## Detection Rule
When constructing a `BigInteger` (or any value) from a `ByteBuffer`'s backing array, never pass `bytes.array()` alone; always extract the relevant bytes using the buffer's `position`, `arrayOffset`, and `remaining()` to respect sliced or duplicated buffers.

## Code Smell
`new java.math.BigInteger(bytes.array())` uses the entire backing array regardless of `bytes.position()` and `bytes.arrayOffset()`. If the buffer is a slice or sub-buffer, this reads extra leading or trailing bytes, producing a wrong integer string representation.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: bytebuffer-array-ignores-offset
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
`IntegerType.getString(ByteBuffer bytes)` called `bytes.array()` to get all bytes of the backing array, ignoring that the `ByteBuffer` might have a non-zero `arrayOffset()` or `position()`. The fix uses `TBaseHelper.byteBufferToByteArray(bytes)` which correctly copies only the relevant portion of the buffer.

## Example
```java
// BEFORE
return new java.math.BigInteger(bytes.array()).toString(10);

// AFTER
return new java.math.BigInteger(TBaseHelper.byteBufferToByteArray(bytes)).toString(10);
```
