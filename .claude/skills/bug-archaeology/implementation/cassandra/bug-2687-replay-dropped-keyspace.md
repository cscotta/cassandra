# Bug 2687: Commitlog replay crashes on mutations from dropped keyspaces

**Commit**: 022208bf0f · **Ticket**: CASSANDRA-2631

## Detection Rule
When replaying commitlog segments, check that the keyspace referenced by each mutation still exists in the schema before attempting to open it, to avoid exceptions on mutations from keyspaces that were dropped after the commitlog was written.

## Code Smell
`Table.open(rm.getTable())` called inside the commitlog replay loop without first verifying that `DatabaseDescriptor.getKSMetaData(rm.getTable())` returns non-null.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: missing-schema-existence-check
- severity: crash
- subsystem: commitlog-cdc

## Root Cause
During commitlog recovery, mutations for keyspaces that existed when the commitlog segment was written but were subsequently dropped would cause `Table.open()` to fail or produce incorrect results. The fix adds an early return inside the replay runnable: if `DatabaseDescriptor.getKSMetaData(frm.getTable()) == null`, the mutation is silently skipped. Also fixes a subtle bug where `tablesRecovered` was populated before replay, rather than after, by moving the `tablesRecovered.add(table)` call inside the non-empty mutation path.

## Example
```java
// BEFORE – no keyspace existence check before Table.open()
final Table table = Table.open(rm.getTable());
tablesRecovered.add(table);
```
```java
// AFTER – guard added inside the runnable
if (DatabaseDescriptor.getKSMetaData(frm.getTable()) == null)
    return;
final Table table = Table.open(frm.getTable());
// ... replay ...
if (!newRm.isEmpty()) {
    Table.open(newRm.getTable()).apply(newRm, null, false);
    tablesRecovered.add(table);
}
```
