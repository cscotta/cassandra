# Bug 1036: Materialized View Inserts Empty Rows Causing Memtable Flush AssertionError

**Commit**: 2998bb3cc4 · **Ticket**: CASSANDRA-10614
**Subsystem**: Materialized Views
**Category**: Logic error in condition

---

## Root Cause
When a `PartitionUpdate` contained multiple rows but only some matched the MV's `WHERE` filter, `updateAffectsView()` correctly detected that at least one row was relevant and admitted the entire partition for processing. However, `createTombstonesForRow` (called for every row in the partition) did not re-check per-row filter applicability, and `createMutationForRow` could build a `Row` with no live cells when processing a non-matching row. Passing an empty `Row` to `PartitionUpdate.singleRowUpdate` then caused an `AssertionError` during memtable flush because the storage engine forbids empty rows.

A secondary problem was that `viewHasAllPrimaryKeys` was used as a bypass flag in `updateAppliesToView`, incorrectly allowing rows through the relevance check even when they were not live. The flag was renamed to `viewPKIncludesOnlyBasePKColumns` and its use in `updateAppliesToView` was removed to require a proper per-row liveness check every time.

### Before / After
```java
// BEFORE – admitting any row if viewHasAllPrimaryKeys, skipping liveness check
if (includeAllColumns || viewHasAllPrimaryKeys || !row.deletion().isLive())
    return true;

// AFTER – always require the row to pass the liveness check
if (includeAllColumns || !row.deletion().isLive())
    return true;

// BEFORE – no per-row clustering filter when partition has multiple rows
for (TemporalRow temporalRow : rowSet) { ... }

// AFTER – skip rows whose clustering does not match the view select query
if (partition.rowCount() != 1 && !selectQuery.selectsClustering(partition.partitionKey(), temporalRow.baseClustering()))
    continue;

// AFTER – guard against empty rows produced for non-matching rows
Row row = regularBuilder.build();
if (row.isEmpty())
    return null;
return PartitionUpdate.singleRowUpdate(viewCfm, partitionKey, row);
```

---

## How to Spot
Any MV mutation-building path that iterates all rows in a partition after a partition-level filter pass is suspect. In review, verify that every individual row is re-checked against the MV's clustering/column predicates before being passed to `PartitionUpdate.singleRowUpdate`, and that the result of `Row.build()` is tested for emptiness before use. The pattern of a coarse admission check followed by fine-grained processing without per-item re-validation is a common source of this class of bug.

---

## Test
`ViewTest.testIgnoreUpdate` and an extension to `ViewFilteringTest` cover batches that mix matching and non-matching rows in the same partition, and explicitly flush the MV memtable to confirm no `AssertionError` is raised.
