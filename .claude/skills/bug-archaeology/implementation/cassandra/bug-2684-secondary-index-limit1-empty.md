# Bug 2684: Secondary index with limit=1 returns empty result

**Commit**: 4a604fbebc · **Ticket**: CASSANDRA-2628

## Detection Rule
When fetching candidate rows from a secondary index table and the caller's `limit` is 1, fetch at least 2 rows so that paging can skip an initial row that fails additional filter predicates without returning an empty result.

## Code Smell
Passing `clause.count` (which may be 1) directly as the fetch size to the index table's `QueryFilter`, causing the paging loop to see an empty next-page when the single fetched row did not satisfy all clauses.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: index-paging-minimum-fetch
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause
The secondary-index scan fetched exactly `clause.count` rows from the index table per page. With `count=1`, if the first candidate row failed non-index filter predicates, the paging logic had no remaining candidates and returned an empty result even though matching rows existed later in the index. The fix ensures the per-page fetch size is at least 2 (`Math.max(clause.count, 2)`) so that skipping a non-matching first row still leaves a candidate to test.

## Example
```java
// BEFORE
QueryFilter indexFilter = QueryFilter.getSliceFilter(indexKey, ..., clause.count);
```
```java
// AFTER
int count = Math.max(clause.count, 2);
QueryFilter indexFilter = QueryFilter.getSliceFilter(indexKey, ..., count);
```
