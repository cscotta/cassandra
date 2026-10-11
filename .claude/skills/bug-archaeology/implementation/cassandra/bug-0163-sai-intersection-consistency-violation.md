# Bug 0163: SAI Intersection Queries Return Inconsistent Results on Partially Updated Rows at Reconciling CL

**Commit**: f7984627e7 · **Ticket**: CASSANDRA-19018
**Subsystem**: src/java/org/apache/cassandra/index/sai/plan, src/java/org/apache/cassandra/service/reads
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Storage-Attached Index (SAI) intersection queries (`WHERE col_a = x AND col_b = y`) build a `KeyRangeIntersectionIterator` across all per-column index results and return only keys that appear in every set. When a row has been partially updated—one column written but not yet the other—only one of the per-column indexes contains the key. Under consistency levels that require reconciliation across replicas (e.g. QUORUM), the coordinator merges partial replica responses, so such a row can become visible after merging even though it should be filtered. Because every index searched all SSTables together (repaired and unrepaired alike), the intersection silently dropped keys that existed only in the unrepaired/in-flight indexes, and Replica Filtering Protection (RFP) did not account for the extra short-reads introduced by the coordinator's post-merge filtering step.

The fix introduces three coordinated changes: (1) `ReadCommand` now serializes a `NEEDS_RECONCILIATION` flag so replicas know the user query requires reconciliation; (2) when reconciliation is required and multiple mutable columns are restricted, the local SAI query degrades intersections to unions on *unrepaired* SSTables (only repaired SSTable indexes are intersected), so partial updates are never dropped; (3) RFP correctly counts short-reads that arise from coordinator-side filtering of merged rows.

### Before / After

```java
// BEFORE – all SSTable indexes (repaired + unrepaired) fed into a single intersection
KeyRangeIterator.Builder builder = KeyRangeIntersectionIterator.builder(expressions.size());
for (Pair<Expression, Collection<SSTableIndex>> pair : queryView.view)
    builder.add(IndexSearchResultIterator.build(pair.left, pair.right, mergeRange, queryContext));
```

```java
// AFTER – unrepaired indexes go into a top-level union; only repaired indexes are intersected
KeyRangeIterator.Builder builder = command.rowFilter().isStrict()
    ? KeyRangeIntersectionIterator.builder(expressions.size())
    : KeyRangeUnionIterator.builder(expressions.size());
// ... repaired subset fed into repairedBuilder (intersection), unrepairedIterator added to outer union
```

---

## How to Spot

Index intersection logic that does not distinguish repaired from unrepaired data can silently drop rows that exist in only one column's index because of in-flight partial writes. During review, any multi-column index intersection should be reviewed for whether the caller's consistency level permits the replica to apply strict local filtering; if reconciliation is required, intersections must be widened (or degraded to unions) over data that has not yet been fully repaired, and any RFP short-read accounting must include rows that the coordinator itself discards after merging.

---

## Test

`PartialUpdateHandlingTest` writes rows with partial column updates to multiple replicas and executes intersection queries at various consistency levels, asserting that all expected rows are returned regardless of which columns have been written to which replica.
