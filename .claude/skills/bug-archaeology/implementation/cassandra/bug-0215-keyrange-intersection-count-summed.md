# Bug 0215: KeyRangeIntersectionIterator Sums Counts Instead of Taking the Minimum

**Commit**: b57181032d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/index/sai/iterators/KeyRangeIntersectionIterator.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`IntersectionStatistics.update()` accumulated the estimated count for an
intersection iterator by adding each child iterator's count to a running total
(`count += range.getCount()`).  For an intersection the correct upper-bound
estimate is the *minimum* of the child counts, because the intersection can never
produce more results than the smallest contributor.  Summing inflated the count
estimate — in the test case three iterators each with 3 elements produced a count
of 9 instead of 3 — which could mislead query planning code that uses the count
to decide whether to skip, reorder, or short-circuit iterators.

### Before / After

```java
// BEFORE – sums counts, producing an inflated estimate
count += range.getCount();
```

```java
// AFTER – uses min of all child counts, and handles first update specially
if (empty)
{
    empty = false;
    count = range.getCount();
}
else
{
    count = Math.min(count, range.getCount());
}
```

---

## How to Spot

When reviewing statistics or cost-estimation helpers for composite iterators,
verify that the aggregation operation matches the iterator semantics: unions
logically sum counts, intersections take the minimum, and differences subtract.
A `count +=` pattern inside an intersection builder is a strong signal that the
wrong aggregation is being used.

---

## Test

`KeyRangeIntersectionIteratorTest` updated — asserts `getCount()` returns 3
(the correct minimum) rather than 9 (the former incorrect sum) for a three-way
intersection.
