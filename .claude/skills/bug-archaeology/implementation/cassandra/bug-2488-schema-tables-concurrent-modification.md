# Bug 2488: ConcurrentModificationException in Schema.tables — HashMap iterated while written concurrently

**Commit**: 353fadd73c · **Ticket**: CASSANDRA-3529

## Detection Rule

When a `HashMap` field in a singleton class is iterated (e.g., in `Table.all()`) while another thread can concurrently add or remove entries (e.g., schema changes), a `ConcurrentModificationException` will be thrown.

## Code Smell

`private final Map<String, KSMetaData> tables = new HashMap<...>()` on a shared singleton accessed from multiple threads without synchronization on reads, while writes occur via schema update code paths.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: unsynchronized-hashmap-iteration
- severity: crash
- subsystem: schema-metadata

## Root Cause

`Schema` used `HashMap` for `tables` and `tableInstances`. `Table.all()` iterated over `tableInstances` without locking, while schema change operations (DDL) could add entries concurrently. The `ConcurrentModificationException` was thrown during iteration when a concurrent write modified the map's structural version. The fix replaces both `HashMap` instances with `NonBlockingHashMap`, which is safe for concurrent iteration and modification.

## Example

```java
// BEFORE
private final Map<String, KSMetaData> tables;
private final Map<String, Table> tableInstances;
// in constructor:
tables = new HashMap<String, KSMetaData>();
tableInstances = new NonBlockingHashMap<String, Table>();  // inconsistent
```

```java
// AFTER
private final Map<String, KSMetaData> tables = new NonBlockingHashMap<String, KSMetaData>();
private final Map<String, Table> tableInstances = new NonBlockingHashMap<String, Table>();
```
