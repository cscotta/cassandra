# Bug 2892: Obsolete secondary index writes not suppressed before applying mutation

**Commit**: 6055d1b696 · **Ticket**: n/a (precursor to CASSANDRA-1546)

## Detection Rule
When applying a write that updates a secondary index, check that the new column value actually wins over the existing value before writing a new index entry, to avoid creating stale index entries for writes that will be resolved away.

## Code Smell
`Table.apply` updates secondary index entries for every mutation touching an indexed column without first checking whether the new column value is actually newer than the stored one. Concurrent or out-of-order writes could insert new index entries for values that would ultimately be discarded by reconciliation.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: index-write-without-obsolete-check
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause
The index update path read the current indexed column value to build the "remove old entry" list but did not filter out mutations that the reconciler would discard. Adding a check using `cfs.metadata.reconciler.reconcile(oldColumn, newColumn).equals(oldColumn)` before writing the new index entry skips updates for writes that would lose to the existing value.

## Example
```java
// BEFORE — always writes new index entry even for obsolete mutations
applyCF(cfs, key, cf, memtablesToFlush);
for (byte[] columnName : mutatedIndexedColumns) {
    IColumn column = cf.getColumn(columnName);
    // writes index entry regardless of whether this write wins
    applyCF(indexCfs, valueKey, cfi, memtablesToFlush);
}
```
```java
// AFTER — skip index update if new value loses reconciliation
if (reconciler.reconcile(oldColumn, newColumn).equals(oldColumn)) {
    cf.remove(oldColumn.name());
    mutatedIndexedColumns.remove(oldColumn.name());
}
```
