# Bug 0713: Secondary index query on partition key skips static-only partitions

**Commit**: 64d8a1d9fd · **Ticket**: CASSANDRA-13147
**Subsystem**: CQL query execution / secondary indexes
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`SelectStatement.processColumnFamily()` had a guard that prevented a static row from being emitted when no non-static rows followed it. The guard was conditioned on `!usesSecondaryIndexing`, meaning secondary-index queries were always allowed to emit the static row even when the search matched only the partition key and the partition contained no clustering rows. When the index match was on a partition key column and the table had static columns with data, the result erroneously included the static row for partitions that had no regular rows, and — in the converse case — the static row could also be suppressed incorrectly. The fix replaces `!usesSecondaryIndexing` with `hasNoRegularColumnsRestriction()`, which accurately captures whether the result should include static-only partitions regardless of how the partition was located.

### Before / After
```java
// BEFORE
if (staticRow != null && !iter.hasNext()
        && !usesSecondaryIndexing
        && hasNoClusteringColumnsRestriction())
{
    // emit static row
}

// AFTER
if (staticRow != null && !iter.hasNext()
        && hasNoClusteringColumnsRestriction()
        && hasNoRegularColumnsRestriction())
{
    // emit static row
}
```

---

## How to Spot
When a SELECT guard uses a coarse flag like `usesSecondaryIndexing` as a proxy for a more precise predicate ("the query restricts regular columns"), review whether the flag truly captures the intended semantic. Here the real question is whether the query's restriction set targets only non-regular columns; replacing the index-usage flag with a direct inspection of restriction kinds produces the correct answer for both index and non-index paths.

---

## Test
`SecondaryIndexTest` extended with queries against a table that has static columns, using a secondary index on the partition key; verifies correct row counts and values when static-only partitions are present.
