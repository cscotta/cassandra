# Bug 0703: SSTableLoader Fails When Loaded Table Has Dropped Columns

**Commit**: b687641f78 · **Ticket**: CASSANDRA-13276
**Subsystem**: src/java/org/apache/cassandra/utils/NativeSSTableLoaderClient.java
**Category**: Missing null / bounds check

---

## Root Cause

`NativeSSTableLoaderClient.tableMetadataFromCql()` reconstructed table metadata by querying the `system_schema.columns` table for live columns, but never queried `system_schema.dropped_columns`. When an SSTable being loaded contained cells for columns that had since been dropped from the table schema, the metadata object had no knowledge of those dropped columns. Downstream deserialization code expected dropped-column metadata to be present and threw an exception when it was absent, aborting the bulk load entirely.

### Before / After

```java
// BEFORE — only live columns loaded, dropped columns ignored
for (Row colRow : session.execute(columnsQuery, keyspace, name))
    builder.addColumn(createDefinitionFromRow(colRow, keyspace, name, types));

return TableMetadataRef.forOfflineTools(builder.build());
```

```java
// AFTER — dropped columns also queried and added to metadata
for (Row colRow : session.execute(columnsQuery, keyspace, name))
    builder.addColumn(createDefinitionFromRow(colRow, keyspace, name, types));

Map<ByteBuffer, DroppedColumn> droppedColumns = new HashMap<>();
for (Row colRow : session.execute(droppedColumnsQuery, keyspace, name))
{
    DroppedColumn dc = createDroppedColumnFromRow(colRow, keyspace, name);
    droppedColumns.put(dc.column.name.bytes, dc);
}
builder.droppedColumns(droppedColumns);
return TableMetadataRef.forOfflineTools(builder.build());
```

---

## How to Spot

Any code that reconstructs table schema metadata for offline tools must replicate all parts of the schema that the live server consults, including dropped columns. A reviewer examining schema-reconstruction code should check that both `system_schema.columns` and `system_schema.dropped_columns` (and analogously any other schema sub-tables) are queried. Omitting dropped columns is a silent gap that only manifests when an SSTable actually contains cells for those columns.

---

## Test

No test added in this commit.
