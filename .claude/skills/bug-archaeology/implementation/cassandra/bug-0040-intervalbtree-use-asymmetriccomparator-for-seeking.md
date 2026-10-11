# Bug 0040: IntervalBTree Incorrect Seeking with Symmetric Comparator

**Commit**: 7e32d92d06 · **Ticket**: CASSANDRA-20756
**Subsystem**: utils/btree/IntervalBTree
**Category**: Logic error in condition

---

## Root Cause

`IntervalBTree.accumulate` used `Arrays.binarySearch` with a symmetric `Comparator<V>` for seeking into the B-tree, but seeking a `Find` key against `Interval` nodes is an inherently asymmetric operation: the "equal" case must be resolved differently depending on whether the inclusive/exclusive nature of the interval bound applies. Using a symmetric comparator meant the equality-disambiguation logic (`equalsMeansAfter`/`equalsMeansBefore`) was applied to both sides of the comparison, which is only correct when both arguments have the same type and role. The fix replaces all seek comparators with `AsymmetricComparator<K, I>` (from accord-utils) and switches from `Arrays.binarySearch` to `SortedArrays.binarySearch` with explicit `CEIL`/`FLOOR` search modes, making the directionality explicit and removing the symmetry assumption.

### Before / After

```java
// BEFORE — symmetric comparator used for asymmetric seek
int startKey = Arrays.binarySearch(btree, 0, getKeyEnd(btree), intersects,
    (Comparator) comparators.startWithStartComparator());
int endKey = Arrays.binarySearch(btree, startKey, getKeyEnd(btree), intersects,
    (Comparator) comparators.startWithEndComparator());
```

```java
// AFTER — asymmetric comparator with explicit search direction
int startKey = SortedArrays.binarySearch(btree, 0, getKeyEnd(btree), find,
    (AsymmetricComparator) comparators.startWithStartSeeker(), CEIL);
int endKey = SortedArrays.binarySearch(btree, startKey, getKeyEnd(btree), find,
    (AsymmetricComparator) comparators.endWithStartSeeker(), FLOOR);
```

---

## How to Spot

When a comparator is used for binary search that compares two logically different types (a search key vs. an interval), treating the result as symmetric hides divergent semantics at the equality boundary. Review any `Arrays.binarySearch` call where the comparator casts or switches on the runtime type of its arguments — that is a sign the comparator is asymmetric in intent but forced into a symmetric API, and equality may be resolved in the wrong direction.

---

## Test

`IntervalBTreeTest` exercises accumulate over overlapping intervals to verify that no intersecting intervals are missed and no non-intersecting ones are included.
