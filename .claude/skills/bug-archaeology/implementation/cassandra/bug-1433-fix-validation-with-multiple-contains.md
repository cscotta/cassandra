# Bug 1433: Fix Validation with Multiple CONTAINS Clauses

**Commit**: 18cd3e3205 · **Ticket**: CASSANDRA-8131
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The `needFiltering` check in `SelectStatement` used `stmt.restrictedColumns.size() > 1` to decide whether a query required `ALLOW FILTERING`. When a single collection column was restricted by multiple `CONTAINS` or `CONTAINS KEY` clauses, all of those clauses were collapsed into a single `Contains` restriction object for that one column, so `restrictedColumns.size()` remained 1. The query therefore bypassed the `ALLOW FILTERING` guard even though evaluating multiple containment predicates on a collection requires a full scan and in-memory filtering.

### Before / After

```java
// BEFORE
if (stmt.restrictedColumns.size() > 1 || (stmt.restrictedColumns.isEmpty() && !stmt.columnFilterIsIdentity()))
    throw new InvalidRequestException("Cannot execute this query as it might involve data filtering...");
```

```java
// AFTER
private static boolean needFiltering(SelectStatement stmt) {
    return stmt.restrictedColumns.size() > 1
        || (stmt.restrictedColumns.isEmpty() && !stmt.columnFilterIsIdentity())
        || (!stmt.restrictedColumns.isEmpty()
                && stmt.isRestrictedByMultipleContains(Iterables.getOnlyElement(stmt.restrictedColumns)));
}
// isRestrictedByMultipleContains checks (contains.numberOfValues() + contains.numberOfKeys()) > 1
```

---

## How to Spot

When a restriction type aggregates multiple user-supplied predicates into a single object (here, multiple `CONTAINS` clauses merge into one `Contains` instance), any size-based guard on the restriction set will under-count. Reviewers should verify that "how many restrictions exist" checks account for the multiplicity within each restriction object, not just the number of distinct restricted columns.

---

## Test

`ContainsRelationTest` is extended with assertions that queries using two `CONTAINS`, two `CONTAINS KEY`, or a mixed `CONTAINS KEY` + `CONTAINS` on the same collection column are rejected without `ALLOW FILTERING` and return empty results when `ALLOW FILTERING` is provided.
