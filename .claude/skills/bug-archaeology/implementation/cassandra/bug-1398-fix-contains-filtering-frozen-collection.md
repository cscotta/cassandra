# Bug 1398: CONTAINS / CONTAINS KEY Filtering Silently Ignored on Frozen Collection Clustering Columns

**Commit**: e3862bc3e0 · **Ticket**: CASSANDRA-8302
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

In `SelectStatement.Preparer`, the code block that validated `CONTAINS` and `CONTAINS KEY` restrictions on clustering columns was wrapped in a block comment (`/* ... */`), effectively disabling it. Without this validation, when a `CONTAINS` restriction was applied to a frozen collection clustering column, `usesSecondaryIndexing` was never set to `true`. As a result, the query planner treated the restriction as dead — the filter was never executed during range reads and the query returned unfiltered rows. The comment was originally added while the feature was being developed and was never removed.

### Before / After

```java
// BEFORE: entire block commented out
/*
else if (restriction.isContains() && !hasQueriableIndex)
{
    throw new InvalidRequestException(...);
}
*/
```

```java
// AFTER: block uncommented and corrected
else if (restriction.isContains())
{
    if (!hasQueriableIndex)
        throw new InvalidRequestException(...);
    stmt.usesSecondaryIndexing = true;
}
```

---

## How to Spot

Commented-out validation code is a red flag in query planners. Any time a restriction type is handled for some column kinds but silently falls through for others (frozen collections, compact storage, etc.) reviewers should verify that filtering is actually applied, not just that parsing succeeds. A quick test querying with `CONTAINS` and checking that the result set is actually filtered is the fastest way to expose this class of bug.

---

## Test

`FrozenCollectionsTest.testClusteringColumnFiltering` was added. It creates a table with a frozen map clustering column, inserts rows, and verifies that `CONTAINS` and `CONTAINS KEY` with `ALLOW FILTERING` return only the matching rows across both multi-partition and single-partition reads.
