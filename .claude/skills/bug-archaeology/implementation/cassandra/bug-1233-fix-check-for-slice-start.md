# Bug 1233: Fix Check for Non-Empty Slice Start

**Commit**: f1662b1479 · **Ticket**: CASSANDRA-8502
**Subsystem**: src/java/org/apache/cassandra/db/filter/SliceQueryFilter.java
**Category**: Logic error in condition

---

## Root Cause

When advancing a paging query to a new start position, `SliceQueryFilter.withUpdatedStart` needed to detect whether the new start was the empty (sentinel) byte buffer, which signals the beginning of all columns including static ones. The original check used `!newStart.equals(ByteBufferUtil.EMPTY_BYTE_BUFFER)`, which performs a value comparison but may not correctly handle the `Composite`-typed empty sentinel used internally. The fix replaces it with `!newStart.isEmpty()`, which tests the buffer's remaining capacity and works correctly regardless of how the empty buffer was constructed.

### Before / After

```java
// BEFORE
if (!reversed && sliceIncludesStatics(slice, cfm) && !newStart.equals(ByteBufferUtil.EMPTY_BYTE_BUFFER))
    newSlices.add(new ColumnSlice(Composites.EMPTY, cfm.comparator.staticPrefix().end()));
```

```java
// AFTER
if (!reversed && sliceIncludesStatics(slice, cfm) && !newStart.isEmpty())
    newSlices.add(new ColumnSlice(Composites.EMPTY, cfm.comparator.staticPrefix().end()));
```

---

## How to Spot

Whenever code checks a `ByteBuffer` for emptiness, prefer `.isEmpty()` (i.e., `remaining() == 0`) over `.equals(ByteBufferUtil.EMPTY_BYTE_BUFFER)`. The `equals` approach silently fails when two logically-empty `ByteBuffer` instances have different backing arrays, offsets, or limit values—a common occurrence when the buffer is produced by a builder rather than the canonical constant.

---

## Test

No test added for this specific one-line follow-up fix; covered by the broader CASSANDRA-8502 test suite in the same series of commits.
