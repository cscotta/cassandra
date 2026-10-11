# Bug 2648: Index scan throws assertion error and misfilters when dataFilter is SliceQueryFilter with single expression

**Commit**: 1f75bfa1a8 · **Ticket**: CASSANDRA-2653

## Detection Rule
When building the extra filter for an index scan, do not assert that `data` is non-null after fetching the primary indexed row — the row may be a tombstone or absent due to a race between index and data writes, and the satisfies check should evaluate all expressions including the primary one.

## Code Smell
`ColumnFamilyStore.scan()` skips the primary `IndexExpression` in `satisfies()` with `if (expression == first) continue`, but reads can race with writes so the primary column may be absent when the row is fetched; also the outer filter-expansion guard `if (clause.expressions.size() > 1)` means a single-expression scan never builds an extra filter even when needed.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: index-scan-assertion-false-primary-skip
- severity: crash
- subsystem: sai-secondary-index

## Root Cause
`scan()` guarded the extra-filter construction with `clause.expressions.size() > 1`, so a single-expression index scan never expanded the filter and could reach an assertion `assert data != null` that fired when the indexed row had been deleted. Additionally, `satisfies()` skipped the primary expression entirely (`if (expression == first) continue`), which was wrong because the index may return a row that no longer has the primary column at read time due to a race. The fix removes the size guard, removes the primary-skip in `satisfies()`, and replaces the assertion with a null-safe creation of an empty `ColumnFamily`.

## Example
```java
// BEFORE
if (clause.expressions.size() > 1) { /* build extra filter */ }
// ...
assert data != null;  // fires if row tombstoned
// in satisfies():
if (expression == first) continue;  // skip primary — wrong

// AFTER
if (dataFilter instanceof SliceQueryFilter) { /* always try to expand */ }
if (data == null) data = ColumnFamily.create(metadata);  // null-safe
// in satisfies():
for (IndexExpression expression : clause.expressions) { /* check all */ }
```
