# Bug 2653: SystemTable.getCurrentLocalNodeId asserts CF column count <= 1 but can receive tombstones due to gc_grace race

**Commit**: 2f16046480 · **Ticket**: CASSANDRA-2824

## Detection Rule
When reading from a system table with `gc_grace=0` and asserting a specific column count, apply `ColumnFamilyStore.removeDeleted(cf, 0)` before the assertion to remove tombstones that may still exist in a race window between write and compaction.

## Code Smell
`SystemTable.getCurrentLocalNodeId()` reads `cf` from the `NODE_ID_CF` column family and asserts `cf.getColumnCount() <= 1` without first calling `removeDeleted`, so if a tombstone from a concurrent delete is returned (before compaction fires at `gc_grace=0`), the column count includes the tombstone and the assertion fires.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: tombstone-not-purged-before-assert
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
`SystemTable.getCurrentLocalNodeId()` fetched the node ID column family and asserted `cf.getColumnCount() <= 1`. Even with `gc_grace=0` on the System table, there is a narrow race window where a tombstone exists (e.g., from a concurrent node ID rotation) before the background compaction purges it. The tombstone contributes to `getColumnCount()`, causing the assertion to fire spuriously at startup. The fix calls `ColumnFamilyStore.removeDeleted(cf, 0)` to eagerly purge tombstones before the assertion.

## Example
```java
// BEFORE
ColumnFamily cf = table.getColumnFamilyStore(NODE_ID_CF).getColumnFamily(filter);
if (cf != null) {
    assert cf.getColumnCount() <= 1;  // can fire if tombstone present

// AFTER
if (cf != null) {
    cf = ColumnFamilyStore.removeDeleted(cf, 0);  // purge tombstones first
    assert cf.getColumnCount() <= 1;
```
