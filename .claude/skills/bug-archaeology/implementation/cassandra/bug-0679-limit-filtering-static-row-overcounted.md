# Bug 0679: LIMIT Queries with Clustering Column Filtering Return Fewer Rows Than Expected

**Commit**: b08843de67 · **Ticket**: CASSANDRA-11223
**Subsystem**: `src/java/org/apache/cassandra/db/filter/ColumnCounter.java`, `src/java/org/apache/cassandra/db/filter/SliceQueryFilter.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ColumnCounter.GroupByPrefix` counted the static row of a partition as a valid CQL row even when the query filtered on clustering columns and the partition had no matching regular rows. The static row was counted first; then when no regular rows passed the clustering filter, the counter had already consumed one slot of the LIMIT for that partition. This meant queries like `SELECT * FROM t WHERE b = 1 LIMIT 2 ALLOW FILTERING` returned one fewer result than expected because empty-matching partitions were silently "charged" against the limit. The fix introduces a `countPartitionsWithOnlyStaticData` flag: only slices that request the full column range (i.e., no clustering filter) should count static-only rows; filtered slices should not.

### Before / After

```java
// BEFORE – always increments live count, even for static rows from non-matching partitions
live++;
previous = current;
```

```java
// AFTER – only counts static rows when the flag permits it
if (!current.isStatic() || countPartitionsWithOnlyStaticData)
    live++;
previous = current;
```

---

## How to Spot

Row-counting logic that handles both static and regular rows must account for whether the query has a clustering column filter. A static row by itself should not consume a LIMIT slot if the query is filtering for specific clustering values and this partition has none that match. Reviewers should check `ColumnCounter` subclasses whenever a new filtering path is added to ensure the "counts static as a row" semantics is gated on the appropriate query shape.

---

## Test

`testFilteringOnClusteringColumnsWithLimitAndStaticColumns()` creates tables with static columns, inserts rows where some partitions have no matching clustering rows, then asserts that `WHERE b = 1 LIMIT 2 ALLOW FILTERING` returns exactly 2 results, testing with and without flush, with and without paging, and both single-partition and multi-partition form.
