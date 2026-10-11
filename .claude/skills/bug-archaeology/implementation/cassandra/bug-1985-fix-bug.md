# Bug 1985: Range.intersects(Bounds) Returns True for Full-Ring When Bounds Is a Single Point

**Commit**: bbdbc0d1d9 · **Ticket**: CASSANDRA-5771
**Subsystem**: src/java/org/apache/cassandra/dht/Range.java
**Category**: Logic error in condition

---

## Root Cause

`Range.intersects(Bounds<T> that)` constructed a `Range` from `that.left` and `that.right` to test intersection. When `that.left == that.right` (a degenerate single-point `Bounds`), constructing `new Range<T>(that.left, that.right)` produces a range that covers the entire ring rather than a single point. As a result, any `Range` would intersect with such a degenerate `Bounds`, producing false positives. The fix checks `that.left.equals(that.right)` before constructing the helper `Range`, short-circuiting to a `contains` check instead.

### Before / After

```java
// BEFORE: degenerate Bounds wraps to full ring
public boolean intersects(Bounds<T> that)
{
    return intersects(new Range<T>(that.left, that.right)) || contains(that.left);
}
```

```java
// AFTER: guard against equal endpoints expanding to full ring
public boolean intersects(Bounds<T> that)
{
    return contains(that.left) || (!that.left.equals(that.right) && intersects(new Range<T>(that.left, that.right)));
}
```

---

## How to Spot

When converting an inclusive bound type into a `Range` for intersection testing, verify that equal left/right endpoints don't silently produce a full-ring range. In Cassandra's token ring, `new Range(x, x)` means the whole ring, not the empty or single-element set.

---

## Test

No new test added.
