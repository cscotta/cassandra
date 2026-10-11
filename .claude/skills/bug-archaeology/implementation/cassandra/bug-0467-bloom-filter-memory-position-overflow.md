# Bug 0467: Bloom Filter Memory Position Overflows at 2 GB

**Commit**: 7cdad3ce61 · **Ticket**: CASSANDRA-15869
**Subsystem**: src/java/org/apache/cassandra/io/util/MemoryOutputStream.java
**Category**: Off-by-one

---

## Root Cause

`MemoryOutputStream` tracked its write position with an `int` field, which silently overflows to a negative value once accumulated writes exceed 2 GB (2^31 bytes). Bloom filters for very large SSTables can exceed this threshold, causing `mem.setBytes()` to be called with a negative offset, corrupting the filter data. The fix widens the field to `long`.

### Before / After

```java
// BEFORE
private int position = 0;
```

```java
// AFTER
private long position = 0;
```

---

## How to Spot

Whenever a running-total accumulator is used to track a byte offset into a potentially unbounded buffer, check whether the field type can represent the maximum expected size. An `int` position field is safe only up to ~2 GB; any class that writes to off-heap `Memory` backed by a bloom filter or large SSTable component must use `long`. The removed `position()` accessor returning `int` was a secondary signal: callers that consumed the value would inherit the same overflow.

---

## Test

No test added.
