# Bug 2676: Hints replayed to dropped column families cause crash

**Commit**: 40a13696e4 · **Ticket**: CASSANDRA-2685

## Detection Rule
When replaying stored hints, check that the target keyspace and column family still exist before attempting to open them, to avoid exceptions from trying to operate on dropped schema objects.

## Code Smell
`Table.open(tableName)` and `table.getColumnFamilyStore(cfName)` called without first verifying that `CFMetaData.getId(tableName, cfName)` returns non-null, which throws if the column family was dropped after the hint was stored.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: missing-schema-existence-check
- severity: crash
- subsystem: storage-engine

## Root Cause
`HintedHandOffManager.deliverHintsToEndpoint()` attempted to open the table and column family store for every pending hint without first checking whether the column family was still defined. If the CF was dropped after the hint was recorded, `Table.open()` or `getColumnFamilyStore()` would throw an exception. The fix adds a guard using `CFMetaData.getId()` that discards hints for dropped schema objects with a debug log.

## Example
```java
// BEFORE
Table table = Table.open(tableName);
DecoratedKey dkey = StorageService.getPartitioner().decorateKey(key);
ColumnFamilyStore cfs = table.getColumnFamilyStore(cfName);
```
```java
// AFTER
if (CFMetaData.getId(tableName, cfName) == null)
{
    logger_.debug("Discarding hints for dropped keyspace or columnfamily {}/{}", tableName, cfName);
    return true;
}
Table table = Table.open(tableName);
ColumnFamilyStore cfs = table.getColumnFamilyStore(cfName);
DecoratedKey dkey = StorageService.getPartitioner().decorateKey(key);
```
