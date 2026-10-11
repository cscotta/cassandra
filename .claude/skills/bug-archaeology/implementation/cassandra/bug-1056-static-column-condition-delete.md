# Bug 1056: Static Column IF Condition Skips Clustering Requirement on Full-Row DELETE

**Commit**: 9b3b1cb03c · **Ticket**: CASSANDRA-10264
**Subsystem**: CQL / ModificationStatement
**Category**: Logic error in condition

---

## Root Cause
`ModificationStatement.createClusteringPrefix()` gated the "allow missing clustering columns" path on the condition `setsStaticColumns && !setsRegularColumns`. A `DELETE` statement with no column list (full-row delete) sets neither flag, so `setsRegularColumns` is `false` even though the delete logically targets every column including regular ones. The fix introduces `appliesToRegularColumns()` which additionally treats a `DELETE` with an empty `columnOperations` list as applying to regular columns, and `appliesOnlyToStaticColumns()` wraps both checks. Additionally, the cqlsh grammar token `<cmp>` was missing `!` so `IF col != val` would not be recognised.

### Before / After
```java
// BEFORE
if (setsStaticColumns && !setsRegularColumns)

// AFTER
if (appliesOnlyToStaticColumns())

// New helpers:
private boolean appliesOnlyToStaticColumns() {
    return setsStaticColumns && !appliesToRegularColumns();
}
private boolean appliesToRegularColumns() {
    return setsRegularColumns
        || (type == StatementType.DELETE && columnOperations.isEmpty());
}
```

---

## How to Spot
Any guard that tests `!setsRegularColumns` to decide whether clustering columns are optional is suspect when `DELETE` statements are in scope — a bare `DELETE … WHERE pk = ?` has no column operations at all, so both `setsRegularColumns` and `setsStaticColumns` can be `false` simultaneously, making the condition evaluate to an unexpected branch. Review every place that distinguishes "static-only modification" from "full-row modification" and ensure full-row deletes are explicitly accounted for.

---

## Test
`InsertUpdateIfConditionTest` was extended with cases that assert `DELETE` without clustering columns on a table that has a static column and an `IF` condition works correctly.
