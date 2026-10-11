# Bug 1911: NPE in Pig storage when reading thrift-created tables

**Commit**: 308a3e41e7 · **Ticket**: CASSANDRA-6072
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig/AbstractCassandraStorage.java
**Category**: Missing null / bounds check

---

## Root Cause

`AbstractCassandraStorage` queried `system.schema_columnfamilies` and selected both `key_aliases` and `key_alias` columns to determine partition-key names. For tables created through the CQL interface, `key_alias` is `null` because key metadata lives entirely in `key_aliases`. The code attempted `ByteBufferUtil.string(cqlRow.columns.get(6).value)` on the `key_alias` column without checking for null, causing an NPE. The `key_alias` column is a legacy artifact from the pre-CQL era; the fix removes it from the query entirely and derives the partition key solely from `key_aliases` (falling back to the `CFDefinition` for classic thrift tables where `key_aliases` is an empty JSON array).

### Before / After

```java
// BEFORE — queries legacy key_alias column, crashes when null for CQL tables
"       key_aliases," +
"       key_alias " +
...
String keyAlias = ByteBufferUtil.string(cqlRow.columns.get(6).value); // NPE when null
keys = new ArrayList<String>(1);
keys.add(keyAlias);
```

```java
// AFTER — only key_aliases queried; fallback via CFDefinition for thrift tables
"       key_aliases " +
...
String keyAliases = ByteBufferUtil.string(cqlRow.columns.get(5).value);
List<String> keys = FBUtilities.fromJsonList(keyAliases);
if (keys.size() == 0) { /* thrift fallback via CFDefinition */ }
else cql3Table = true;
```

---

## How to Spot

When building CQL queries that select nullable legacy columns in system tables, always check whether the column was introduced to support an older API that may be absent or null for schema objects created via the current API. Mixing `key_alias` (singular) with `key_aliases` (plural JSON array) in the same query is a sign of a transitional API surface that deserves explicit null handling.

---

## Test

No test added.
