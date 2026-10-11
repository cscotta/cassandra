# Bug 0705: SSTableLoader Fails When Table Has Dropped Columns (3.0)

**Commit**: 175e4f8ce8 · **Ticket**: CASSANDRA-13276
**Subsystem**: SSTableLoader / Schema
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
This is the 3.0 backport of CASSANDRA-13276. `NativeSSTableLoaderClient.tableMetadataFromRow()` constructed `CFMetaData` solely from `system_schema.columns`, omitting a query to `system_schema.dropped_columns`. SSTables containing cells for columns that were subsequently dropped could not be deserialized because no `DroppedColumn` type mapping existed in the reconstructed metadata. The fix mirrors the 3.x fix: query `dropped_columns`, build a map keyed by UTF-8-decomposed column name, and attach it via `metadata.droppedColumns(map)`.

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
    CFMetaData.DroppedColumn dc = createDroppedColumnFromRow(colRow, keyspace);
    droppedColumns.put(UTF8Type.instance.decompose(dc.name), dc);
}
metadata.droppedColumns(droppedColumns);
return metadata;
```

---

## How to Spot
Any code path that re-hydrates `CFMetaData` from the native driver without joining against `dropped_columns` will silently omit drop history. During review, check that bulk-load clients (SSTableLoader, sstableimport, stress tools) make this secondary schema query and populate the dropped column map before using the metadata to open SSTables.

---

## Test
No test added. Integration coverage is expected from the SSTableLoader integration tests that exercise tables with prior `ALTER TABLE DROP` statements.
