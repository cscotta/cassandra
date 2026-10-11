# Bug 0565: Full Ring Range Subtraction Returns Wrong Result

**Commit**: c6f822c2a0 · **Ticket**: CASSANDRA-14869
**Subsystem**: src/java/org/apache/cassandra/dht
**Category**: Logic error in condition

---

## Root Cause

`Range.subtractContained` assumed its receiver was never a full-ring range (one where `left.equals(right)`). For a full-ring range, the existing code checked `!left.equals(contained.left)` and `!right.equals(contained.right)` to produce the two residual pieces, but both conditions evaluate to `false` when the contained range has any endpoint equal to the full-ring endpoint, yielding an empty result instead of the correct complement arc. Subtracting a non-full-ring range from a full-ring range should produce a single arc from `contained.right` to `contained.left`, but the original code returned an empty list in that case.

### Before / After

```java
// BEFORE - no special handling for full-ring receiver
private ArrayList<Range<T>> subtractContained(Range<T> contained) {
    ArrayList<Range<T>> difference = new ArrayList<>(2);
    if (!left.equals(contained.left))
        difference.add(new Range<>(left, contained.left));
    if (!right.equals(contained.right))
        difference.add(new Range<>(contained.right, right));
    return difference;
}
```

```java
// AFTER
private List<Range<T>> subtractContained(Range<T> contained) {
    if (isFull(left, right) && isFull(contained.left, contained.right))
        return Collections.emptyList();
    if (isFull(left, right))
        return Collections.singletonList(new Range<>(contained.right, contained.left));
    // ... original logic
}
```

---

## How to Spot

When a method claims to handle all cases of a ring-topology data structure, check whether the "full ring" representation (where the token wraps all the way around, typically encoded as `left == right`) is tested explicitly. A missing branch for the full-ring case is a classic off-by-one or logic omission in token-range arithmetic. Subtracting any range from the full ring must yield a single complement arc, not empty, and subtracting the full ring from itself must yield the empty set.

---

## Test

`testSubtractAllFromFullRingRange` and `testSubtractFromFullRingRange` assert that subtracting a sub-range from a full-ring range returns the correct complement and that subtracting the full ring from itself returns the empty set.
