# Bug 2956: Hinted handoff delete uses column's own timestamp instead of current time

**Commit**: c1e22db3bf · **Ticket**: CASSANDRA-491

## Detection Rule
When deleting a column/supercolumn after delivering a hint, check whether the deletion timestamp comes from the hinted data itself — if it does, the tombstone may be older than the data it should supersede.

## Code Smell
`deleteEndPoint(endpoint.name(), tableName, keyColumn.name(), keyColumn.timestamp())` uses the original hinted-data write timestamp as the delete marker; if a newer write arrived in the meantime, this tombstone with an older timestamp will not suppress it, and the old tombstone will also be older than the existing data and ineffective.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: delete-with-old-timestamp
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
After successfully delivering a hint, the code used the timestamp of the hinted column as the delete timestamp for removing the hint entry. If additional writes occurred to the same key after the hint was stored, the original timestamp could be older than the current column data, meaning the deletion tombstone would be superseded by the live data and the hint entry would not actually be removed. Using `System.currentTimeMillis()` as the delete timestamp guarantees the tombstone is newer than any previously stored data and the hint cleanup succeeds.

## Example
```java
// BEFORE: delete timestamp from hinted column (may be stale)
deleteEndPoint(endpoint.name(), tableName, keyColumn.name(),
    keyColumn.timestamp());
```
```java
// AFTER: delete with current wall time
deleteEndPoint(endpoint.name(), tableName, keyColumn.name(),
    System.currentTimeMillis());
```
