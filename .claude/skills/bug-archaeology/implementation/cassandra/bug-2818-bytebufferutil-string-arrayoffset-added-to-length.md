# Bug 2818: ByteBufferUtil.string() Adds arrayOffset Twice to Length Parameter

**Commit**: b814afa483 · **Ticket**: CASSANDRA-1999

## Detection Rule
When constructing a `String` from a `ByteBuffer`'s backing array, if `buffer.arrayOffset()` is added to both the `offset` parameter and to the `length` parameter of `new String(array, offset, length, charset)`, the string is constructed with more bytes than intended, reading garbage data from beyond the valid region.

## Code Smell
`new String(buffer.array(), buffer.arrayOffset() + offset, length + buffer.arrayOffset(), charset)` — `arrayOffset` added to length is wrong; `length` is a count, not an absolute index.

## Tags
- scope: java
- category: off-by-one
- code-pattern: array-offset-added-to-length-not-start
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`ByteBufferUtil.string(ByteBuffer buffer, int offset, int length, Charset charset)` constructed a `String` using the buffer's backing array. The third argument to `new String(byte[], int offset, int count, Charset)` is a **count** (number of bytes), not an end index. The original code added `buffer.arrayOffset()` to it, expanding the string's length by the buffer's array offset bytes, causing reads beyond the intended range and producing strings with appended garbage bytes. The callers in `DefsTable`, `HintedHandOffManager`, and `RandomPartitioner` also had their own offset/length arithmetic adjusted to correctly compute relative lengths (e.g., `index - joined.position()` instead of raw `index`).

## Example
```java
// BEFORE: arrayOffset incorrectly added to length (count), not just to start
return new String(buffer.array(), buffer.arrayOffset() + offset, length + buffer.arrayOffset(), charset);
//                                ^^^^ correct                   ^^^^ wrong: expands string length
```
```java
// AFTER: arrayOffset only added to start offset
return new String(buffer.array(), buffer.arrayOffset() + offset, length, charset);
```
