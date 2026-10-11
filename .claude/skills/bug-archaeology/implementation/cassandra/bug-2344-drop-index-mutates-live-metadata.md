# Bug 2344: DROP INDEX mutates live CFMetaData in place instead of cloning

**Commit**: f58a51bc5f · **Ticket**: n/a (CHANGES entry: "(cql3) Fix drop index")

## Detection Rule

When code that removes or clears a field on an object retrieved from a live registry (e.g., `cfm.getColumn_metadata()`) does so without first cloning that object, check whether the registry holds a shared reference.

## Code Smell

A DDL statement calls a setter (`.setIndexName(null)`, `.setIndexType(null, null)`) directly on a `ColumnDefinition` returned from the live `CFMetaData` without cloning the parent first, meaning concurrent readers see an inconsistent half-cleared index definition.

## Tags

- scope: cassandra
- category: state-not-cleaned
- code-pattern: mutate-without-clone
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause

`DropIndexStatement.removeIndexFromCfm()` retrieved the target `ColumnDefinition` from the live `CFMetaData`, then cleared its index fields in-place and returned the same (modified) object. Because `CFMetaData` was shared, any concurrent reader or schema serialisation could observe an intermediate state where the index type was null but the name was not yet cleared (or vice versa). The fix clones `cfm` first, then modifies the column in the clone, so the live metadata is only replaced atomically once the migration is announced.

Additionally, `addDefaultIndexNames()` did not deduplicate generated index names against existing indexes in other column families, so loading a schema could produce collisions. The fix collects all existing names first and appends `_N` suffixes as needed.

## Example

```java
// BEFORE
if (column.getIndexType() != null && column.getIndexName() != null
        && column.getIndexName().equals(index)) {
    column.setIndexName(null);      // mutates live metadata
    column.setIndexType(null, null);
    return cfm;
}
```

```java
// AFTER
if (...) {
    CFMetaData cloned = cfm.clone();
    ColumnDefinition toChange = cloned.getColumn_metadata().get(column.name);
    toChange.setIndexName(null);
    toChange.setIndexType(null, null);
    return cloned;   // caller replaces atomically
}
```
