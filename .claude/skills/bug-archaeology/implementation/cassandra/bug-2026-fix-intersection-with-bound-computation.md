# Bug 2026: Range.intersects(Bounds) Checks Wrong Endpoint for Single-Point Containment

**Commit**: 2e2dd48ff9 · **Ticket**: CASSANDRA-5551
**Subsystem**: src/java/org/apache/cassandra/dht/Range.java
**Category**: Logic error in condition

---

## Root Cause

`Range.intersects(Bounds<T>)` was designed to handle the case where converting the `Bounds` to a `Range` loses the inclusive left endpoint. After testing `intersects(new Range<T>(that.left, that.right))` it fell back to a containment check — but it was checking `contains(that.right)` (the right endpoint) instead of `contains(that.left)` (the left endpoint). A `Bounds` whose left endpoint falls inside the `Range` but whose right endpoint does not would incorrectly return `false`.

### Before / After

```java
// BEFORE
return intersects(new Range<T>(that.left, that.right)) || contains(that.right);
```

```java
// AFTER
return intersects(new Range<T>(that.left, that.right)) || contains(that.left);
```

---

## How to Spot

When a method converts between boundary types (Bounds vs Range) and then adds a special-case containment check, verify that the fallback check targets the endpoint that could be lost in the conversion. Here, `Bounds` is left-inclusive while `Range` is left-exclusive, so the left endpoint is the one that needs an explicit `contains` guard.

---

## Test

No test added.
