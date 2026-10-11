# Bug 2209: BulkLoader Fails to Recognize CQL3 Column Families

**Commit**: a58b87020b · **Ticket**: CASSANDRA-4755
**Subsystem**: src/java/org/apache/cassandra/tools/BulkLoader.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`BulkLoader.ExternalClient` used `describe_keyspaces()` to discover all column family names, storing them in a `Map<String, Set<String>>` keyed by keyspace. CQL3 tables are stored with internal, composite-type column families that may not appear correctly via the Thrift `describe_keyspaces` response, causing `validateColumnFamily()` to return false and reject valid CQL3 SSTables. The fix replaces the Thrift-based discovery with a direct CQL3 query against the system schema table, storing only the names for the target keyspace in a flat `Set<String>`.

### Before / After

```java
// BEFORE
private final Map<String, Set<String>> knownCfs = new HashMap<String, Set<String>>();
// ...
List<KsDef> ksDefs = client.describe_keyspaces();
for (KsDef ksDef : ksDefs) {
    Set<String> cfs = new HashSet<String>();
    for (CfDef cfDef : ksDef.cf_defs)
        cfs.add(cfDef.name);
    knownCfs.put(ksDef.name, cfs);
}
// validateColumnFamily:
Set<String> cfs = knownCfs.get(keyspace);
return cfs != null && cfs.contains(cfName);
```

```java
// AFTER
private final Set<String> knownCfs = new HashSet<String>();
// ...
String query = String.format("SELECT columnfamily_name FROM %s.%s WHERE keyspace_name = '%s'",
                             Table.SYSTEM_KS, SystemTable.SCHEMA_COLUMNFAMILIES_CF, keyspace);
CqlResult result = client.execute_cql3_query(ByteBufferUtil.bytes(query), Compression.NONE, ConsistencyLevel.ONE);
for (CqlRow row : result.rows)
    knownCfs.add(new String(row.getColumns().get(0).getValue(), "UTF8"));
// validateColumnFamily:
return knownCfs.contains(cfName);
```

---

## How to Spot

When a tool that operates on SSTables discovers schema via Thrift rather than by querying the system schema tables directly, it may miss CQL3 tables whose metadata is stored in a different format. Prefer querying `system.schema_columnfamilies` for authoritative column family enumeration.

---

## Test

No test added.
