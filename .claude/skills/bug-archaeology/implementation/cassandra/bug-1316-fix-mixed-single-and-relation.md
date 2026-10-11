# Bug 1316: Fix Mixed Single and Multi-Column Relation Support

**Commit**: 90a012a1fe · **Ticket**: CASSANDRA-8613
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectStatement#buildBound()` checked only the first restriction to decide whether to enter a multi-column code path, then delegated entirely to separate methods (`buildMultiColumnSliceBound`, `buildMultiColumnInBound`, `buildMultiColumnEQBound`). When a query mixed single-column and multi-column restrictions on clustering columns — for example `(a, b) = (1, 2) AND c > 3` — the first restriction was a multi-column EQ, but subsequent single-column restrictions were silently ignored, producing incorrect slice bounds. The fix unifies the single- and multi-column paths into one loop that inspects each restriction independently and advances the column iterator correctly for multi-column restrictions that span multiple positions.

### Before / After

```java
// BEFORE  (checks only the first restriction; bails out to a separate path)
Restriction firstRestriction = restrictions[0];
if (firstRestriction != null && firstRestriction.isMultiColumn())
{
    if (firstRestriction.isSlice())
        return buildMultiColumnSliceBound(...);
    else if (firstRestriction.isIN())
        return buildMultiColumnInBound(...);
    else
        return buildMultiColumnEQBound(...);
}
// falls through to single-column loop — never reached when first is multi-column
```

```java
// AFTER  (unified loop handles both kinds per position)
for (int i = 0; i < names.size(); i++)
{
    CFDefinition.Name name = names.get(i);
    Restriction r = restrictions[name.position];
    if (r.isSlice())
    {
        if (r.isMultiColumn()) { /* advance i by values.size() */ }
        else { addComponent(builder, name, getSliceValue(r, b, variables)); }
        ...
    }
    ...
}
```

---

## How to Spot

Dispatch logic that branches on the type of only the *first* element in a collection and then delegates wholesale to a separate code path is fragile when the collection can contain a mix of types. Reviewers should ask: what happens when element N is type A but element N+1 is type B? Unit tests that exercise only pure-multi-column or pure-single-column queries will miss the mixed case; a combinatorial test matrix covering mixed restrictions is the appropriate check.

---

## Test

`MultiColumnRelationTest` expanded and `SelectStatementTest` added with comprehensive coverage of mixed single- and multi-column restriction combinations.
