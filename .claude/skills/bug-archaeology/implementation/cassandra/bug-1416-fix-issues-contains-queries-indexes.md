# Bug 1416: Fix CONTAINS KEY Query Routing on Secondary Indexes

**Commit**: 52a701f295 · **Ticket**: CASSANDRA-8147
**Subsystem**: db/index/SecondaryIndex, cql3/Relation, db/index/composites
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The `allowsIndexQuery()` method on `Relation.Type` was a flat boolean check on the operator alone — it returned `true` for both `CONTAINS` and `CONTAINS_KEY` regardless of what kind of index backed the column. When a column had a keys-index (indexing map keys), a `CONTAINS` query (which targets map values) would still be routed through that index, producing wrong results or no results. Conversely, a `CONTAINS_KEY` query against a values-index would be incorrectly accepted. The fix replaces the operator-only check with a per-index-type `supportsOperator()` override, so `CompositesIndexOnCollectionKey` accepts only `CONTAINS_KEY` (plus `CONTAINS` on set columns), and `CompositesIndexOnCollectionValue` accepts only `CONTAINS`.

### Before / After

```java
// BEFORE — operator check ignores which index type backs the column
public boolean allowsIndexQuery() {
    switch (this) {
        case EQ:
        case CONTAINS:
        case CONTAINS_KEY:
            return true;
        default:
            return false;
    }
}
// called as: def.isIndexed() && relation.operator().allowsIndexQuery()
```

```java
// AFTER — each index subclass declares which operators it handles
// CompositesIndexOnCollectionKey:
public boolean supportsOperator(IndexExpression.Operator operator) {
    return operator == IndexExpression.Operator.CONTAINS_KEY ||
            operator == IndexExpression.Operator.CONTAINS && columnDef.type instanceof SetType;
}
// CompositesIndexOnCollectionValue:
public boolean supportsOperator(IndexExpression.Operator operator) {
    return operator == IndexExpression.Operator.CONTAINS;
}
// Base SecondaryIndex default: operator == EQ only
```

---

## How to Spot

When an operator is dispatched through a polymorphic handler (index, searcher, filter), verify that the dispatch checks both the operator type AND the capability of the specific handler instance — not just the operator alone. A static enum method that approves an operator without consulting the concrete handler is a red flag: it cannot know what the handler actually supports, so it will over-approve operators and silently route queries to the wrong index path.

---

## Test

`ContainsRelationTest.testContainsKeyAndContainsWithIndexOnMapKey` and `testContainsKeyAndContainsWithIndexOnMapValue` verify that `CONTAINS` is rejected when only a keys-index exists (and vice versa), and that mixed `CONTAINS` + `CONTAINS KEY` with `ALLOW FILTERING` returns the correct row set.
