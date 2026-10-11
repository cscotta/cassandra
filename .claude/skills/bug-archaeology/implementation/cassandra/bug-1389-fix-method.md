# Bug 1389: Fix Accumulator.isEmpty method

**Commit**: 178d7b6f88 · **Ticket**: CASSANDRA-7873
**Subsystem**: utils/concurrent/Accumulator.java
**Category**: Logic error in condition

---

## Root Cause

`Accumulator.isEmpty()` returned `presentCount != 0` — the exact inverse of the correct predicate. The method reported `true` (empty) whenever the accumulator held one or more elements, and `false` (not empty) when it was genuinely empty. Any caller relying on `isEmpty()` to skip processing an empty accumulator would instead skip a non-empty one, and vice versa.

This is a straightforward boolean negation error introduced when writing the method; the correct check is `presentCount == 0`.

### Before / After

```java
// BEFORE
public boolean isEmpty()
{
    return presentCount != 0;  // inverted: returns true when non-empty
}
```

```java
// AFTER
public boolean isEmpty()
{
    return presentCount == 0;
}
```

---

## How to Spot

Boolean predicates whose names contain "is" or "has" are easy to invert accidentally. During code review, read each boolean method aloud: "isEmpty returns true when presentCount is not zero" is immediately nonsensical. Unit tests for the degenerate case (empty collection, single element, full collection) catch this class of inversion instantly and should be mandatory for any collection utility method.

---

## Test

`AccumulatorTest.testIsEmptyAndSize` — verifies `isEmpty()` returns `true` on a fresh accumulator and `false` after elements are added.
