# Bug 0673: Static Compact Table Row Count Ignores LIMIT

**Commit**: 336baebadc · **Ticket**: CASSANDRA-11223
**Subsystem**: `src/java/org/apache/cassandra/db/filter/`, `src/java/org/apache/cassandra/db/`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

For static compact tables, `SelectStatement.makeClusteringIndexFilter()` produces a `ClusteringIndexNamesFilter` with an empty clustering set (because there are no clustering columns). `ClusteringIndexNamesFilter.selectsAllPartition()` always returned `false`, which caused `selectsFullPartition()` to be `false` even though the entire partition was being read. Consequently the row-limit counter was never given the "full partition" signal it needed, so static-only rows were silently discarded by the ColumnCounter before the LIMIT was applied, causing queries like `SELECT * FROM compact_table LIMIT 5` to return fewer than 5 rows when some partitions contained only static data.

### Before / After

```java
// BEFORE – ClusteringIndexNamesFilter.selectsAllPartition()
public boolean selectsAllPartition()
{
    return false;
}
```

```java
// AFTER
public boolean selectsAllPartition()
{
    // an empty clustering set means we're selecting the static row only
    return clusterings.isEmpty();
}
```

---

## How to Spot

When a filter can legitimately produce an empty name-set with a meaningful semantics (here: "read the static row"), a blanket `return false` from `selectsAllPartition()` is wrong. Reviewers should check every `selectsAllPartition` override and ask: "is there any configuration of this filter that does, in fact, select the full partition?" The `isStaticCompactTable` guard added to `selectsFullPartition()` provides a belt-and-suspenders safety for the read commands themselves.

---

## Test

Inserts 10 rows into a `WITH COMPACT STORAGE` static table and asserts that `SELECT * LIMIT 5`, `SELECT v LIMIT 5`, `SELECT k LIMIT 5`, and `SELECT DISTINCT k LIMIT 5` each return exactly 5 rows.
