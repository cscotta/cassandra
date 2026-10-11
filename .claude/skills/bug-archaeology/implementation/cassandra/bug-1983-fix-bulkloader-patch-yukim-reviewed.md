# Bug 1983: BulkLoader used only CF name for validation, losing type metadata needed for SSTable open

**Commit**: d881024eef · **Ticket**: CASSANDRA-5542
**Subsystem**: tools/BulkLoader.java, io/sstable/SSTableLoader.java, streaming/ConnectionHandler.java, streaming/StreamResultFuture.java
**Category**: Logic error in condition

---

## Root Cause

`SSTableLoader.Client.validateColumnFamily()` returned only a boolean confirming that a column family existed, so `SSTableLoader` called `SSTableReader.open(desc, components, null, partitioner)` with a `null` CFMetaData argument. Opening an SSTable without its metadata omits type information needed for correct deserialization. Additionally, `BulkLoader.ExternalClient` fetched only the CF name from the schema table (`SELECT columnfamily_name`) rather than the full row, so it had no CFMetaData to supply even if the interface had supported it. The fix replaces the boolean `validateColumnFamily` interface with `getCFMetaData` that returns the full `CFMetaData` object (or null when absent), and changes the schema query to `SELECT *` so the full row can be deserialised via the new `CFMetaData.fromThriftCqlRow` helper.

### Before / After

```java
// BEFORE: boolean-only validation, null metadata passed to SSTableReader.open
if (!client.validateColumnFamily(keyspace, desc.cfname)) { ... return false; }
sstables.add(SSTableReader.open(desc, components, null, client.getPartitioner()));
```

```java
// AFTER: full CFMetaData returned and passed to open
CFMetaData metadata = client.getCFMetaData(keyspace, desc.cfname);
if (metadata == null) { ... return false; }
sstables.add(SSTableReader.open(desc, components, metadata, client.getPartitioner()));
```

```java
// BEFORE: schema query returned only name
String query = String.format("SELECT columnfamily_name FROM %s.%s WHERE keyspace_name = '%s'", ...);
for (CqlRow row : result.rows)
    knownCfs.add(new String(row.getColumns().get(0).getValue(), "UTF8"));
```

```java
// AFTER: full row fetched and deserialised
String query = String.format("SELECT * FROM %s.%s WHERE keyspace_name = '%s'", ...);
for (CqlRow row : result.rows) {
    CFMetaData metadata = CFMetaData.fromThriftCqlRow(row);
    knownCfs.put(metadata.cfName, metadata);
}
```

---

## How to Spot

When a validation method returns only a boolean, ask whether the validated entity's full object will be needed immediately after — if so, return the object directly (null-as-absent) to avoid a second lookup and to propagate metadata that a boolean cannot carry.

---

## Test

No test added.
