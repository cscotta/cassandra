# Bug 0574: RT Boundary Marker Split Uses Wrong Deletion Time in Reversed Iterators

**Commit**: d496dca672 · **Ticket**: CASSANDRA-14672
**Subsystem**: `src/java/org/apache/cassandra/db/rows/RangeTombstoneBoundaryMarker.java`
**Category**: Logic error in condition

---

## Root Cause

A `RangeTombstoneBoundaryMarker` encodes two adjacent range tombstone intervals: a closing side and an opening side. The class stores them in physical order as `endDeletion` and `startDeletion`, but the logical close/open assignment flips when the iterator is reversed. `createCorrespondingCloseMarker` and `createCorrespondingOpenMarker` — called by the purge function when splitting a boundary into two separate bound markers — were using the raw `endDeletion`/`startDeletion` fields directly instead of the orientation-aware `closeDeletionTime(reversed)` / `openDeletionTime(reversed)` accessors. In a reversed iterator the fields have opposite meaning, so the deletion times were swapped: the newly created close marker carried the open deletion time and vice versa, causing the purge function to incorrectly evaluate expiry and corrupt or drop live range tombstone boundaries.

### Before / After

```java
// BEFORE
public RangeTombstoneBoundMarker createCorrespondingCloseMarker(boolean reversed)
{
    return new RangeTombstoneBoundMarker(closeBound(reversed), endDeletion);
}

public RangeTombstoneBoundMarker createCorrespondingOpenMarker(boolean reversed)
{
    return new RangeTombstoneBoundMarker(openBound(reversed), startDeletion);
}
```

```java
// AFTER
public RangeTombstoneBoundMarker createCorrespondingCloseMarker(boolean reversed)
{
    return new RangeTombstoneBoundMarker(closeBound(reversed), closeDeletionTime(reversed));
}

public RangeTombstoneBoundMarker createCorrespondingOpenMarker(boolean reversed)
{
    return new RangeTombstoneBoundMarker(openBound(reversed), openDeletionTime(reversed));
}
```

---

## How to Spot

In Cassandra's row-iterator model, physical field layout can differ from logical meaning depending on the scan direction. Whenever a class has both raw fields (e.g., `endDeletion`, `startDeletion`) and direction-aware accessors (e.g., `closeDeletionTime(reversed)`), any method that accepts a `reversed` flag must use the accessors — never the raw fields — so the physical-to-logical mapping stays consistent. A code review heuristic: if a method takes `boolean reversed` as a parameter, grep its body for direct field accesses that have an orientation-aware accessor equivalent.

---

## Test

The new `PurgeFunctionTest` exercises ASC and DESC partition iterators with boundary markers where one or both halves are purgeable, asserting that the surviving bounds carry the correct deletion times and clustering values after purging.
