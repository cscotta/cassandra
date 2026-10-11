# Bug 0916: Slice queries return wrong rows on DESC-ordered COMPACT tables

**Commit**: c8d955533b · **Ticket**: CASSANDRA-10988
**Subsystem**: CQL SELECT / COMPACT STORAGE
**Category**: Logic error in condition

---

## Root Cause
`SelectStatement.makeExclusiveSliceBound()` called `restrictions.areRequestedBoundsInclusive(bound)` passing the original logical bound (START or END). However, for non-compound (COMPACT) tables with a reversed clustering column, `clusteringColumnBounds()` internally reverses the bound direction when building the physical cell name — so START becomes END and vice versa. `areRequestedBoundsInclusive()` uses the logical bound to check the restriction's `isInclusive` flag and never performs this reversal, so the inclusive/exclusive check was applied to the wrong boundary. The result was that an exclusive slice became inclusive (or vice versa), returning rows that should have been excluded or omitting rows that should have been included.

### Before / After
```java
// BEFORE
private CellName makeExclusiveSliceBound(Bound bound, ...) {
    if (restrictions.areRequestedBoundsInclusive(bound))
        return null;
    return type.makeCellName(...);
}

// AFTER
private CellName makeExclusiveSliceBound(Bound bound, ...) {
    // reverse bound to match what clusteringColumnBounds() does for DESC columns
    if (restrictions.areRequestedBoundsInclusive(reverseBoundIfNeeded(bound)))
        return null;
    return type.makeCellName(...);
}

private Bound reverseBoundIfNeeded(Bound bound) {
    assert !cfm.comparator.isCompound();
    List<ColumnDefinition> columnDefs = cfm.clusteringColumns();
    return columnDefs.get(columnDefs.size() - 1).isReversedType()
           ? bound.reverse() : bound;
}
```

---

## How to Spot
Whenever a query layer has two parallel code paths — one that physically reverses a bound (e.g., for DESC ordering) and another that checks a flag against the logical bound — they will diverge for reversed types. In review, look for any call to `areRequestedBoundsInclusive(bound)` that does not account for the fact that the physical evaluation of the same bound has already been flipped.

---

## Test
An extensive set of new test cases was added to `SelectMultiColumnRelationTest` and `SelectTest` covering slice queries on ordered COMPACT STORAGE tables with DESC clustering columns.
