# Bug 1972: SELECT Incorrectly Allowed Non-EQ Partition Key Relations via Secondary Index

**Commit**: 4021fffa0c · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

During SELECT validation, when a partition key restriction was not an EQ or IN relation, the code allowed the query to proceed if a secondary index was queriable (`hasQueriableIndex`), enabling secondary-index-backed scans with non-EQ partition key predicates. However, secondary indexes in Cassandra are also ordered by the partitioner, so a range predicate on the partition key still cannot be correctly answered via a secondary index without full filtering. The condition was removed and an unconditional error is now thrown for any non-EQ/IN partition key relation, regardless of secondary index availability.

### Before / After

```java
// BEFORE - silently permitted non-EQ partition key restriction via secondary index
if (hasQueriableIndex)
{
    stmt.usesSecondaryIndexing = true;
    break;
}
throw new InvalidRequestException("Only EQ and IN relation are supported on the partition key for random partitioners (unless you use the token() function)");
```

```java
// AFTER - unconditionally rejects non-EQ partition key restriction
// Non EQ relation is not supported without token(), even if we have a 2ndary index
// (since even those are ordered by partitioner).
throw new InvalidRequestException("Only EQ and IN relation are supported on the partition key (unless you use the token() function)");
```

---

## How to Spot

When validating query predicates, be careful about short-circuiting validation with index availability. An index that is partitioner-ordered cannot substitute for token-function semantics; a secondary index check should not bypass fundamental partition key access pattern rules.

---

## Test

No test added.
