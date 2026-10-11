# Bug 2728: Race between schema migration and memtable flush can leave orphaned SSTable data files

**Commit**: 979ce41ef4 · **Ticket**: CASSANDRA-2381

## Detection Rule
When a schema change (e.g., drop column family) must ensure no in-flight writes land in files after the drop, hold a migration lock that blocks flushes during the critical window.

## Code Smell
`ColumnFamilyStore` flushes memtables without acquiring any migration-awareness lock; a concurrent `drop_column_family` migration completes the drop bookkeeping while a flush is mid-way, producing SSTable files for a column family that no longer exists.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: flush-without-migration-lock
- severity: data-loss
- subsystem: storage-engine

## Root Cause
The existing `Table.flusherLock` (renamed `switchLock`) coordinated memtable switches between threads but was not visible to migration operations. A `drop_column_family` migration could complete and remove the CF's directory references while a concurrent flush thread was writing SSTable segments for that CF, leaving orphaned files on disk that would cause errors on restart. The fix adds a per-`ColumnFamilyStore` `flushLock` (a `ReentrantLock`) that migrations acquire before proceeding, and renames the table-level lock to `switchLock` to clarify its purpose.

## Example
```java
// BEFORE — flush proceeds with no migration awareness
Table.flusherLock.writeLock().lock();
// ... switch memtable, write sstable ...
Table.flusherLock.writeLock().unlock();
```
```java
// AFTER — per-CFS flushLock lets migrations block flushes
public final Lock flushLock = new ReentrantLock();
// migrations acquire flushLock before dropping CF
Table.switchLock.writeLock().lock();
// ... switch memtable ...
Table.switchLock.writeLock().unlock();
```
