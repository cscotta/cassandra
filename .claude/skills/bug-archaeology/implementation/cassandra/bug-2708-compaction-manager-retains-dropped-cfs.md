# Bug 2708: Dropped CFS objects retained in CompactionManager via strong map keys

**Commit**: 8deaa64751 · **Ticket**: CASSANDRA-2708

## Detection Rule
When a long-lived map uses object instances as keys and those objects can be dropped at runtime (e.g. ColumnFamilyStore after a drop), use weak-key references so the map doesn't prevent GC of the dropped objects.

## Code Smell
`private Map<ColumnFamilyStore, Integer> estimatedCompactions = new NonBlockingHashMap<>()` holds strong references to `ColumnFamilyStore` keys that may have been dropped, preventing garbage collection of the entire CFS object graph.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: strong-key-map-prevents-gc
- severity: performance
- subsystem: compaction

## Root Cause
`CompactionManager.estimatedCompactions` used a `NonBlockingHashMap` which holds strong references to its keys. When a `ColumnFamilyStore` was dropped, the entry remained in the map indefinitely, retaining the CFS object and all its resources. The fix replaces the map with a `MapMaker().weakKeys()` map so dropped CFS instances can be garbage collected once no other strong references exist.

## Example
```java
// BEFORE
private Map<ColumnFamilyStore, Integer> estimatedCompactions =
    new NonBlockingHashMap<ColumnFamilyStore, Integer>();
```
```java
// AFTER
private Map<ColumnFamilyStore, Integer> estimatedCompactions =
    new MapMaker().concurrencyLevel(1).weakKeys().makeMap();
```
