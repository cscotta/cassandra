# Bug 1061: IN Clause on Partition Key Produces Wrong ORDER BY Sort

**Commit**: f587397c9c · **Ticket**: CASSANDRA-10363
**Subsystem**: CQL / Query Execution (Selection / SelectStatement)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
When a `SELECT` with `ORDER BY` required a column to be added for sorting purposes, `SelectStatement` used a local `indexOf()` helper that searched the raw `Selection.getColumns()` list and returned a column-list index. For a `SelectionWithProcessing` (used when aggregation or non-trivial projections are present), this raw index does not correspond to a result-set position — the actual result-set position is determined by the `selectors` list, which may reorder or omit columns. The wrong index was then stored in `orderingIndexes`, causing rows to be sorted by the wrong value. The fix introduces `Selection.getResultSetIndex()`, which `SelectionWithProcessing` overrides to search the `selectors` list for the matching `SimpleSelector` and return the position within the selector list, not the column list. Additionally, `addColumnForOrdering` was corrected to return `selectors.size() - 1` (the newly appended selector's position) instead of the raw column index.

### Before / After
```java
// BEFORE — returns column-list index, not result-set index
int index = indexOf(def, stmt.selection);

// AFTER — returns result-set index via virtual dispatch
int index = stmt.selection.getResultSetIndex(def);

// SelectionWithProcessing override:
public int getResultSetIndex(ColumnDefinition c) {
    int index = getColumnIndex(c);
    if (index < 0) return -1;
    for (int i = 0; i < selectors.size(); i++) {
        Selector s = selectors.get(i);
        if (s instanceof SimpleSelector && ((SimpleSelector) s).idx == index)
            return i;
    }
    return -1;
}

// addColumnForOrdering fix:
public int addColumnForOrdering(ColumnDefinition c) {
    int index = super.addColumnForOrdering(c);
    selectors.add(new SimpleSelector(c.name.toString(), index, c.type));
    return selectors.size() - 1;  // was: return index;
}
```

---

## How to Spot
When a `Selection` subclass maintains a separate `selectors` list that wraps the underlying column list, any method that returns a "position" must distinguish between the two lists. A method named `indexOf` or `getIndex` that searches `getColumns()` directly is unsafe for use in ordering when a `SelectionWithProcessing` is active. Code review should verify that ordering index lookups go through a virtual method that each `Selection` subclass implements correctly for its own internal layout.

---

## Test
`SelectOrderByTest` was extended with queries covering `IN` on partition key combined with `ORDER BY` to assert correct result ordering.
