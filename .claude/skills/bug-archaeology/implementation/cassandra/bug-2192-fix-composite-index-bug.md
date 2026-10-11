# Bug 2192: Composite Index Insert Skips Non-Primary Indexed Columns

**Commit**: c320b82be8 · **Ticket**: CASSANDRA-4884
**Subsystem**: src/java/org/apache/cassandra/db/index/SecondaryIndexManager.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When inserting data into a `ColumnFamily` that has composite secondary indexes, `SecondaryIndexManager.indexesCreated()` iterated over `indexesByColumn` by key (the indexed column name) and looked up that exact column in the mutation via `cf.getColumn(entry.getKey())`. For composite types, a single `PerColumnSecondaryIndex` may index multiple column names that share the same comparator prefix, so looking up only the registered key column missed all other columns that the index should have covered. The fix replaced the exact-key lookup with a scan of all columns in the `ColumnFamily`, calling `index.indexes(column.name())` to let each index decide which columns it owns.

### Before / After

```java
// BEFORE
for (Map.Entry<ByteBuffer, SecondaryIndex> entry : indexesByColumn.entrySet())
{
    SecondaryIndex index = entry.getValue();
    // ...
    IColumn column = cf.getColumn(entry.getKey());
    if (column == null)
        continue;
    ((PerColumnSecondaryIndex) index).insert(key, column);
}
```

```java
// AFTER
for (SecondaryIndex index : indexesByColumn.values())
{
    // ...
    for (IColumn column : cf)
    {
        if (index.indexes(column.name()))
            ((PerColumnSecondaryIndex) index).insert(key, column);
    }
}
```

---

## How to Spot

When a `PerColumnSecondaryIndex` is backed by a composite comparator, verify that the insertion path iterates over all mutation columns rather than performing a single point-lookup by the registered key. Any code that fetches `cf.getColumn(registeredKey)` for an index that can match multiple column names is suspect.

---

## Test

No test added.
