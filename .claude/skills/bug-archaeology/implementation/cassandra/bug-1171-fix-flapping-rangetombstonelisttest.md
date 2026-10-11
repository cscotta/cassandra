# Bug 1171: Off-by-One in RangeTombstoneList Boundary Comparisons Causes Flapping Test

**Commit**: e338d2fa8b · **Ticket**: CASSANDRA-9799
**Subsystem**: src/java/org/apache/cassandra/db/RangeTombstoneList.java
**Category**: Off-by-one

---

## Root Cause

Two boundary comparisons in `RangeTombstoneList.addInternal` used strict less-than (`< 0`) where they should have used less-than-or-equal (`<= 0`). In Cassandra's clustering comparator, an end bound and the immediately following start bound that share the same cell value are considered equal because they represent the two sides of a common boundary between adjacent intervals — one inclusive, the other exclusive. Using `< 0` instead of `<= 0` caused the code to fall through to the wrong branch when `end` exactly equalled `starts[i+1]`, corrupting the list by duplicating or mis-ordering interval boundaries, which then triggered assertion failures non-deterministically depending on the random intervals generated in the stress test.

### Before / After

```java
// BEFORE: strict comparison misses the shared-boundary case
if (comparator.compare(end, starts[i+1]) < 0)
{
    setInternal(i, start, end, markedAt, delTime);
    return;
}
// ...
if (comparator.compare(end, starts[i]) < 0)
{
    addInternal(i, start, end, markedAt, delTime);
    return;
}
```

```java
// AFTER: inclusive comparison handles the boundary-equality case
if (i == size-1 || comparator.compare(end, starts[i+1]) <= 0)
{
    setInternal(i, start, end, markedAt, delTime);
    return;
}
// ...
if (comparator.compare(end, starts[i]) <= 0)
{
    addInternal(i, start, end, markedAt, delTime);
    return;
}
```

---

## How to Spot

Range-interval data structures are especially susceptible to off-by-one errors at boundaries. When the domain has inclusive and exclusive endpoints — as Cassandra's `Slice.Bound` does — every `< 0` comparison in an insertion or merge loop deserves scrutiny: ask whether the equality case represents two distinct positions or a shared boundary that should be handled by the "fits here" branch rather than the "continue searching" branch. A randomised stress test that asserts structural invariants after each insertion is the most reliable way to expose these failures.

---

## Test

A regression test `addAllBugFrom9799` is added, reproducing the exact sequence of intervals that triggered the corruption. A `fromString`/`assertValid` helper infrastructure is also introduced to make future range-tombstone list tests easier to write and diagnose.
