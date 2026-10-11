# Bug 0424: ByteBufferAccessor.getUnsignedShort Ignores Buffer Position

**Commit**: 1102687d06 · **Ticket**: CASSANDRA-16249
**Subsystem**: src/java/org/apache/cassandra/db/marshal/ByteBufferAccessor.java
**Category**: Off-by-one

---

## Root Cause

`ByteBufferAccessor.getUnsignedShort` delegated to `ByteBufferUtil.getUnsignedShort(value, offset)` passing the caller's logical offset directly as an absolute byte-buffer index. Because `ByteBuffer` can have a non-zero `position()`, the correct absolute index is `position() + offset`. All other accessor methods in the same class that call into `ByteBufferUtil` correctly add `value.position()`, but this one was added or ported without that adjustment. Reading at the wrong absolute position returns garbage or an out-of-bounds value whenever the buffer was sliced or positioned past zero.

### Before / After

```java
// BEFORE
public int getUnsignedShort(ByteBuffer value, int offset)
{
    return ByteBufferUtil.getUnsignedShort(value, offset);
}
```

```java
// AFTER
public int getUnsignedShort(ByteBuffer value, int offset)
{
    return ByteBufferUtil.getUnsignedShort(value, value.position() + offset);
}
```

---

## How to Spot

In any `ValueAccessor` implementation that wraps `ByteBuffer`, every low-level read helper that takes an explicit offset must add `buffer.position()` to the offset before passing it to a `ByteBufferUtil` method whose index parameter is absolute rather than relative. A reviewer can grep for every `ByteBufferUtil.get*` call inside `ByteBufferAccessor` and verify that each one reads `value.position() + offset` rather than bare `offset`. The absence of that addition is the defect pattern; property-based tests that left-pad buffers before exercising every accessor method surface it reliably.

---

## Test

The updated `ValueAccessorTest.testUnsignedShort` uses QuickTheories to generate random integer values, accessor instances, and random buffer padding amounts, verifying that `getUnsignedShort` round-trips correctly after `putShort` even when the backing `ByteBuffer` has a non-zero position.
