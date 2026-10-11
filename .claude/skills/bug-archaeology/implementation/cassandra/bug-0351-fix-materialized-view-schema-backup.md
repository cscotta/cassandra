# Bug 0351: Materialized View Schema Snapshot Written as Base Table DDL Instead of CREATE MATERIALIZED VIEW

**Commit**: 67eb22ec9d · **Ticket**: CASSANDRA-12734
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStoreCQLHelper.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ColumnFamilyStoreCQLHelper.getCFMetadataAsCQL` always emitted `CREATE TABLE IF NOT EXISTS` DDL regardless of whether the `CFMetaData` described a materialized view. Because a view's internal storage representation shares the same `CFMetaData` structure as a base table, the schema backup (written on drop when `auto_snapshot` is enabled) produced a syntactically valid but semantically wrong `CREATE TABLE` statement. Restoring from such a backup would create a regular table with the view's physical layout rather than recreating the actual materialized view. Additionally, the compaction interruption and snapshot steps ran after the keyspace metadata had already been updated to remove the view, causing the snapshot to potentially miss in-flight compaction data. The fix adds a `getViewMetadataAsCQL` path that emits proper `CREATE MATERIALIZED VIEW` DDL and reorders the drop sequence to interrupt compaction and snapshot before removing the view from the schema.

### Before / After

```java
// BEFORE (always emits CREATE TABLE)
sb.append("CREATE TABLE IF NOT EXISTS ");
sb.append(quoteIdentifier(metadata.ksName)).append('.').append(quoteIdentifier(metadata.cfName)).append(" (");
// ...
```

```java
// AFTER
if (metadata.isView())
{
    sb.append(getViewMetadataAsCQL(metadata, includeDroppedColumns));
}
else
{
    sb.append(getBaseTableMetadataAsCQL(metadata, includeDroppedColumns));
}
```

---

## How to Spot

Any schema-serialization path that iterates all `ColumnFamilyStore` instances without branching on `metadata.isView()` will silently produce wrong DDL for materialized views, since views share the `CFMetaData` type with base tables. Review all places that call `getCFMetadataAsCQL` or equivalent and confirm they handle the view case explicitly.

---

## Test

Adds `ViewSchemaTest.testViewMetadataCQLIncludeAllColumn` and `testViewMetadataCQLNotIncludeAllColumn`: create a base table and view with various column selections, then assert that `getCFMetadataAsCQL` produces a string starting with the expected `CREATE MATERIALIZED VIEW IF NOT EXISTS` DDL including correct `PRIMARY KEY` and `CLUSTERING ORDER BY` clauses.
