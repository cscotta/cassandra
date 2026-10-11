# Bug 0841: SASI index rebuild overwrites existing index data incorrectly

**Commit**: fa11316795 · **Ticket**: CASSANDRA-12374
**Subsystem**: Secondary Indexes / SASI
**Category**: State not cleaned up

---

## Root Cause
When `nodetool rebuild_index` is called for SASI indexes, `SASIIndex.getIndexBuildTask` filtered out SSTables already tracked by the index (`!sasi.index.hasSSTable`) before adding them to the rebuild list. This was correct but incomplete: the old on-disk index data for the SSTables being rebuilt was never removed from `DataTracker` before the new build started. The result was stale index entries surviving after rebuild. A second problem in `SASIIndexBuilder` unconditionally called `indexWriter.nextUnfilteredCluster(partition.staticRow())` even for tables without static columns, causing `ClassCastException` or corrupted index entries on tables that have no static schema.

### Before / After
```java
// BEFORE (SASIIndex.getIndexBuildTask)
indexes.stream()
    .filter(i -> i instanceof SASIIndex)
    .forEach(i -> {
        SASIIndex sasi = (SASIIndex) i;
        // stale data NOT dropped before rebuild
        sstablesToRebuild.stream()
            .filter(s -> !sasi.index.hasSSTable(s))
            .forEach(s -> { ... });
    });

// AFTER
    .forEach(i -> {
        SASIIndex sasi = (SASIIndex) i;
        sasi.index.dropData(sstablesToRebuild);   // remove stale entries first
        sstablesToRebuild.stream()
            .filter(s -> !sasi.index.hasSSTable(s))
            .forEach(s -> { ... });
    });

// BEFORE (SASIIndexBuilder)
indexWriter.nextUnfilteredCluster(partition.staticRow());

// AFTER
if (cfs.metadata.hasStaticColumns())
    indexWriter.nextUnfilteredCluster(partition.staticRow());
```

---

## How to Spot
Index rebuild paths should always drop/invalidate old state before repopulating, not just check presence. In `DataTracker`, look for a `dropData` overload that accepts a `Collection<SSTableReader>` — if it is missing, a targeted rebuild cannot clear the targeted subset. Also guard every static-row access behind a `hasStaticColumns()` check; calling `partition.staticRow()` on a table without statics produces an empty-but-non-null row that still gets indexed, inflating the index unnecessarily.

---

## Test
`SASIIndexTest.testIndexRebuild` was added to verify that rebuilding an index on an existing SSTable produces the correct results and does not duplicate or retain stale entries.
