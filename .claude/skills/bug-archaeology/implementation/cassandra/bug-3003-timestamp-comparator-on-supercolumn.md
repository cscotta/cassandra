# Bug 3003: ColumnTimestampComparator applied to SuperColumns, which have no timestamp

**Commit**: 9049d5919d · **Ticket**: n/a

## Detection Rule
If a comparator sorts columns by timestamp, ensure it is never applied to SuperColumn instances, which do not have a meaningful per-column timestamp and will cause a crash or incorrect ordering.

## Code Smell
`ColumnComparatorFactory.getComparator()` defaults to `ColumnTimestampComparator` for any unrecognized type, allowing it to be applied to SuperColumns that lack a sortable timestamp, resulting in a ClassCastException or undefined ordering.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: timestamp-comparator-on-supercolumn
- severity: crash
- subsystem: storage-engine

## Root Cause
`ColumnTimestampComparator.compare()` called `column.timestamp()` on both operands. `SuperColumn` does not define a meaningful timestamp, so applying the timestamp comparator to super-column collections caused crashes. The commit refactored the comparator factory (removing the debug `main()` method and standardizing indentation) as a prerequisite to separating super-column sorting from regular-column sorting, preventing the comparator from being applied to inappropriate column types.

## Example
```java
// BEFORE: default falls through to timestamp comparator, crashes on SuperColumn
public int compare(IColumn column1, IColumn column2) {
    long result = column2.timestamp() - column1.timestamp(); // NPE/CCE for SuperColumn
    ...
}

// AFTER: comparator factory cleaned up; timestamp comparator separated
// from name comparator so SuperColumns only use ColumnNameComparator
```
