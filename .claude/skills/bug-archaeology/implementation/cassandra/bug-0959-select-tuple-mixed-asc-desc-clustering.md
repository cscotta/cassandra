# Bug 0959: SELECT on tuple relations returns wrong rows for mixed ASC/DESC clustering

**Commit**: 90fc8969a6 · **Ticket**: CASSANDRA-7281
**Subsystem**: CQL / Query Restrictions
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
When a `WHERE` clause contained a multi-column (tuple) slice restriction such as `WHERE (a, b) > (1, 2)` and the clustering columns had mixed sort orders (e.g., `a ASC, b DESC`), `MultiColumnRestriction.Slice.appendBoundTo()` computed composite component bounds without accounting for direction changes. It simply iterated the bound components in declaration order, applied the same `Bound` side (START or END) to every column, and appended them all in one pass. This produced an incorrect composite key range that could include too many or too few rows.

The fix rewrites `appendBoundTo()` to detect order reversals: when the current column's direction differs from the previous one, it saves the current partial composite, starts a new one from scratch, and ultimately calls `builder.addAllElementsToAll(toAdd)` to produce the multiple boundary slices needed to correctly represent the range in a mixed-order index. A helper `reverseBoundIfNeeded()` was added to `AbstractRestriction` to swap `START`/`END` when a column's type is reversed.

### Before / After
```java
// BEFORE — MultiColumnRestriction.Slice.appendBoundTo
List<ByteBuffer> vals = componentBounds(bound, options);
for (int i = 0, m = vals.size(); i < m; i++)
{
    ByteBuffer v = checkNotNull(vals.get(i), ...);
    builder.addElementToAll(v);
}
return builder;

// AFTER
boolean reversed = getFirstColumn().isReversedType();
EnumMap<Bound, List<ByteBuffer>> componentBounds = new EnumMap<>(Bound.class);
componentBounds.put(Bound.START, componentBounds(Bound.START, options));
componentBounds.put(Bound.END,   componentBounds(Bound.END,   options));

List<List<ByteBuffer>> toAdd = new ArrayList<>();
List<ByteBuffer> values = new ArrayList<>();

for (int i = 0, m = columnDefs.size(); i < m; i++)
{
    ColumnDefinition column = columnDefs.get(i);
    Bound b = reverseBoundIfNeeded(column, bound);

    if (reversed != column.isReversedType())
    {
        reversed = column.isReversedType();
        toAdd.add(values);
        // rebuild values from scratch for the new direction
        ...
        values = new ArrayList<>();
    }
    if (!hasComponent(b, i, componentBounds)) continue;
    values.add(componentBounds.get(b).get(i));
}
toAdd.add(values);
if (bound.isEnd()) Collections.reverse(toAdd);
return builder.addAllElementsToAll(toAdd);
```

---

## How to Spot
Tuple slice restrictions (`WHERE (col1, col2, ...) > (v1, v2, ...)`) must generate composite row bounds that respect each column's individual sort order. If the bound-building code uses a single `Bound` direction for all components without reversing it on direction changes, it will silently produce a range that is correct only when all columns share the same order. In code review, look for loops over `columnDefs` inside `appendBoundTo()` that do not call any reversal helper — that is a sign the mixed-order case is unhandled. The presence of `reverseBoundIfNeeded()` or equivalent per-column direction logic is the expected invariant.

---

## Test
`PrimaryKeyRestrictionSetTest` was substantially expanded (1164 lines of new test code) covering multi-column restrictions with all ASC, all DESC, and mixed ASC/DESC clustering orders. `SelectMultiColumnRelationTest` added 859 lines of end-to-end query correctness checks for the same scenarios.
