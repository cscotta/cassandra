# Bug 2744: Index CF created with default settings instead of parent CF settings

**Commit**: 7a0ebb9cb5 · **Ticket**: CASSANDRA-2312

## Detection Rule
When `newIndexMetadata` constructs a secondary-index `CFMetaData`, verify it copies compaction thresholds and GC grace from the parent CF rather than using hardcoded defaults.

## Code Smell
`newIndexMetadata` accepts raw `String table, String parentCf` parameters and fills every optional field with `DEFAULT_*` constants, ignoring the parent CF's tuned settings.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: default-instead-of-parent-value
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause
`CFMetaData.newIndexMetadata` was called with the table name and CF name strings but had no reference to the parent `CFMetaData` object, so it used global defaults (e.g. `DEFAULT_GC_GRACE_SECONDS`, `DEFAULT_MIN_COMPACTION_THRESHOLD`) for the secondary-index CF. The fix passes the parent `CFMetaData` instance and reads `parent.gcGraceSeconds`, `parent.minCompactionThreshold`, etc.

## Example
```java
// BEFORE
public static CFMetaData newIndexMetadata(String table, String parentCf,
        ColumnDefinition info, AbstractType columnComparator) {
    return new CFMetaData(table, indexName(parentCf, info), ...,
        DEFAULT_GC_GRACE_SECONDS,
        DEFAULT_MIN_COMPACTION_THRESHOLD,
        DEFAULT_MAX_COMPACTION_THRESHOLD, ...);
}
```
```java
// AFTER
public static CFMetaData newIndexMetadata(CFMetaData parent,
        ColumnDefinition info, AbstractType columnComparator) {
    return new CFMetaData(parent.tableName, indexName(parent.cfName, info), ...,
        parent.gcGraceSeconds,
        parent.minCompactionThreshold,
        parent.maxCompactionThreshold, ...);
}
```
