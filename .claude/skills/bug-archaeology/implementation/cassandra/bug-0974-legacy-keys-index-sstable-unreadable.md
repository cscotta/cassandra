# Bug 0974: Legacy KEYS index SSTables unreadable due to wrong table format metadata

**Commit**: 5ec40a3595 · **Ticket**: CASSANDRA-11045
**Subsystem**: Secondary indexes / legacy compatibility
**Category**: Wrong serialization / deserialization

---

## Root Cause
`CassandraIndex.indexCfsMetadata()` built the `CFMetaData` for an index's backing column family using the same builder path regardless of the index type. Legacy KEYS indexes were written on disk as non-compound, dense (compact) tables, but the metadata builder always created a compound, non-dense table. When a 3.0 node tried to open these SSTables, the mismatch between the on-disk format and the constructed metadata caused deserialization failures.

Additionally, a dense compact table requires a compact-value column to be defined even if unused; without it, the metadata was structurally invalid.

The fix branches on `indexMetadata.isKeys()`: for KEYS indexes it creates a compact, non-compound, dense builder and adds a placeholder compact-value column (`EmptyType`); for COMPOSITES indexes it uses the existing regular (non-compact) builder and adds the appropriate clustering columns.

### Before / After
```java
// BEFORE
CFMetaData.Builder builder = CFMetaData.Builder.create(ksName, indexCFName)
    .withId(...)
    .withPartitioner(new LocalPartitioner(indexedValueType))
    .addPartitionKey(indexedColumn.name, indexedColumn.type);
builder.addClusteringColumn("partition_key", ...);
builder = utils.addIndexClusteringColumns(builder, baseCfsMetadata, indexedColumn);

// AFTER
CFMetaData.Builder builder = indexMetadata.isKeys()
    ? CFMetaData.Builder.create(ksName, indexCFName, true, false, false)  // compact/dense
    : CFMetaData.Builder.create(ksName, indexCFName);                      // regular

builder = builder.withId(...).withPartitioner(...).addPartitionKey(...).addClusteringColumn(...);

if (indexMetadata.isKeys()) {
    // compact value column required for dense tables
    builder = builder.addRegularColumn(names.defaultCompactValueName(), EmptyType.instance);
} else {
    builder = utils.addIndexClusteringColumns(builder, baseCfsMetadata, indexedColumn);
}
```

---

## How to Spot
When constructing metadata for legacy SSTable formats, the compactness and density flags must match what was written on disk. Any code that builds `CFMetaData` for index tables without branching on legacy vs. modern index kinds is suspect. Watch for a single `CFMetaData.Builder.create(ks, name)` that is supposed to cover both KEYS and COMPOSITES indexes — those two have incompatible on-disk formats and require separate builder configurations.

---

## Test
No test added.
