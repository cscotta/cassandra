# Bug 2779: ByteBufferUtil.string Uses Buffer-Absolute Offset Instead of Absolute Position

**Commit**: fc4a261787 · **Ticket**: CASSANDRA-2134

## Detection Rule
When slicing a `ByteBuffer` by absolute position and length, call `copy.position(position)` with the caller-supplied absolute position; do not add `buffer.position()` to it, as that double-applies the relative offset.

## Code Smell
`copy.position(buffer.position() + offset)` adds the buffer's current read position to an `offset` parameter that was intended to be the absolute position within the buffer's backing array, yielding the wrong slice.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: double-applied-position-offset
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
`ByteBufferUtil.string(ByteBuffer, int, int)` documented its second parameter as `offset` but the implementation added `buffer.position()` to it before setting the duplicate's position. If `buffer.position() > 0`, the duplicate started reading too far into the buffer, producing a garbled or truncated string. The fix renames the parameter to `position` and uses it directly: `copy.position(position)`.

## Example
```java
// BEFORE
public static String string(ByteBuffer buffer, int offset, int length, Charset charset) {
    ByteBuffer copy = buffer.duplicate();
    copy.position(buffer.position() + offset); // double-offset bug
    copy.limit(copy.position() + length);
    return string(buffer, charset); // note: reads from original 'buffer' not 'copy'
}
```
```java
// AFTER
public static String string(ByteBuffer buffer, int position, int length, Charset charset) {
    ByteBuffer copy = buffer.duplicate();
    copy.position(position); // absolute position
    copy.limit(copy.position() + length);
    return string(copy, charset);
}
```
