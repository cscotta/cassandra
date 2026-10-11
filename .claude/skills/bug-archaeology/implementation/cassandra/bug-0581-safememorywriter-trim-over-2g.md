# Bug 0581: SafeMemoryWriter trim() Uses Wrong Capacity and Overflows Over 2 GiB

**Commit**: 49adbe7e0f · **Ticket**: CASSANDRA-14649
**Subsystem**: src/java/org/apache/cassandra/io/util/SafeMemoryWriter.java
**Category**: Off-by-one

---

## Root Cause

`SafeMemoryWriter.setCapacity(long)` called `reallocate(newCapacity)`, but `reallocate` treated its argument as a *count of additional bytes needed*, not an absolute target capacity — it added that value to the current length and then applied the growth formula. Passing the desired final size as a count caused the buffer to be grown to the wrong (larger) size instead of being trimmed to the exact written length. Additionally, `validateReallocation` did not cap the new size, allowing a single resize step to request more than `Integer.MAX_VALUE` bytes for the backing `ByteBuffer`, which would throw at the `buffer.position(...)` call. The fix separates trim logic into `resizeTo(length())` and clamps `validateReallocation` to `length() + Integer.MAX_VALUE`.

### Before / After

```java
// BEFORE
public void setCapacity(long newCapacity) {
    reallocate(newCapacity);  // treated as count, not absolute target
}

@Override
public long validateReallocation(long newSize) {
    return newSize;            // no cap; can exceed Integer.MAX_VALUE
}
```

```java
// AFTER
public void trim() {
    resizeTo(length());         // explicit absolute-target resize
}

@Override
public long validateReallocation(long newSize) {
    return Math.min(newSize, length() + Integer.MAX_VALUE);
}
```

---

## How to Spot

When an overridden method name says "reallocate by count" but callers need "resize to capacity," the dual semantics create a subtle misuse. Look for any call site that passes an absolute size to a method designed for a delta, or vice versa. Additionally, any allocation path that feeds into `ByteBuffer.position(int)` or similar APIs that take `int` must validate that the computed size fits in an `int`.

---

## Test

`SafeMemoryWriterTest.testTrim` verifies that `trim()` shrinks the buffer to the exact written length; `testOver2GBuffer` exercises writing more than 2 GiB and confirms the resize path does not overflow.
