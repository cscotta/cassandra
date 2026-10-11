# Bug 1761: SecondaryIndexManager#deleteFromIndexes() Looks Up Index by Wrong Key

**Commit**: 7937ee38f5 · **Ticket**: CASSANDRA-6711
**Subsystem**: db/index/SecondaryIndexManager
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

In `deleteFromIndexes()`, the `indexesByColumn` map is keyed by `ColumnIdentifier` (the logical column name), but the code was looking up the index using `column.name()` which returns the raw `ByteBuffer` encoding of the column name. Because the map key type and the lookup type did not match, `indexesByColumn.get(column.name())` always returned `null`, causing all secondary index entries to be silently skipped during delete operations — rows were never removed from secondary indexes on delete.

### Before / After

```java
// BEFORE
SecondaryIndex index = indexesByColumn.get(column.name()); // ByteBuffer key, map uses ColumnIdentifier
```

```java
// AFTER
ColumnDefinition cDef = baseCfs.metadata.getColumnDefinitionFromColumnName(column.name());
SecondaryIndex index = indexesByColumn.get(cDef.name); // ColumnIdentifier key matches map
```

---

## How to Spot

When a map lookup consistently returns null and callers silently skip via `if (index == null) continue`, verify that the key type used in `get()` exactly matches the key type used in `put()`. Type-unsafe maps (e.g., `Map<ColumnIdentifier, …>` accessed with a raw `ByteBuffer`) will silently miss.

---

## Test

No test added.
