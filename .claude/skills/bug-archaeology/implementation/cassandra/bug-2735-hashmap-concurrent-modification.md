# Bug 2735: HashMap for columnFamilyStores allows ConcurrentModificationException during CF add/drop

**Commit**: 46b0dd5f10 · **Ticket**: CASSANDRA-2350

## Detection Rule
When a map is read by multiple threads without external synchronization and can be mutated by schema-change operations, use a concurrent map implementation.

## Code Smell
`Table.columnFamilyStores` is a `public final HashMap` iterated in compaction and other background threads; concurrent `add_column_family` / `drop_column_family` operations modify it without a lock, causing `ConcurrentModificationException`.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: unsynchronized-map-iteration
- severity: crash
- subsystem: schema-metadata

## Root Cause
`Table.columnFamilyStores` used a plain `HashMap`. Background threads (compaction manager, flush scheduler) iterated `values()` without holding any lock, while schema-change migrations added or removed entries from the main thread. The structural modification to `HashMap` invalidates active iterators, causing `ConcurrentModificationException`. The fix changes the field to a `ConcurrentHashMap` and makes it private, providing safe concurrent iteration.

## Example
```java
// BEFORE — plain HashMap, public, mutation races with iteration
public final Map<Integer, ColumnFamilyStore> columnFamilyStores =
    new HashMap<Integer, ColumnFamilyStore>();
```
```java
// AFTER — ConcurrentHashMap is safely iterable under concurrent mutations
private final Map<Integer, ColumnFamilyStore> columnFamilyStores =
    new ConcurrentHashMap<Integer, ColumnFamilyStore>();
```
