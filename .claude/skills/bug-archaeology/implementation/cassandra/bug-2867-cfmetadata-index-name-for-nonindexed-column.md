# Bug 2867: CFMetaData generates index name for columns that have no index type

**Commit**: 8a79b6476f · **Ticket**: CASSANDRA-2867

## Detection Rule
When auto-generating an index name for a `ColumnDefinition`, check that `getIndexType() != null` before checking `getIndexName() == null`, so that unindexed columns are never assigned an index name.

## Code Smell
`if (cd.getIndexName() == null) cd.setIndexName(...)` will set an index name on every column that lacks one, regardless of whether the column has an index type at all.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: missing-precondition-check
- severity: crash
- subsystem: sai-secondary-index

## Root Cause
`CFMetaData.fromThrift()` iterated over column metadata and assigned a default index name to any `ColumnDefinition` where `getIndexName()` was null. This condition is also true for columns that have no secondary index at all (`getIndexType() == null`), causing every non-indexed column to receive a spurious index name. Downstream code that expected index names only on indexed columns would then try to create index column families for non-indexed columns, triggering assertion errors. The fix adds an `indexType != null` precondition.

## Example
```java
// BEFORE
if (cd.getIndexName() == null)
    cd.setIndexName(getDefaultIndexName(cf.name.toString(), comparator, cd.name));
```
```java
// AFTER
if (cd.getIndexType() != null && cd.getIndexName() == null)
    cd.setIndexName(getDefaultIndexName(cf.name.toString(), comparator, cd.name));
```
