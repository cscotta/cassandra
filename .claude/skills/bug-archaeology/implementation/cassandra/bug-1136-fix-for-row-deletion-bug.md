# Bug 1136: Row Deletion Not Propagated to Materialized Views

**Commit**: 2fcfc7c58a · **Ticket**: CASSANDRA-10014
**Subsystem**: src/java/org/apache/cassandra/db/view/MaterializedView.java
**Category**: Silent skip on non-match

---

## Root Cause

`MaterializedView.updateIsNeeded()` iterated over the rows of a partition mutation to determine whether the view needed updating, but only inspected column data — it never checked whether the row itself carried a deletion. As a result, row-level tombstones (both clustering-key deletes and complex-column deletions) were silently ignored and never reflected in the materialized view. The check for a partition-level `deletionInfo` existed, but the per-row deletion path was entirely absent.

### Before / After

```java
// BEFORE
for (Row row : partition)
{
    for (ColumnData data : row)
    {
        if (getViewCfs().metadata.getColumnDefinition(data.column().name) != null)
```

```java
// AFTER
for (Row row : partition)
{
    if (row.hasComplexDeletion())
        return true;
    if (!row.deletion().isLive())
        return true;

    for (ColumnData data : row)
    {
        if (getViewCfs().metadata.getColumnDefinition(data.column().name) != null)
```

---

## How to Spot

When a method scans a composite object (partition → rows → columns) to decide whether an update is needed, look for missing checks at each nesting level. Here the outermost level (partition tombstone) and the innermost level (column data) were handled, but the middle level (row tombstone) was not. Any code path that short-circuits "no update needed" without exhaustively considering all tombstone granularities in Cassandra's data model is a candidate for this class of bug.

---

## Test

Two new unit tests in `MaterializedViewTest`: `testPartitionTombstone` (DELETE on a partition-key table) and `testClusteringKeyTombstone` (DELETE on a specific clustering row), both verifying the view is emptied after the delete.
