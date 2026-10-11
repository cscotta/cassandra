# Bug 1962: CqlPagingRecordReader and CqlRecordWriter fail silently on Thrift tables

**Commit**: 7a39421074 · **Ticket**: CASSANDRA-5752
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3/CqlPagingRecordReader.java, src/java/org/apache/cassandra/hadoop/cql3/CqlRecordWriter.java
**Category**: Silent skip on non-match

---

## Root Cause

Both `CqlPagingRecordReader` and `CqlRecordWriter` fetched partition key metadata by querying the `system.schema_columnfamilies` table and parsing a JSON list of key names. For classic Thrift-defined tables the JSON list is empty because such tables have no CQL-visible primary-key metadata. The code continued after receiving an empty list without populating `partitionBoundColumns` / `partitionKeyColumns`, causing subsequent operations to produce incorrect results or fail. The fix detects the empty-list case and falls back to `CFDefinition` (derived from the Thrift schema) to reconstruct the key columns.

### Before / After

```java
// BEFORE (CqlPagingRecordReader)
for (String key : keys)
    partitionBoundColumns.add(new BoundColumn(key));
// No check: if keys is empty the column list stays empty and processing continues incorrectly
keyString = ByteBufferUtil.string(...);
```

```java
// AFTER
for (String key : keys)
    partitionBoundColumns.add(new BoundColumn(key));
if (partitionBoundColumns.size() == 0) {
    retrieveKeysForThriftTables();
    return;
}
// retrieveKeysForThriftTables() uses describe_keyspace + CFDefinition
```

---

## How to Spot

When a Hadoop record reader or writer initialises schema metadata from a CQL system table, it should guard against the empty-result case and provide a fallback for legacy Thrift column families. Any code that silently continues after receiving an unexpectedly empty schema list risks producing wrong results downstream.

---

## Test

No test added.
