# Bug 1050: Thrift get() Returns Wrong Result for Static Columns

**Commit**: 4d00468cc3 · **Ticket**: CASSANDRA-10586
**Subsystem**: Thrift / Read Path
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
The Thrift `get()` RPC translates a `column_path` into a `SinglePartitionReadCommand`. When the decoded cell name resolved to `Clustering.STATIC_CLUSTERING` (i.e. a column that is stored as a static column in the new storage format), the code fell through to the regular-column branch and constructed a `ClusteringIndexNamesFilter` using `cellname.clustering` directly. For static columns that clustering is `STATIC_CLUSTERING`, which is not a real data clustering, so the read returned no rows. In addition, only the static column itself was selected in the `ColumnFilter`, omitting the compact-value column that `ThriftResultsMerger` needs to synthesise the correct Thrift-style result.

The fix detects the static case, selects both the named column and `metadata.compactValueColumn()`, and builds the filter using the column name bytes as an actual clustering (matching the equivalent dynamic-column path), consistent with the way the broader `multiget_slice` path already handled this.

### Before / After
```java
// BEFORE – static column decoded as STATIC_CLUSTERING, used verbatim → no rows returned
columns = ColumnFilter.selection(PartitionColumns.of(cellname.column));
filter = new ClusteringIndexNamesFilter(
    FBUtilities.singleton(cellname.clustering, metadata.comparator), false);

// AFTER – static case branched out; filter uses column bytes as real clustering
if (cellname.clustering == Clustering.STATIC_CLUSTERING)
{
    ColumnFilter.Builder builder = ColumnFilter.selectionBuilder();
    builder.add(cellname.column);
    builder.add(metadata.compactValueColumn());
    columns = builder.build();
    filter = new ClusteringIndexNamesFilter(
        FBUtilities.singleton(new Clustering(column_path.column), metadata.comparator), false);
}
else
{
    columns = ColumnFilter.selection(PartitionColumns.of(cellname.column));
    filter = new ClusteringIndexNamesFilter(
        FBUtilities.singleton(cellname.clustering, metadata.comparator), false);
}
```

---

## How to Spot
Whenever Thrift column-path decoding feeds into a `ClusteringIndexNamesFilter`, check that `Clustering.STATIC_CLUSTERING` is explicitly handled before the generic path. Static cells do not have a meaningful clustering in the new storage model, so routing them through the regular clustering filter silently returns an empty result instead of an error. Also verify that `ThriftResultsMerger`-aware code always includes `compactValueColumn()` in the column selection for static reads.

---

## Test
No test file was added in this commit.
