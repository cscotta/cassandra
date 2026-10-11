# Bug 1208: Suboptimal Secondary Index Selection When Clustering Column Is Also Indexed

**Commit**: f2db756abd · **Ticket**: CASSANDRA-9631
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Logic error in condition

---

## Root Cause

When building a SELECT statement, the planner tracks which clustering columns are restricted and whether those restrictions come from a secondary index. After determining whether secondary indexing will be used, it prunes clustering column restrictions from `restrictedNames` so they do not interfere with index selection. The pruning condition was `if (!indexed)` — meaning it removed entries for columns that are *not* indexed. But when the query *does* use a secondary index (`usesSecondaryIndexing == true`), even an indexed clustering column should be removed from the map to avoid double-counting it as an index target. Because the `|| !stmt.usesSecondaryIndexing` guard was absent, queries with an indexed clustering column could end up choosing a suboptimal (or wrong) secondary index.

### Before / After

```java
// BEFORE
if (!indexed)
    stmt.restrictedNames.remove(clusteringColumn);
```

```java
// AFTER
if (!indexed || !stmt.usesSecondaryIndexing)
    stmt.restrictedNames.remove(clusteringColumn);
```

---

## How to Spot

Whenever a map or set is pruned inside index-selection logic, verify that every branch of the keep/remove decision accounts for whether secondary indexing is actually active. A condition that only checks the property of the entry (`!indexed`) without also checking the mode of query execution (`!usesSecondaryIndexing`) is a sign that two orthogonal concerns have been conflated into one guard, leaving a case reachable where the entry should still be removed.

---

## Test

Tests added to `MultiColumnRelationTest` and `SingleColumnRelationTest` verify that queries with an indexed clustering column return correct results without requiring `ALLOW FILTERING`, covering equality, IN, and range predicates.
