# Bug 1663: SerializingCache Leaks RefCountedMemory on Exception and Incorrect replace() Ordering

**Commit**: 72203c5037 · **Ticket**: CASSANDRA-6994
**Subsystem**: src/java/org/apache/cassandra/cache
**Category**: Resource leak

---

## Root Cause

`SerializingCache` manages off-heap `RefCountedMemory` objects through explicit reference counting. Three separate paths failed to release memory when an unexpected exception occurred or when the operation was logically a no-op:

1. In `serialize()`, if an `IOException` was thrown after allocating `freeableMemory`, the memory was never unreferenced before re-throwing.
2. In `put()` and `putIfAbsent()`, if `map.put()`/`map.putIfAbsent()` threw a `Throwable` (e.g., `OutOfMemoryError`), the newly serialized memory block was orphaned.
3. In `replace()`, the deserialization and equality check occurred *after* serializing the new value, meaning a mismatch would allocate memory that was then silently leaked; the fix moves the old-value check before serialization.

### Before / After

```java
// BEFORE – serialize(): no unreference on IOException path
catch (IOException e) {
    throw new RuntimeException(e);  // freeableMemory leaked
}
```

```java
// AFTER
catch (IOException e) {
    freeableMemory.unreference();
    throw new RuntimeException(e);
}
```

```java
// BEFORE – put(): map.put() exception leaks mem
RefCountedMemory old = map.put(key, mem);
```

```java
// AFTER
try {
    old = map.put(key, mem);
} catch (Throwable t) {
    mem.unreference();
    throw Throwables.propagate(t);
}
```

---

## How to Spot

Any class that allocates a reference-counted or pooled resource must follow a strict acquire-then-guard pattern: wrap every operation that could throw between acquisition and handoff in a try/catch that releases the resource on the exceptional path. Code review should trace every allocation site to its matching release, including non-obvious failure branches like `map.put()` throwing.

---

## Test

No test added.
