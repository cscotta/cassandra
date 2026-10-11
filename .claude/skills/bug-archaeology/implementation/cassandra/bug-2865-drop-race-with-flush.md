# Bug 2865: Race between DROP COLUMN FAMILY and concurrent flush

**Commit**: 3f83c10ef1 · **Ticket**: CASSANDRA-1631

## Detection Rule
When a DROP operation removes schema metadata and then asynchronously cleans up storage, check that concurrent flush tasks that check the schema before writing will see the drop and abort cleanly.

## Code Smell
`ColumnFamilyStore.switchMemtable` proceeds to freeze and flush a memtable without checking whether the column family has already been dropped from `DatabaseDescriptor`. The `dropCf` call in `CompactionManager` runs outside the `flusherLock` write lock.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: check-then-act-without-lock
- severity: crash
- subsystem: storage-engine

## Root Cause
Two concurrent paths — a flush triggered just before a CF drop, and the drop itself — could race. The fix adds a null-check in `switchMemtable` to abort flushing if the CF has been removed from the schema, and wraps `table.dropCf()` in the `flusherLock` write lock so it is serialised with respect to all ongoing read-locked flush operations.

## Example
```java
// BEFORE (ColumnFamilyStore.switchMemtable)
if (oldMemtable.isFrozen())
    return null;
assert memtable == oldMemtable;
memtable.freeze();
// proceeds even if CF was just dropped
```
```java
// AFTER
if (oldMemtable.isFrozen())
    return null;
if (DatabaseDescriptor.getCFMetaData(metadata.cfId) == null)
    return null; // column family was dropped. no point in flushing.
assert memtable == oldMemtable;
memtable.freeze();
```
