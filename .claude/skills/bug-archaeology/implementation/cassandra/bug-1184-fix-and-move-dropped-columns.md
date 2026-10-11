# Bug 1184: Dropped Column Name Missing from DroppedColumn Record, Breaks Collection-Type Validation and Comparator Reconstruction

**Commit**: 1b5fa8ce3c · **Ticket**: CASSANDRA-6717
**Subsystem**: src/java/org/apache/cassandra/schema/SchemaKeyspace.java, config/CFMetaData.java
**Category**: State not cleaned up

---

## Root Cause

When a column was dropped, `DroppedColumn` stored only the type and drop timestamp but not the column name. The dropped-columns metadata was also stored inline in the main table schema row rather than in a dedicated schema table. In 3.0 the composite comparator is reconstructed from live column definitions, so without the name there is no way to re-inject dropped collection columns into the `ColumnToCollectionType` comparator component — meaning reads of legacy SSTables that still contain data for the dropped collection would use the wrong comparator and produce corrupt results. Additionally, without a separate table the schema mutation diff path could not detect dropped-column changes individually.

### Before / After

```java
// BEFORE
public DroppedColumn(AbstractType<?> type, long droppedTime)
{
    this.type = type;
    this.droppedTime = droppedTime;
}
```

```java
// AFTER
public DroppedColumn(String name, AbstractType<?> type, long droppedTime)
{
    this.name = name;
    this.type = type;
    this.droppedTime = droppedTime;
}
```

---

## How to Spot

Any time a metadata record captures partial identity information (type and timestamp, but not the name of what was dropped), lookups that need the name will silently fail or be bypassed. Review every place a "record X was dropped" action is taken and ask whether the record stores enough fields to be self-contained — especially when a later pass must reconstruct derived structures (comparators, indexes) from the record alone.

---

## Test

`LegacySchemaMigratorTest` updated to verify dropped-column metadata round-trips correctly through the new dedicated `dropped_columns` schema table.
