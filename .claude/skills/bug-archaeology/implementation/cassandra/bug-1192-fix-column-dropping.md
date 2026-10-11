# Bug 1192: Dropped-Column Map Keyed by ColumnIdentifier Instead of ByteBuffer Causes Silent Lookup Misses

**Commit**: 4c12f1961b · **Ticket**: CASSANDRA-9781
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java, schema/SchemaKeyspace.java
**Category**: Wrong constant / default value

---

## Root Cause

`CFMetaData.droppedColumns` was declared as `Map<ColumnIdentifier, DroppedColumn>`. `ColumnIdentifier` has two interning modes and its `equals`/`hashCode` can differ from `ByteBuffer` equality, so callers that held only the raw `ByteBuffer` form of a column name (e.g., during SSTable reading in `UnfilteredPartitionIterators.removeDroppedColumns`) used `column.name.bytes` as the key, which never matched a `ColumnIdentifier` key even for the same logical column. The result was that dropped-column filtering was silently skipped: cells belonging to dropped columns were returned to the client instead of being suppressed.

### Before / After

```java
// BEFORE
private volatile Map<ColumnIdentifier, DroppedColumn> droppedColumns = new HashMap<>();

public void recordColumnDrop(ColumnDefinition def)
{
    droppedColumns.put(def.name, new DroppedColumn(def.type, FBUtilities.timestampMicros()));
}
```

```java
// AFTER
private volatile Map<ByteBuffer, DroppedColumn> droppedColumns = new HashMap<>();

public void recordColumnDrop(ColumnDefinition def)
{
    droppedColumns.put(def.name.bytes, new DroppedColumn(def.type, FBUtilities.timestampMicros()));
}
```

---

## How to Spot

When a map's key type is a wrapper around `ByteBuffer` (e.g., `ColumnIdentifier`, `DecoratedKey`), ensure every call site inserts and looks up using the same key type. Mixed-key lookups (inserting a `ColumnIdentifier` but querying with a `ByteBuffer`) always return null without any error, making the map appear empty. Prefer `ByteBuffer` as the canonical map key when the map must be queried from multiple contexts that may not all have access to the wrapper type.

---

## Test

`LegacySchemaMigratorTest` updated to exercise the corrected `ByteBuffer`-keyed lookup path through the schema migration layer.
