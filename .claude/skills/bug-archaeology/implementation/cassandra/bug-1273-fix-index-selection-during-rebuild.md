# Bug 1273: Wrong Index Selected During Rebuild With Certain Table Layouts

**Commit**: 1e35fa4b5a · **Ticket**: CASSANDRA-9281
**Subsystem**: db/index/SecondaryIndexManager.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When filtering which secondary indexes should participate in a rebuild, `SecondaryIndexManager` used `candidate.indexes(column.name)` to test membership. For per-row secondary indexes, `indexes(ByteBuffer name)` can be overridden to return `true` unconditionally or with custom logic unrelated to the actual column definitions tracked by the index object. This meant an index could report itself as covering a column it had no definition for, causing the wrong index to be selected or rebuilt when multiple indexes exist on the same table. The correct check is against the index's own `ColumnDefinition` set, which is the authoritative record of which columns it manages.

### Before / After

```java
// BEFORE
if (candidate.indexes(column.name))
{
    filtered.add(candidate.getIndexName());
    break;
}
```

```java
// AFTER
if (candidate.getColumnDefs().contains(column))
{
    filtered.add(candidate.getIndexName());
    break;
}
```

---

## How to Spot

Any place that asks "does this index cover column X?" should query the index's structural metadata (`getColumnDefs()`) rather than calling a virtual method whose contract may be broadened by subclasses. The `indexes(ByteBuffer)` method was designed as an internal row-indexing hook, not as a reliable membership predicate for rebuild selection. Reviewers should treat calls to behavioral override methods as suspicious when the intent is a simple set-membership check.

---

## Test

An existing test in `ColumnFamilyStoreTest.testRebuildSecondaryIndex` was updated: instead of toggling a static `ACTIVE` flag on the test index (which overrode `indexes()` globally), it now directly removes the column definition from the index's `ColumnDefs` set to simulate the "not covering this column" scenario, matching the new implementation.
