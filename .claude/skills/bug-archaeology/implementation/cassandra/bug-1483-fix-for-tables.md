# Bug 1483: Fix CFMetaData#isThriftCompatible() for PK-only Tables

**Commit**: 9219361b62 · **Ticket**: CASSANDRA-7832
**Subsystem**: config / CFMetaData
**Category**: Logic error in condition

---

## Root Cause

`CFMetaData.isThriftCompatible()` iterated over regular column definitions and returned `false` if any column used a CQL3-only type. However, a table with no regular columns (a PK-only CQL3 table) would pass the loop without emitting a `false`, then fall through to `return true`, incorrectly claiming Thrift compatibility. The real indicator of incompatibility — using a CQL3-only PK comparator with `isDense == false` — was never checked in the absence of regular columns.

### Before / After

```java
// BEFORE
for (ColumnDefinition def : allColumns())
{
    if (def.type == ColumnDefinition.Type.REGULAR && !def.isThriftCompatible())
        return false;
}
return true;   // wrong for PK-only CQL3 tables
```

```java
// AFTER
for (ColumnDefinition def : allColumns())
{
    if (def.type == ColumnDefinition.Type.REGULAR && !def.isThriftCompatible())
        return false;
}
// The table might also have no REGULAR columns (be PK-only), but still be "thrift incompatible".
if (isCQL3OnlyPKComparator(comparator) && !isDense)
    return false;
return true;
```

---

## How to Spot

When a loop-based check is the only guard before a `return true`, consider whether the positive result is also valid for the degenerate empty-collection case. Tables with no regular columns are a legitimate edge case that bypasses all loop iterations.

---

## Test

No test added in this commit.
