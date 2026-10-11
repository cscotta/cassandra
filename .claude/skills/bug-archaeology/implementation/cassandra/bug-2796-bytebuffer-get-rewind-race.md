# Bug 2796: ByteBuffer Mutated via get/rewind Instead of Read-Only Copy

**Commit**: 6aa5b81d24 · **Ticket**: CASSANDRA-2066

## Detection Rule
When code calls `ByteBuffer.get(byte[])` (which advances the buffer's position) followed by `ByteBuffer.rewind()` to restore it, concurrent readers of the same buffer see a briefly inconsistent position, causing data corruption or `ArrayIndexOutOfBoundsException`.

## Code Smell
Pattern `byte[] b = new byte[bytes.remaining()]; bytes.get(b); bytes.rewind();` appears in multiple places. The `get()` advances the position, so any concurrent thread observing the buffer between `get()` and `rewind()` sees position = limit.

## Tags
- scope: java
- category: race-condition
- code-pattern: bytebuffer-get-then-rewind
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause
`ByteBuffer` instances passed across method boundaries were being read by calling `bytes.get(byte[])`, which mutates the buffer's position as a side effect, then calling `bytes.rewind()` to "undo" the mutation. This is not thread-safe: any concurrent reader of the buffer observes the modified position mid-read. The fix replaces all such patterns with `ByteBufferUtil.getArray(bytes)`, which reads from `bytes.array()` using `arrayOffset` without touching `position`, making reads truly non-destructive. Additionally, `ByteBufferUtil.getArray(b, 0, length)` was updated to return the backing array directly (without copy) when possible, reducing allocation.

## Example
```java
// BEFORE: mutates buffer position, unsafe for concurrent readers
public Token<BigInteger> fromByteArray(ByteBuffer bytes) {
    byte[] b = new byte[bytes.remaining()];
    bytes.get(b);    // advances position
    bytes.rewind();  // restores — but window of corruption exists
    return new BigIntegerToken(new BigInteger(b));
}
```
```java
// AFTER: non-destructive read
public Token<BigInteger> fromByteArray(ByteBuffer bytes) {
    return new BigIntegerToken(new BigInteger(ByteBufferUtil.getArray(bytes)));
}
```
