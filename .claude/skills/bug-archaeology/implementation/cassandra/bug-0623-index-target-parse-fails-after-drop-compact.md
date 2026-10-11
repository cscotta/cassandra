# Bug 0623: Index Target Parsing Fails for Dense Composite Tables After DROP COMPACT STORAGE

**Commit**: 0521f8dc5d · **Ticket**: CASSANDRA-14104
**Subsystem**: `index/internal/CassandraIndex.java`
**Category**: Logic error in condition

---

## Root Cause

`CassandraIndex.parseTarget()` used `CFMetaData.isCQLTable()` to decide whether to look up a column by CQL `ColumnIdentifier` (fast path) or by a linear scan comparing `column.name.toString()` (slow path for thrift/compact tables). After `DROP COMPACT STORAGE` is executed, a formerly-compact table reports `isCQLTable() == true`, so the code took the fast path and called `getColumnDefinition(new ColumnIdentifier(columnName, true))`. For columns whose names were encoded with Thrift/DynamicCompositeType bytes, the CQL identifier lookup returns `null`, causing a `NullPointerException` or a `RuntimeException` when checking the index. The fix removes the `isCQLTable()` branch: it always tries the CQL lookup first, and if that returns `null`, falls back to the linear scan. A `null` return from the refactored private overload is translated into a `ConfigurationException` at the call site.

### Before / After

```java
// BEFORE
if (cfm.isCQLTable())
    return Pair.create(cfm.getColumnDefinition(new ColumnIdentifier(columnName, true)), targetType);
else
    for (ColumnDefinition column : cfm.allColumns())
        if (column.name.toString().equals(columnName))
            return Pair.create(column, targetType);

throw new RuntimeException(...);
```

```java
// AFTER
ColumnDefinition cd = cfm.getColumnDefinition(new ColumnIdentifier(columnName, true));
if (cd != null)
    return Pair.create(cd, targetType);

for (ColumnDefinition column : cfm.allColumns())
    if (column.name.toString().equals(columnName))
        return Pair.create(column, targetType);

return null; // caller throws ConfigurationException
```

---

## How to Spot

Branching on a table-type flag (`isCQLTable()`, `isCompactTable()`) that can be changed by a schema operation, while storing index metadata that was written under the old type, creates inconsistency. Reviewers should look for code paths that distinguish between "CQL" and "thrift" table layouts and verify that those paths remain valid after schema alterations like `DROP COMPACT STORAGE`.

---

## Test

`DropCompactStorageThriftTest.denseCompositeWithIndexesTest` creates a DynamicCompositeType table via the Thrift API with three indexed columns, drops compact storage, and asserts that `parseTarget` returns identical results before and after the conversion.
