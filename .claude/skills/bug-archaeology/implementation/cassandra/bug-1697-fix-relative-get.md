# Bug 1697: Fix BB relative get

**Commit**: 180bb75716 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/marshal/BooleanType.java
**Category**: Logic error in condition

---

## Root Cause

`BooleanType.compare()` called `ByteBuffer.get()` with no argument on two input buffers, which performs a **relative get** — it reads from the buffer's current position and then advances that position. Because `ByteBuffer` objects in Cassandra are often shared and reused, the relative read would consume the position, causing subsequent reads from the same buffer to return incorrect bytes or throw `BufferUnderflowException`. The fix uses `get(position())` — an **absolute get** — which reads from the current position without modifying it.

### Before / After

```java
// BEFORE
byte b1 = o1.get();
byte b2 = o2.get();
```

```java
// AFTER
byte b1 = o1.get(o1.position());
byte b2 = o2.get(o2.position());
```

---

## How to Spot

Any `ByteBuffer.get()` call (no-argument form) inside a comparator or serializer is suspicious: the buffer may be a read-only slice or a shared view whose position must not be mutated. Always use the index-based `get(index)` form when the caller does not own the buffer.

---

## Test

No test added.
