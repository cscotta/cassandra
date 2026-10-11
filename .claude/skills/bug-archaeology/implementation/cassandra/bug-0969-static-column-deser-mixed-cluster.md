# Bug 0969: Wrong deserialization of static columns in mixed-cluster read commands

**Commit**: b21df5b70a · **Ticket**: CASSANDRA-11087
**Subsystem**: Storage engine / mixed-version cluster compatibility
**Category**: Wrong serialization / deserialization

---

## Root Cause
When a 3.0 node deserialized a legacy (pre-3.0) name-filter read command that contained a static column name, it skipped adding the clustering instead of converting it into the correct form for the 3.0 data model. In pre-3.0 compact tables, static columns written through Thrift were stored as dynamic (regular) clustered cells, not as true static cells. The deserializer checked `if (!cellName.clustering.equals(Clustering.STATIC_CLUSTERING))` and silently dropped static cells, so static column data could not be read by a 3.0 node in a mixed cluster.

The fix detects the case where the table is a static compact table and the deserialized cell resolves to `STATIC_CLUSTERING`: in that case, the actual column name bytes are used as the clustering value and the compact value column is requested, replicating the pre-3.0 storage layout correctly.

### Before / After
```java
// BEFORE
if (!cellName.clustering.equals(Clustering.STATIC_CLUSTERING))
    clusterings.add(cellName.clustering);
selectionBuilder.add(cellName.column);

// AFTER
if (metadata.isStaticCompactTable() && cellName.clustering.equals(Clustering.STATIC_CLUSTERING))
{
    clusterings.add(new Clustering(cellName.column.name.bytes));
    selectionBuilder.add(metadata.compactValueColumn());
}
else
{
    clusterings.add(cellName.clustering);
}
selectionBuilder.add(cellName.column);
```

---

## How to Spot
Name-filter deserialization code in mixed-version clusters needs special handling for static columns in compact tables. Whenever a condition silently skips adding a clustering (`if (!eq) add`) without any else branch, consider whether the skipped case actually needs different treatment rather than being ignored — especially when legacy formats are involved. The absence of an `else` clause processing the skipped branch is the code smell.

---

## Test
No test added.
