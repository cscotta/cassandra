# Bug 2509: CQL query NPE when row has no columns (row.cf is null)

**Commit**: a853d7e40f · **Ticket**: CASSANDRA-3424

## Detection Rule
When iterating rows returned from a range scan, check that `row.cf` is null-guarded before iterating its columns; an early `continue` on null skips adding the row key to the result, hiding tombstone-only rows instead of returning an empty column list.

## Code Smell
`if (row.cf == null) continue;` before building `thriftColumns` — this silently drops the entire row from CQL results instead of returning the row key with an empty column set, causing queries that expect to see all keys (even empty ones) to return fewer rows than requested.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: null-check-skips-row
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
A range query can return rows where `cf` is `null` (the row exists as a key but has only tombstoned or expired columns). The original code skipped such rows entirely before entering the column loop. The fix moves the null guard inside the column-iteration block so the row is still added to `cqlRows` with an empty column list, matching the expected behaviour of returning all matching keys.

## Example
```java
// BEFORE
for (Row row : rows) {
    if (row.cf == null)
        continue;           // row key is lost
    List<Column> thriftColumns = new ArrayList<>();
    for (IColumn c : row.cf.getSortedColumns()) { ... }
    cqlRows.add(new CqlRow(row.key, thriftColumns));
}
```
```java
// AFTER
for (Row row : rows) {
    List<Column> thriftColumns = new ArrayList<>();
    if (row.cf != null) {
        for (IColumn c : row.cf.getSortedColumns()) { ... }
    }
    cqlRows.add(new CqlRow(row.key, thriftColumns));
}
```
