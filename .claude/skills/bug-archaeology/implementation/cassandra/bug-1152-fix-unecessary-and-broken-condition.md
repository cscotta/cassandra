# Bug 1152: Reversed Paging Skips Correct Clusterings Due to Wrong Guard Condition

**Commit**: 028df729b7 · **Ticket**: CASSANDRA-9775
**Subsystem**: src/java/org/apache/cassandra/db/filter/ClusteringIndexNamesFilter.java
**Category**: Logic error in condition

---

## Root Cause

`ClusteringIndexNamesFilter.forPaging` contained a guard that compared `lastReturned` against `clusteringsInQueryOrder.first()` to decide whether to return the unmodified filter early. In a forward scan this check was redundant; in a reversed scan it was actively incorrect because the ordering of `clusteringsInQueryOrder` is reversed relative to the natural comparator, so the comparison's sign no longer matched the intended semantics. The result was that reversed paged queries would incorrectly return the original (full) filter instead of the properly trimmed tail/head set, causing already-returned clusterings to be re-emitted on subsequent pages.

### Before / After

```java
// BEFORE
int cmp = comparator.compare(lastReturned, clusteringsInQueryOrder.first());
if (cmp < 0 || (inclusive && cmp == 0))
    return this;

NavigableSet<Clustering> newClusterings = reversed ?
    clusterings.headSet(lastReturned, inclusive) :
    clusterings.tailSet(lastReturned, inclusive);
```

```java
// AFTER
// Guard removed entirely; always compute the trimmed set.
NavigableSet<Clustering> newClusterings = reversed ?
    clusterings.headSet(lastReturned, inclusive) :
    clusterings.tailSet(lastReturned, inclusive);
```

---

## How to Spot

When a method has an early-return optimisation that compares a position against a sorted collection, verify that the comparison direction matches the iteration order for every calling mode (forward and reversed). If the collection is a `NavigableSet` whose logical ordering depends on a runtime flag (`reversed`), a single comparator-based guard cannot correctly cover both orderings. A reviewer should check that early-return guards are either order-agnostic or gated on the same `reversed` flag that governs the main logic below.

---

## Test

No test added (fix is part of the broader CASSANDRA-9775 paging correctness work).
