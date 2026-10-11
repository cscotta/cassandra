# Bug 1479: Spurious Assertion Failure When Comparing Empty Cell Names with heap_buffers

**Commit**: 3a410175ff · **Ticket**: CASSANDRA-7862
**Subsystem**: src/java/org/apache/cassandra/db/composites/AbstractSimpleCellNameType.java
**Category**: Missing null / bounds check

---

## Root Cause

`AbstractSimpleCellNameType.compareForCQL3SortOrder` contained an assertion `assert b1.hasRemaining() & b2.hasRemaining()` that fired when either `ByteBuffer` was empty (zero remaining bytes). When using heap buffers, it is valid for a cell name's serialized `ByteBuffer` to be empty — for example, a CQL row marker or an empty composite component. The assertion was incorrect because the underlying `ByteBufferUtil.compareUnsigned` handles empty buffers correctly. Removing the assertion allows the comparison to proceed without throwing an `AssertionError`.

### Before / After

```java
// BEFORE
ByteBuffer b1 = c1.toByteBuffer();
ByteBuffer b2 = c2.toByteBuffer();
assert b1.hasRemaining() & b2.hasRemaining();
return ByteBufferUtil.compareUnsigned(b1, b2);
```

```java
// AFTER
ByteBuffer b1 = c1.toByteBuffer();
ByteBuffer b2 = c2.toByteBuffer();
return ByteBufferUtil.compareUnsigned(b1, b2);
```

---

## How to Spot

Assertions that guard "impossible" states in comparison/sorting code should be verified against all valid inputs, especially empty or zero-length buffers. If the underlying operation is already correct for the edge case, the assertion is redundant and hazardous when `-ea` is enabled.

---

## Test

No test added.
