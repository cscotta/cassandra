# Bug 1264: Fix Regression in DISTINCT Queries with Deletions

**Commit**: ce3ce44a53 · **Ticket**: CASSANDRA-8717
**Subsystem**: src/java/org/apache/cassandra/db/AbstractRangeCommand.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`AbstractRangeCommand.trim()` applies a hard row-count limit to the result list of a range scan before returning it to the coordinator. A follow-up for CASSANDRA-8717 introduced the `ignoredTombstonedPartitions()` code path to handle DISTINCT queries that encounter deleted partitions, but the `trim()` method only bypassed the limit when `countCQL3Rows()` was true, not when `ignoredTombstonedPartitions()` was true. When a DISTINCT range scan returned tombstoned partitions that were meant to be filtered out by the caller, the premature truncation to `limit` rows discarded live rows that should have been in the result, producing a short or incorrect answer.

### Before / After

```java
// BEFORE
if (countCQL3Rows())
    return rows;
```

```java
// AFTER
if (countCQL3Rows() || ignoredTombstonedPartitions())
    return rows;
```

---

## How to Spot

When a method that guards result-set trimming or limiting is extended with a new code path (here, the tombstoned-partition handling), every other place that performs the same guard must be updated in lockstep. Review all callers and sibling methods that check `countCQL3Rows()` for analogous gating logic and verify they also account for the new flag.

---

## Test

No test added.
