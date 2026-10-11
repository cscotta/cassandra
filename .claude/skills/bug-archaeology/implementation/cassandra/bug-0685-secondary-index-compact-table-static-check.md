# Bug 0685: Secondary Index Query on COMPACT Table Fails Due to containsStaticColumns Returning True

**Commit**: 57c590f6f7 · **Ticket**: CASSANDRA-13627
**Subsystem**: `src/java/org/apache/cassandra/cql3/selection/Selection.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`Selection.containsStaticColumns()` returned `true` for static-compact (COMPACT STORAGE without clustering) tables because the short-circuit check only tested `cfm.hasStaticColumns()` and not whether the table was a static-compact table. In a COMPACT STORAGE table the "static" columns from CQL's perspective are actually stored as regular columns internally; treating them as static columns caused the secondary index query path to build an incorrect result set. Adding an early-exit for `cfm.isStaticCompactTable()` prevents the misclassification.

### Before / After

```java
// BEFORE
public boolean containsStaticColumns()
{
    if (!cfm.hasStaticColumns())
        return false;
    ...
}
```

```java
// AFTER
public boolean containsStaticColumns()
{
    if (cfm.isStaticCompactTable() || !cfm.hasStaticColumns())
        return false;
    ...
}
```

---

## How to Spot

Whenever a method asks "does this table have static columns?", verify that COMPACT STORAGE tables are handled separately: in compact storage the schema representation of "static" columns differs from normal tables. Any code that calls `hasStaticColumns()` without also checking `isStaticCompactTable()` is a candidate for this class of misfiring guard.

---

## Test

New `testIndicesOnCompactTable` test creates COMPACT STORAGE tables with and without clustering columns, creates secondary indexes, inserts rows, and verifies that queries by indexed column return the correct rows.
