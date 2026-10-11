# Bug 0704: SSTableLoader Fails When Table Has Dropped Columns (3.x)

**Commit**: 42904c6538 · **Ticket**: CASSANDRA-13276
**Subsystem**: SSTableLoader / Schema
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`NativeSSTableLoaderClient.tableMetadataFromRow()` built a `CFMetaData` object from the live columns in `system_schema.columns` but never consulted `system_schema.dropped_columns`. When the SSTable being loaded contained cells for a column that had since been dropped, the loader had no `DroppedColumn` entry for it, so deserialization could not map the on-disk bytes to a type, causing an exception. The fix adds a second query against the `dropped_columns` table and calls `metadata.droppedColumns(map)` before returning the metadata object.

### Before / After
```java
// BEFORE
return CFMetaData.create(keyspace, name, id, isDense, isCompound,
                         isSuper, isCounter, isView, defs, partitioner);

// AFTER
CFMetaData metadata = CFMetaData.create(keyspace, name, id, isDense,
                                        isCompound, isSuper, isCounter,
                                        isView, defs, partitioner);

String droppedColumnsQuery = String.format(
    "SELECT * FROM %s.%s WHERE keyspace_name = ? AND table_name = ?",
    SchemaConstants.SCHEMA_KEYSPACE_NAME, SchemaKeyspace.DROPPED_COLUMNS);
Map<ByteBuffer, CFMetaData.DroppedColumn> droppedColumns = new HashMap<>();
for (Row colRow : session.execute(droppedColumnsQuery, keyspace, name))
{
    CFMetaData.DroppedColumn droppedColumn = createDroppedColumnFromRow(colRow, keyspace);
    droppedColumns.put(UTF8Type.instance.decompose(droppedColumn.name), droppedColumn);
}
metadata.droppedColumns(droppedColumns);
return metadata;
```

---

## How to Spot
When `NativeSSTableLoaderClient` (or any tool that reconstructs `CFMetaData` from the native driver) builds table metadata, look for whether it queries both `system_schema.columns` and `system_schema.dropped_columns`. If `droppedColumns()` is never called on the constructed metadata, any SSTable that was written before a column was dropped will fail to load because the deserializer cannot resolve the obsolete column's type.

---

## Test
No test added in this commit. The same fix is tested in the companion 3.0 backport (CASSANDRA-13276).
