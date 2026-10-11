# Bug 1125: Queries on Static Compact Tables Return Wrong Results

**Commit**: 2350320501 · **Ticket**: CASSANDRA-10093
**Subsystem**: cql3/restrictions/StatementRestrictions, cql3/statements/SelectStatement
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Static compact tables expose only static columns to CQL, but internally they have a fake clustering column. `StatementRestrictions.getClusteringColumns()` was returning the value from the clustering restriction (which would include the fake column's value), causing `ClusteringIndexNamesFilter` to attempt to fetch a specific clustering row rather than the static part. Similarly, `SelectStatement` was gating the filter creation on `selection.containsStaticColumns()` instead of on whether the fetched column set had non-empty statics, causing silent query suppression in some paths. The fix makes `getClusteringColumns()` return an empty set for static compact tables so the filter correctly targets static storage, and tightens the empty-clustering guard to check `queriedColumns.fetchedColumns().statics.isEmpty()`.

### Before / After

```java
// BEFORE (StatementRestrictions.getClusteringColumns)
public NavigableSet<Clustering> getClusteringColumns(QueryOptions options) throws InvalidRequestException
{
    return clusteringColumnsRestrictions.valuesAsClustering(options);
}
```

```java
// AFTER
public NavigableSet<Clustering> getClusteringColumns(QueryOptions options) throws InvalidRequestException
{
    if (cfm.isStaticCompactTable())
        return BTreeSet.empty(cfm.comparator);
    return clusteringColumnsRestrictions.valuesAsClustering(options);
}
```

---

## How to Spot

When a code path special-cases `isStaticCompactTable()` for one operation (e.g., `isColumnRange()`), every adjacent method that inspects clustering columns should also ask whether the table is a static compact table. The absence of a matching guard in a sibling method is a strong signal that the fix was incomplete. Reviewers should search for all usages of `isStaticCompactTable()` and check that the surrounding control flow is consistent.

---

## Test

No test added for the fix itself; an assertion was added to `Clustering.Serializer.serialize` to catch mismatched clustering sizes early.
