# Bug 0436: Super-Column ColumnFilter Not Applied for Range Reads, Causing Wrong Cell Skipping

**Commit**: f106ef0697 · **Ticket**: CASSANDRA-16223
**Subsystem**: `src/java/org/apache/cassandra/cql3/statements/SelectStatement.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectStatement` maintained two separate code paths for building a `ColumnFilter`: partition-key (IN) lookups used a super-column-aware branch that called `SuperColumnCompatibility.getColumnFilter`, while range scans used the pre-built `queriedColumns` field unconditionally. For dense super-column tables this meant the range-scan path always applied the wrong filter, causing the reader to skip cell values that should have been returned (or include ones that should not). The fix extracts a shared `createColumnFilter(options)` helper that is called in both paths.

### Before / After

```java
// BEFORE – IN path used super-column filter; range path used queriedColumns always
ColumnFilter cf = (cfm.isSuper() && cfm.isDense())
    ? SuperColumnCompatibility.getColumnFilter(cfm, options, restrictions.getSuperColumnRestrictions())
    : queriedColumns;
// range read:
PartitionRangeReadCommand.create(..., queriedColumns, ...);
```

```java
// AFTER – shared helper used in both paths
private ColumnFilter createColumnFilter(QueryOptions options) {
    return (cfm.isSuper() && cfm.isDense())
           ? SuperColumnCompatibility.getColumnFilter(cfm, options, restrictions.getSuperColumnRestrictions())
           : queriedColumns;
}
// range read:
PartitionRangeReadCommand.create(..., createColumnFilter(options), ...);
```

---

## How to Spot

When a conditional (type-check or feature-flag) code path is duplicated only in one of several sibling call sites that share the same logical operation, the branches will diverge as the code evolves. Review `SELECT` statement construction and look for any `ColumnFilter` or `rowFilter` assignment that is not routed through a single factory method — if the same `isSuper() && isDense()` guard appears in one call site but not its sibling, a filter inconsistency almost certainly exists.

---

## Test

`ThriftIntegrationTest.flushAll()` was corrected to also flush the dense table and call `forceBlockingFlush()`, ensuring the on-disk path is exercised and the cell-skipping fix is validated against persisted data.
