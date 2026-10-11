# Bug 1615: Fix Merge (Wrong Type in Secondary Index Operator Reversal)

**Commit**: 82be86a568 · **Ticket**: n/a
**Subsystem**: cql3/statements/SelectStatement
**Category**: Logic error in condition

---

## Root Cause

A bad merge left two errors in `SelectStatement`: (1) the local variable `op` was typed as the old `IndexOperator` enum instead of the new `IndexExpression.Operator` enum after a refactoring; and (2) the reversed-clustering-order guard checked `name.type` instead of `def.type` — `name` was no longer in scope, while `def` is the actual `ColumnDefinition` whose type must be inspected to detect `ReversedType`. Together these would cause a compile error (wrong enum type) and a logic error (wrong object consulted for the type check) for secondary-index range queries on columns with reversed clustering order.

### Before / After

```java
// BEFORE — stale type name and wrong variable for type check
IndexOperator op = slice.getIndexOperator(b);
if (name.type instanceof ReversedType)   // 'name' not in scope; wrong object
    op = reverse(op);

private static IndexOperator reverse(IndexOperator op) { ... }
```

```java
// AFTER — correct enum type and correct variable
IndexExpression.Operator op = slice.getIndexOperator(b);
if (def.type instanceof ReversedType)    // 'def' is the ColumnDefinition
    op = reverse(op);

private static IndexExpression.Operator reverse(IndexExpression.Operator op) { ... }
```

---

## How to Spot

After a merge or type-rename refactoring, search for any remaining references to the old type name and for variable references that match the old code's naming convention but differ from the new one. A compile error would normally catch the type issue, but wrong-variable references (same type, different object) require careful reading or a failing test to surface.

---

## Test

No test added in this commit. The scenario is: a SELECT with a range restriction on a secondary-index column that has `CLUSTERING ORDER BY ... DESC` should return correctly filtered results rather than returning wrong rows or throwing an exception.
