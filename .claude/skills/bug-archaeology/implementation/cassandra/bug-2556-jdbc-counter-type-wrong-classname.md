# Bug 2556: JDBC TypesMap registers wrong class name for CounterColumnType

**Commit**: c56760338e · **Ticket**: CASSANDRA-3268

## Detection Rule
When a static registry maps a fully-qualified class name string to a JDBC type handler, a typo in the key causes the mapping to be permanently absent, making every attempt to use the counter column type through JDBC fail silently.

## Code Smell
A `map.put("org.apache.cassandra.db.marshal.ColumnCounterType", ...)` entry appears in the type registry when the actual class is named `CounterColumnType`, so the key never matches and the handler is unreachable.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: mistyped-classname-key
- severity: crash
- subsystem: tools-clients

## Root Cause
`TypesMap` registered the counter column handler under `"ColumnCounterType"` instead of the actual class name `"CounterColumnType"`. Any CQL/JDBC query that touched a counter column would fail to resolve the type and throw or return wrong data.

## Example
```java
// BEFORE
map.put("org.apache.cassandra.db.marshal.ColumnCounterType",
        JdbcCounterColumn.instance);
```
```java
// AFTER
map.put("org.apache.cassandra.db.marshal.CounterColumnType",
        JdbcCounterColumn.instance);
```
