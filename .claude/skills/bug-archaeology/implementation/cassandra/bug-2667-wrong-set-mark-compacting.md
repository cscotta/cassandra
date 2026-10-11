# Bug 2667: Wrong set used when marking SSTables as compacting

**Commit**: d9a19253e8 · **Ticket**: CASSANDRA-2765

## Detection Rule
When building a new immutable set to replace a tracked set, verify that the existing tracked set (not the total live set) is used as the base for the new set.

## Code Smell
`ImmutableSet.builder().addAll(sstables)` used instead of `ImmutableSet.builder().addAll(compacting)` in a method named `markCompacting`, so all live SSTables get added to the compacting set instead of only the already-compacting ones plus the new batch.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-field-in-builder
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause
`DataTracker.View.markCompacting()` accumulated new compacting SSTables on top of *all* live SSTables (`sstables`) rather than on top of the already-compacting set (`compacting`). In multithreaded compaction this caused the entire live set to be treated as actively compacting, blocking concurrent compaction tasks from picking any SSTable.

## Example
```java
// BEFORE
public View markCompacting(Collection<SSTableReader> tomark)
{
    Set<SSTableReader> compactingNew =
        ImmutableSet.<SSTableReader>builder().addAll(sstables).addAll(tomark).build();
    return new View(memtable, memtablesPendingFlush, sstables, compactingNew);
}
```
```java
// AFTER
public View markCompacting(Collection<SSTableReader> tomark)
{
    Set<SSTableReader> compactingNew =
        ImmutableSet.<SSTableReader>builder().addAll(compacting).addAll(tomark).build();
    return new View(memtable, memtablesPendingFlush, sstables, compactingNew);
}
```
