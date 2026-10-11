# Bug 2957: Hinted handoff strips tombstones before delivering hint

**Commit**: eeb164e2c0 · **Ticket**: CASSANDRA-491

## Detection Rule
When hinted handoff sends a row mutation to a recovering node, check whether the code calls `removeDeleted()` on the column families before sending — tombstones are valid data and must be delivered along with live columns.

## Code Smell
`sendMessage` builds the `RowMutation` by iterating `row.getColumnFamilies()` and calling `ColumnFamilyStore.removeDeleted(cf)` on each, which strips out tombstoned columns before the mutation is sent, so the recovering node never learns about the deletes.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: tombstones-stripped-before-hint-delivery
- severity: data-loss
- subsystem: storage-engine

## Root Cause
The hinted handoff code was purging tombstones from column families before constructing the `RowMutation` to deliver. If the original write was a delete (tombstone), stripping it meant the recovering node received an empty or truncated row and never applied the delete, leaving stale data. Similarly, the `deleteHintedData` helper tried to delete the hinted row from the application table using the maximum column timestamp as a tombstone — a complex and fragile approach that could also delete too much. The fix sends the raw row as-is (`new RowMutation(tableName, row)`) and changes hint cleanup to delete only the hint entry in the system hints CF via `deleteHintKey`.

## Example
```java
// BEFORE: tombstones stripped before delivery
Row purgedRow = new Row(tableName, key);
for (ColumnFamily cf : row.getColumnFamilies()) {
    purgedRow.addColumnFamily(ColumnFamilyStore.removeDeleted(cf));
}
RowMutation rm = new RowMutation(tableName, purgedRow);
```
```java
// AFTER: deliver row including tombstones unchanged
RowMutation rm = new RowMutation(tableName, row);
```
