# Bug 2652: NPE in maybeScheduleRepairs when resolveSuperset returns null after all columns are tombstoned

**Commit**: 058cf7fe69 · **Ticket**: CASSANDRA-2823

## Detection Rule
When `resolveSuperset()` / `resolveSuperSet()` can return `null` after `removeDeleted` purges all columns (empty tombstone row), guard every subsequent call to `maybeScheduleRepairs(resolved, ...)` with a null check on `resolved`.

## Code Smell
`RowRepairResolver.resolve()` and `RangeSliceResponseResolver` call `RowRepairResolver.maybeScheduleRepairs(resolved, ...)` unconditionally after `resolveSuperset(versions)`, even though `resolveSuperset` can return `null` when `removeDeleted` eliminates all columns from a tombstoned row.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-result-from-remove-deleted
- severity: crash
- subsystem: repair

## Root Cause
`resolveSuperset()` returns `null` when all versions of a row consist solely of tombstones that were purged by `removeDeleted`. Both `RowRepairResolver.resolve()` and `RangeSliceResponseResolver` passed this potentially-null value directly to `maybeScheduleRepairs()`, which dereferences it, causing a `NullPointerException` during read repair. The fix adds a null guard (`if (resolved != null)`) before the `maybeScheduleRepairs` call in both places.

## Example
```java
// BEFORE
resolved = resolveSuperset(versions);
maybeScheduleRepairs(resolved, table, key, versions, endpoints);  // NPE if resolved is null

// AFTER
resolved = resolveSuperset(versions);
// resolved can be null even if versions doesn't have all nulls (removeDeleted)
if (resolved != null)
    maybeScheduleRepairs(resolved, table, key, versions, endpoints);
```
