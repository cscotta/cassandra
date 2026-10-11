# Bug 1116: multiget_slice Returns Wrong Results for Static Columns via Thrift

**Commit**: 706ba8767e · **Ticket**: CASSANDRA-9906
**Subsystem**: src/java/org/apache/cassandra/thrift/CassandraServer.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The Thrift `multiget_slice` path translates legacy column names into CQL3 clusterings for the `ClusteringIndexNamesFilter`. Static columns in the legacy layout decode to `Clustering.STATIC_CLUSTERING`, a sentinel value that is not a real clustering row key. When static column names were included in the request, this sentinel was added to the `clusterings` set, producing an invalid filter. On the `ColumnFilter` side, for static-compact tables the compact value column was never included in the selection, so the value was silently dropped from the result.

### Before / After

```java
// BEFORE — static sentinel added to clusterings set, corrupts the filter
for (ByteBuffer bb : predicate.column_names) {
    LegacyLayout.LegacyCellName name = LegacyLayout.decodeCellName(...);
    clusterings.add(name.clustering);  // STATIC_CLUSTERING added unconditionally
}
return new ClusteringIndexNamesFilter(clusterings, false);
```

```java
// AFTER — static sentinel excluded; ColumnFilter handles static columns separately
for (ByteBuffer bb : predicate.column_names) {
    LegacyLayout.LegacyCellName name = LegacyLayout.decodeCellName(...);
    if (!name.clustering.equals(Clustering.STATIC_CLUSTERING))
        clusterings.add(name.clustering);
}
// static columns are handled via ColumnFilter, not clusterings
return new ClusteringIndexNamesFilter(clusterings, false);

// BEFORE (ColumnFilter path) — compact value column omitted for static-compact tables
PartitionColumns.Builder builder = new PartitionColumns.Builder();
...
return ColumnFilter.selection(builder.build());

// AFTER — compact value column added when needed
if (metadata.isStaticCompactTable())
    builder.add(metadata.compactValueColumn());
return ColumnFilter.selection(builder.build());
```

---

## How to Spot

When translating legacy Thrift column names into CQL3 filter objects, watch for sentinel or special-case values (such as `STATIC_CLUSTERING`) that must be handled separately from ordinary clusterings. Any loop that blindly maps every decoded name into a clustering set without checking for these sentinels will corrupt the filter. Similarly, when building a `ColumnFilter` from a predicate, verify that all column families (regular, static, compact-value) that the predicate can reference are covered by the selection.

---

## Test

No test added.
