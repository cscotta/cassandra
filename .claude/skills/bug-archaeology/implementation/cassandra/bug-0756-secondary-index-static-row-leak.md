# Bug 0756: Secondary index queries return static row for filtered partitions

**Commit**: 77f0f68313 · **Ticket**: CASSANDRA-13013
**Subsystem**: CQL / Secondary Indexes / Query Execution
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`SelectStatement.processPartition()` emitted the static row of a partition whenever that partition contained no matching regular rows and the query had no clustering-column restriction. The condition used was `!restrictions.hasClusteringColumnsRestriction()`. After the secondary-index refactor, a query with a restriction on a regular (non-primary-key) column — such as a secondary index query on column `v` — had `hasClusteringColumnsRestriction() == false`, so the static row was unconditionally emitted even when the partition had no matching live rows. The correct predicate for "this is a full-partition scan that should include static rows on empty partitions" is that there are *neither* clustering-column *nor* regular-column restrictions. The fix introduces `queriesFullPartitions()` combining both checks, and adds `hasRegularColumnsRestrictions()` to `StatementRestrictions`.

### Before / After
```java
// BEFORE — static row emitted whenever no clustering restriction, even for index queries
if (!staticRow.isEmpty() && (!restrictions.hasClusteringColumnsRestriction()
        || cfm.isStaticCompactTable()))

// AFTER — only emit static row on true full-partition scans
if (!staticRow.isEmpty() && (queriesFullPartitions() || cfm.isStaticCompactTable()))

// queriesFullPartitions():
return !restrictions.hasClusteringColumnsRestrictions()
    && !restrictions.hasRegularColumnsRestrictions();
```

---

## How to Spot
A chain of refactors that renames or splits a boolean predicate is a common source of this type of regression. Whenever `hasXxxRestriction()` is the sole gate for emitting extra rows in results, verify that the predicate captures all the dimensions it needs to. Index-query tests should cover partitions that have a static column but no matching regular rows, as this exposes the over-emission edge case.

---

## Test
Two new tests in `SecondaryIndexTest`: `testIndexOnStaticColumnWithPartitionWithoutRows` and `testIndexOnRegularColumnWithPartitionWithoutRows`, covering partitions that have a static-column value but no regular rows after deletion.
