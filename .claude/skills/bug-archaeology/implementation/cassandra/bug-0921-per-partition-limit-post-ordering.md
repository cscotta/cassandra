# Bug 0921: PER PARTITION LIMIT Silently Dropped for Post-Query Ordering Queries

**Commit**: 9a0eb9a31e · **Ticket**: CASSANDRA-11556
**Subsystem**: CQL / Query execution
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`SelectStatement.makeDataLimits()` set `cqlPerPartitionLimit` only inside the branch that also required `!needsPostQueryOrdering()`. When a query needed post-query ordering (e.g., `ORDER BY` on a non-native-ordered column), the entire `if` block was skipped, meaning the `PER PARTITION LIMIT` clause was silently ignored and the full partition was scanned. Additionally, the code did not validate that `PER PARTITION LIMIT` is incompatible with aggregate queries; that combination was accepted but produced wrong results.

### Before / After
```java
// BEFORE
if (!selection.isAggregate() && !needsPostQueryOrdering())
{
    cqlRowLimit = userLimit;
    cqlPerPartitionLimit = perPartitionLimit;
}

// AFTER
if (!selection.isAggregate())
{
    if (!needsPostQueryOrdering())
        cqlRowLimit = userLimit;
    cqlPerPartitionLimit = perPartitionLimit;
}

// Also added in prepare():
checkFalse(selection.isAggregate() && perPartitionLimit != null,
           "PER PARTITION LIMIT is not allowed with aggregate queries.");
```

---

## How to Spot
Whenever a query-limit flag is applied inside a compound boolean condition, verify that each sub-condition is independently correct. Here, guarding `cqlPerPartitionLimit` with `!needsPostQueryOrdering()` was incorrect because per-partition limiting is orthogonal to post-ordering; the limit can and should be enforced before the ordering step. Review all paths through limit-setting code and write tests that combine `PER PARTITION LIMIT` with `ORDER BY` and with aggregate functions.

---

## Test
`SelectLimitTest.testPerPartitionLimit` / `testPerPartitionLimitWithCompactStorage` — covers `PER PARTITION LIMIT` with `ORDER BY`, `IN`, filtering, combined `LIMIT`, and validates that it is rejected with `SELECT DISTINCT` and aggregate queries.
