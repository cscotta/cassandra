# Bug 1431: Fix CREATE TABLE for CQL2

**Commit**: 94fea8df62 · **Ticket**: CASSANDRA-8144
**Subsystem**: src/java/org/apache/cassandra/cql/CreateColumnFamilyStatement.java
**Category**: Wrong constant / default value

---

## Root Cause

The CQL2 `CreateColumnFamilyStatement` unconditionally constructed a `SimpleDenseCellNameType` for every new column family, regardless of whether the table actually had explicitly declared columns. In Cassandra's storage model, a table with no extra column definitions is a dense table (CQL1-style "wide row"), while one with column definitions is sparse. By always choosing the dense cell name type, tables created via CQL2 with explicit column definitions were assigned the wrong internal representation, causing incorrect behavior at read time. The fix infers `isDense` from whether any columns were declared, then uses `CellNames.fromAbstractType(comparator, isDense)` and calls `newCFMD.isDense(isDense)` so both the cell name type and the metadata flag are set consistently.

### Before / After

```java
// BEFORE – always dense, regardless of column definitions
CFMetaData newCFMD = new CFMetaData(keyspace, name,
    ColumnFamilyType.Standard,
    new SimpleDenseCellNameType(comparator));
```

```java
// AFTER – infer density from presence of column definitions
boolean isDense = columns.isEmpty();
CFMetaData newCFMD = new CFMetaData(keyspace, name,
    ColumnFamilyType.Standard,
    CellNames.fromAbstractType(cfProps.getComparator(), isDense));
newCFMD.isDense(isDense)
       .addAllColumnDefinitions(getColumns(newCFMD))
       ...
       .rebuild();
```

---

## How to Spot

When constructing a schema object that has a binary "mode" flag (dense vs. sparse, compact vs. non-compact, etc.), look for hardcoded type constructor calls that ignore the surrounding context. The heuristic is: if the type or mode is chosen before the relevant inputs (column list, options) have been inspected, the choice is likely wrong for at least one code path. Always derive structural flags from the actual schema inputs rather than assuming a default.

---

## Test

No test added.
