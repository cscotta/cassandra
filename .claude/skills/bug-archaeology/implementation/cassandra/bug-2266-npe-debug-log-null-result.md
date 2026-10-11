# Bug 2266: NPE in ColumnFamilyStore Debug Logging on Null Read Result

**Commit**: 5b6a2b11bc · **Ticket**: CASSANDRA-1123

## Detection Rule

When a method can return null and the return value is passed directly as an argument to a method call (including logger format arguments), check that the null case is guarded before dereferencing.

## Code Smell

`result.getColumnCount()` is called in a `logger.debug` argument unconditionally even though `result` may be null when no row is found; the null check only appears after this statement.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: null-dereference-in-log-arg
- severity: crash
- subsystem: cql-query

## Root Cause

After a read operation, `result` can be null when no matching row exists. The debug log statement called `result.getColumnCount()` before checking whether `result` was null, causing an NPE on every cache-miss read path. The fix guards the method call with a ternary that substitutes `0` when `result` is null.

## Example

```java
// BEFORE
logger.debug("Read {} columns", result.getColumnCount());
return result;
```

```java
// AFTER
logger.debug("Read {} columns", result == null ? 0 : result.getColumnCount());
return result;
```
