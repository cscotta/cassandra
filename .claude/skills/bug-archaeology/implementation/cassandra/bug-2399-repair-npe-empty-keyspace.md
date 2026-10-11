# Bug 2399: Repair NPE when a keyspace has no column families

**Commit**: ef01ca5bcf · **Ticket**: CASSANDRA-3988

## Detection Rule

When a method may legitimately return null to signal "nothing to do," every call site must check for null before using the return value.

## Code Smell

`forceTableRepair(range, tableName, columnFamilies)` returns `null` when the column-family list is empty, but both callers immediately call `.add(future)` or `.get()` on the return value without a null check.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: unchecked-null-return-value
- severity: crash
- subsystem: repair

## Root Cause

`StorageService.forceTableRepair` returns `null` when the resolved list of column family names is empty (e.g. a keyspace with no user CFs). Its two callers — the range-based repair loop and the single-range repair — both stored the result directly in a `List` or called `.get()` without first checking for null. The fix adds an early `return null` inside the helper when the name list is empty and null-checks at both call sites before using the future.

## Example

```java
// BEFORE
AntiEntropyService.RepairFuture future = forceTableRepair(range, tableName, columnFamilies);
futures.add(future);   // NPE if future is null (empty keyspace)
future.get();          // NPE in single-range path
```

```java
// AFTER
if (names.isEmpty()) {
    logger_.info("No column family to repair for keyspace " + tableName);
    return null;
}
// callers:
AntiEntropyService.RepairFuture future = forceTableRepair(...);
if (future == null) continue;   // or return
futures.add(future);
```
