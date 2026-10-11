# Bug 2494: Incorrect query results when EchoedRow.maxTimestamp() throws UnsupportedOperationException

**Commit**: aee40158c0 · **Ticket**: CASSANDRA-3510

## Detection Rule

When `maxTimestamp()` throws `UnsupportedOperationException` instead of returning a sentinel value, any caller that uses the timestamp for optimization decisions (e.g., skipping column lookups) will crash rather than safely fall back.

## Code Smell

`public long maxTimestamp() { throw new UnsupportedOperationException(); }` in a concrete class where callers use the return value as a filter — the exception propagates instead of triggering a safe fallback path.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: unsupported-operation-instead-of-sentinel
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`EchoedRow.maxTimestamp()` threw `UnsupportedOperationException` because computing it would require deserializing the row. `CollationController.collectTimeOrderedData()` used `sstableTimestamp` (sourced from `maxTimestamp()`) to skip fetching columns that were definitely present in a more recent SSTable. When this optimization was applied with `Long.MIN_VALUE` from an old SSTable without metadata, the code incorrectly pruned columns. The fix makes `EchoedRow.maxTimestamp()` return `Long.MIN_VALUE` (meaning "unknown") and adds a guard `|| sstableTimestamp == Long.MIN_VALUE` to disable the timestamp-based optimization when the value is unknown.

## Example

```java
// BEFORE
public long maxTimestamp() {
    throw new UnsupportedOperationException();
}

// In CollationController:
if (container == null)
    return;  // missed the Long.MIN_VALUE sentinel case
```

```java
// AFTER
public long maxTimestamp() {
    return Long.MIN_VALUE;  // sentinel: "unknown"
}

// In CollationController:
if (container == null || sstableTimestamp == Long.MIN_VALUE)
    return;  // skip optimization when timestamp is unknown
```
