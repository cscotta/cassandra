# Bug 0229: Nested Element/Slice Selection Fails on Reversed Collection Clustering Columns

**Commit**: 2fce3025c4 · **Ticket**: CASSANDRA-17913
**Subsystem**: cql3/selection
**Category**: Logic error in condition

---

## Root Cause

`ElementsSelector` and the selection validation code in `Selectable` assumed that a collection column's type is directly an instance of `CollectionType`. When a collection is used as a clustering column with `CLUSTERING ORDER BY ... DESC`, its type is wrapped in `ReversedType<CollectionType<?>>`. The `instanceof CollectionType` check failed, raising an assertion error, and the cast to `(CollectionType<?>)` would also fail. The fix unwraps `ReversedType` before performing the type check and cast.

### Before / After

```java
// BEFORE (ElementsSelector)
assert selected.getType() instanceof MapType || selected.getType() instanceof SetType;
this.type = (CollectionType<?>) selected.getType();
```

```java
// AFTER (ElementsSelector)
private static CollectionType<?> getCollectionType(Selector selected)
{
    AbstractType<?> type = selected.getType();
    if (type instanceof ReversedType)
        type = ((ReversedType<?>) type).baseType;
    assert type instanceof MapType || type instanceof SetType;
    return (CollectionType<?>) type;
}
// ...
this.type = getCollectionType(selected);
```

---

## How to Spot

Any code that casts or pattern-matches a column's `AbstractType` should first check for (and peel) `ReversedType` wrappers. The pattern `type instanceof XType` will silently return false when the same logical type is stored as `ReversedType<XType>`, making reversed clustering columns a systematic blind spot for type-dispatch logic.

---

## Test

Added `testMapReversed` and `testSetReversed` in `CollectionsTest`; inserts a row into a table whose frozen map/set clustering column is declared `DESC` and then exercises both element (`c['key']`) and slice (`c['k1'..'k2']`) selections.
