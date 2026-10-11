# Bug 0059: RowFilter#isMutableIntersection Treats Two Ranges on Same Column as Multi-Column

**Commit**: 4bcae8e57e · **Ticket**: CASSANDRA-20566
**Subsystem**: db/filter
**Category**: Logic error in condition

---

## Root Cause

`RowFilter.isMutableIntersection()` was designed to return `true` when a row filter touches more than one non-primary-key column, because that requires a mutable (post-filter) intersection pass. The implementation counted raw `Expression` objects rather than distinct columns, so two expressions on the same column (e.g. `col > 5 AND col < 10`) incremented the counter twice and incorrectly triggered a mutable intersection — forcing unnecessary extra filtering work. The fix tracks distinct `ColumnMetadata` objects via a `HashSet` instead of a simple counter.

### Before / After

```java
// BEFORE – counts expressions, not distinct columns
int count = 0;
for (Expression e : expressions) {
    if (!e.column.isPrimaryKeyColumn())
        if (++count > 1)
            return true;
}
```

```java
// AFTER – counts distinct columns
Set<ColumnMetadata> columns = null;
for (Expression e : expressions) {
    if (!e.column.isPrimaryKeyColumn()) {
        if (columns == null)
            columns = new HashSet<>(expressions.size());
        columns.add(e.column);
        if (columns.size() > 1)
            return true;
    }
}
```

---

## How to Spot

When counting "distinct things" with a plain counter and iterating a collection that may contain duplicates, the counter will over-count. Any time a condition fires based on a threshold on such a counter, check whether the items iterated are guaranteed unique or whether duplicates must be deduplicated first (e.g. via a `Set`).

---

## Test

`RowFilterTest.testMutableIntersections` creates a filter with two range expressions (`GT`, `LT`) on the same column and asserts `isMutableIntersection()` returns `false`, then adds an expression on a second column and asserts it returns `true`.
