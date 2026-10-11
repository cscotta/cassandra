# Bug 2362: CLI applyAssumption crashes with RuntimeException for unknown column families

**Commit**: 189e133a58 · **Ticket**: CASSANDRA-4052

## Detection Rule

When a method calls a helper that throws a `RuntimeException` for a "not found" condition, ensure callers that should treat "not found" as a non-fatal case catch that exception rather than letting it propagate.

## Code Smell

`applyAssumption(cfName, ...)` calls `getCfDef(cfName)` which throws a `RuntimeException` if the CF does not exist; the caller does not catch this, so refreshing the CLI schema causes an unhandled exception to surface instead of silently skipping assumptions for dropped CFs.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: uncaught-not-found-exception
- severity: crash
- subsystem: tools-clients

## Root Cause

When the CLI user refreshes the schema, `applyAssumption` is re-evaluated for all stored assumptions. If a column family referenced by an assumption has since been dropped, `getCfDef(cfName)` throws a `RuntimeException`. Without a catch, this propagates out of the schema-refresh path and crashes the CLI. The fix wraps the call in a try/catch and returns `false` (skip this assumption) when the CF is not found.

## Example

```java
// BEFORE
private boolean applyAssumption(String cfName, String assumptionElement, String defaultType) {
    CfDef columnFamily = getCfDef(cfName); // throws RuntimeException if CF missing
    ...
}
```

```java
// AFTER
private boolean applyAssumption(String cfName, String assumptionElement, String defaultType) {
    CfDef columnFamily;
    try {
        columnFamily = getCfDef(cfName);
    } catch (RuntimeException e) {
        return false; // just fail if CF does not exist
    }
    ...
}
```
