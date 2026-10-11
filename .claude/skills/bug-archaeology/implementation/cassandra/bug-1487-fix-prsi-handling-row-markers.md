# Bug 1487: Fix PRSI Handling of CQL3 Row Markers for Row Clearing

**Commit**: 7554eb5316 · **Ticket**: CASSANDRA-7787
**Subsystem**: db/index / SecondaryIndexManager
**Category**: Missing null / bounds check

---

## Root Cause

The row-cleanup path in `SecondaryIndexManager` iterated over `indexedColumnsInRow` and looked up the column definition via `baseCfs.metadata.getColumnDefinitionFromColumnName(column.name())`. CQL3 tables store an internal row-marker column with an empty byte-buffer name; this marker has no corresponding `ColumnDefinition`. `getColumnDefinitionFromColumnName` returned `null` for it, and the subsequent `indexesByColumn.get(cDef.name)` threw a `NullPointerException`. The fix replaces the lookup with a call to `indexFor(column.name())`, which already handles unknown column names gracefully by returning an empty set and never touches a null `ColumnDefinition`.

### Before / After

```java
// BEFORE
ColumnDefinition cDef = baseCfs.metadata.getColumnDefinitionFromColumnName(column.name());
SecondaryIndex index = indexesByColumn.get(cDef.name);  // NPE when cDef == null (row marker)
if (index == null)
    continue;
// ... dispatch on index type
```

```java
// AFTER
for (SecondaryIndex index : indexFor(column.name()))  // returns empty set for unknown names
{
    if (index instanceof PerRowSecondaryIndex)
    {
        if (cleanedRowLevelIndexes == null)
            cleanedRowLevelIndexes = new HashSet<>();
        if (cleanedRowLevelIndexes.add(index.getClass()))
            ((PerRowSecondaryIndex) index).delete(key);
    }
    else
    {
        ((PerColumnSecondaryIndex) index).delete(key.key, column);
    }
}
```

---

## How to Spot

When iterating over columns in a CQL3-backed `ColumnFamily`, always account for the synthetic empty-name row-marker column. Any code that maps column names to `ColumnDefinition` objects without a null guard is a candidate for this class of bug.

---

## Test

No test added in this commit.
