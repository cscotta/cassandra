# Bug 0373: Queries on Empty Partitions with Static Data Return No Rows

**Commit**: 2059c7586e · **Ticket**: CASSANDRA-16686
**Subsystem**: `src/java/org/apache/cassandra/db/filter/ColumnFilter.java`, `cql3/selection/`, `cql3/statements/SelectStatement.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Since CASSANDRA-12768, the `ColumnFilter` for a `SELECT` statement fetches all regular columns but only the *user-selected* static columns rather than all static columns. For a full-partition query (no clustering or regular column restrictions), CQL semantics require returning a row for any partition that contains static data even if it has no regular rows. The bug was that when a user selected only a subset of static columns (e.g. `SELECT s1 FROM t WHERE pk=1`) and the partition's value for `s1` had been written to an earlier SSTable while a different static column `s2` lived in a newer one, short-read protection would examine only the SSTables holding `s1`, find no data, and declare the partition empty — silently dropping the row that should have been returned. The fix propagates a `returnStaticContentOnPartitionWithNoRows` flag from `StatementRestrictions` through `Selection` and `ColumnFilterFactory` down to `ColumnFilter.Builder`, which then includes *all* static columns in the fetched set for full-partition queries.

### Before / After

```java
// BEFORE – ColumnFilter.allRegularColumnsBuilder only fetches regular cols +
//           user-selected statics; partition-level queries may miss static data
ColumnFilter.Builder builder = ColumnFilter.allRegularColumnsBuilder(table);
builder.addAll(selectedColumns);
```

```java
// AFTER – full-partition queries also fetch all static columns so short-read
//          protection cannot incorrectly declare the partition empty
ColumnFilter.Builder builder = ColumnFilter.allRegularColumnsBuilder(table,
                                    returnStaticContentOnPartitionWithNoRows);
builder.addAll(selectedColumns);
```

---

## How to Spot

Wherever a query engine filters the set of columns to fetch as an optimization (e.g. "only fetch queried columns"), review what happens when the result depends on columns that were *not* selected by the user but are semantically required to determine row existence. Partition-level existence checks in wide-row stores (static columns, liveness info) are especially prone to this class of bug because the existence signal is orthogonal to the user's column selection.

---

## Test

`DeleteTest` and `InsertUpdateIfConditionStaticsTest` gain scenarios that write static columns across multiple SSTables and then assert that a partition-level query returns the expected static row even when the queried static column has no value.
