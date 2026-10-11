# Bug 2866: Race between DROP COLUMN FAMILY and concurrent compaction

**Commit**: f43e6217e3 · **Ticket**: CASSANDRA-1631

## Detection Rule
When a schema DROP operation calls storage cleanup directly on the calling thread without going through the compaction serialisation point, check that active compactions referencing the same SSTables will not conflict.

## Code Smell
`DropColumnFamily.applyModels` calls `Table.open(...).dropCf(...)` directly on the migration thread, bypassing the `CompactionManager` executor that serialises all compaction and drop work.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: bypass-serialization-executor
- severity: crash
- subsystem: compaction

## Root Cause
`dropCf` deletes SSTable files on disk. If a compaction is simultaneously reading those files, the direct call from the migration thread races with active compaction tasks. Routing the drop through `CompactionManager.submitDrop(...).get()` ensures it executes on the single-threaded compaction executor, after any in-progress compaction completes.

## Example
```java
// BEFORE (DropColumnFamily.applyModels)
if (!clientMode)
    Table.open(ksm.name).dropCf(cfm.cfId);
```
```java
// AFTER
if (!clientMode)
{
    try {
        CompactionManager.instance.submitDrop(cfs).get();
    } catch (ExecutionException ex) { ... }
}
```
