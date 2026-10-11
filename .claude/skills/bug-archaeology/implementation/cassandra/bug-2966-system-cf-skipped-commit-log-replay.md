# Bug 2966: System table uses wrong comparator and skips hint CF during commit log replay

**Commit**: 7158863861 · **Ticket**: CASSANDRA-362

## Detection Rule
When adding a new column family to a system table definition, check whether the commit log replay code skips that CF via an `isApplicationColumnFamily` guard — system CFs that are not application CFs will have their commit log entries silently dropped during recovery.

## Code Smell
`CommitLog` replay has a guard `if (!DatabaseDescriptor.isApplicationColumnFamily(columnFamily.name())) { row.removeColumnFamily(cf); continue; }` that discards all non-application column families, causing the `HintedHandOffManager.HINTS_CF` to be skipped during crash recovery.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: system-cf-skipped-in-replay
- severity: data-loss
- subsystem: commitlog-cdc

## Root Cause
Three related bugs fixed together: (1) The `LOCATION_CF` in the system table used `AsciiType` as its comparator but tokens are stored as UTF-8 strings, so `SystemTable.loadStorageInfo()` used a `NamesQueryFilter` to look up `GENERATION` by name but the column name might not be found with the wrong comparator. (2) The `HINTS_CF` was added to the system table but the commit log replay code explicitly removed non-application CFs from the row before applying, so hints written to the commit log before a crash were silently discarded on recovery. (3) The system table's CF map was keyed on the string `"system"` rather than `Table.SYSTEM_TABLE`, causing it to never be found. The fix corrects the comparator, removes the faulty `isApplicationColumnFamily` guard, and uses the constant `Table.SYSTEM_TABLE` consistently.

## Example
```java
// BEFORE: non-application CFs (including HINTS_CF) skipped during replay
if (!DatabaseDescriptor.isApplicationColumnFamily(columnFamily.name())) {
    row.removeColumnFamily(columnFamily);
    continue;
}
```
```java
// AFTER: all CFs replayed (system CFs also need recovery)
// guard removed entirely
int id = table.getColumnFamilyId(columnFamily.name());
if (!clHeader.isDirty(id) || ...) { continue; }
```
