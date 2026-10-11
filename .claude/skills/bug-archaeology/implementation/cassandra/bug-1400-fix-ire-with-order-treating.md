# Bug 1400: Fix IRE with ORDER BY, treating all selections as fns

**Commit**: 1945384fdf · **Ticket**: CASSANDRA-8286
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/Selection.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The `isUsingFunction` predicate in `Selection.fromSelectors` determined whether a query needed the "processing" code path by checking `!(rawSelector.selectable instanceof ColumnIdentifier)`. When any selector was a function or WRITETIME/TTL expression, the query was routed through `SelectionWithFunctions`, which tracked the actual resolved `CFDefinition.Name` columns. However, the ORDER BY validation then iterated over the raw `selectClause` list and compared `name.name.equals(selector.selectable)`, matching a `CFDefinition.Name` against a raw `Selectable` object — a comparison that can never be true for function-wrapped columns. This caused an `InvalidRequestException` claiming the ORDER BY column was missing from the select clause even when it was legitimately present as a function argument.

### Before / After

```java
// BEFORE
private static boolean isUsingFunction(List<RawSelector> rawSelectors)
{
    for (RawSelector rawSelector : rawSelectors)
    {
        if (!(rawSelector.selectable instanceof ColumnIdentifier))
            return true;
    }
    return false;
}

// In ORDER BY validation:
for (int i = 0; i < selectClause.size(); i++)
{
    RawSelector selector = selectClause.get(i);
    if (name.name.equals(selector.selectable))
    { ... }
}
```

```java
// AFTER
// Selectable.Raw gains a processesSelection() hook; ColumnIdentifier returns false,
// functions/WritetimeOrTTL return true.
private static boolean selectionsNeedProcessing(List<RawSelector> rawSelectors)
{
    for (RawSelector rawSelector : rawSelectors)
    {
        if (rawSelector.processesSelection())
            return true;
    }
    return false;
}

// ORDER BY validation now iterates the already-resolved column list:
List<Name> selectedColumns = stmt.selection.getColumns();
for (int i = 0; i < selectedColumns.size(); i++)
{
    Name selected = selectedColumns.get(i);
    if (name.equals(selected))
    { ... }
}
```

---

## How to Spot

Whenever ORDER BY validation compares against a different representation of the selection list than the one actually used at execution time, the check can disagree with reality. Reviewers should confirm that the column list iterated for ORDER BY lookup is the same resolved, post-preparation list that will be used during query execution — not the raw AST form. Any place that compares `Name` objects against raw `Selectable` or `ColumnIdentifier` instances without an explicit prepare step is a red flag.

---

## Test

`SelectionOrderingTest` (new) covers ORDER BY with plain columns, function-wrapped columns, columns not present in the select clause, single and multiple clustering columns, and both ASC/DESC directions.
