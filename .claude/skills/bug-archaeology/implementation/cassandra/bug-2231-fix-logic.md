# Bug 2231: Reversed CQL3 Range Queries Swap Bounds Incorrectly for Reversed Comparator Columns

**Commit**: 6ddcf0388b · **Ticket**: CASSANDRA-4716
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Logic error in condition

---

## Root Cause

`SelectStatement.getRequestedBound()` was called with already-swapped bounds when `ORDER BY DESC` was requested: the caller passed `Bound.END` where `Bound.START` was expected and vice versa. This worked correctly for normally-ordered columns, but when a column itself used a `ReversedType` comparator, the swap was applied twice (once by the caller, once by the comparator), accidentally un-reversing the bounds and producing wrong or empty result sets. The fix moves the bound-swapping logic inside `getRequestedBound()`, where it can account for both the query's `isReversed` flag and the column's own type reversal: bounds are swapped only when exactly one of the two conditions is true (`isReversed != isReversedType(name)`).

### Before / After

```java
// BEFORE — caller pre-swaps bounds without considering reversed-type columns
ByteBuffer start = getRequestedBound(isReversed ? Bound.END : Bound.START, variables);
ByteBuffer finish = getRequestedBound(isReversed ? Bound.START : Bound.END, variables);
```

```java
// AFTER — bounds always passed as START/END; getRequestedBound swaps internally
ByteBuffer start = getRequestedBound(Bound.START, variables);
ByteBuffer finish = getRequestedBound(Bound.END, variables);
// inside getRequestedBound:
Bound b = isReversed == isReversedType(name) ? bound : Bound.reverse(bound);
```

---

## How to Spot

When a sort direction flag interacts with a column's own storage ordering, applying the direction at a single call site is fragile — any column-level reversal will produce double-negation bugs. Push directional logic down to where per-column type information is available, and use an XOR/inequality check to combine the two boolean reversal conditions.

---

## Test

No test added.
