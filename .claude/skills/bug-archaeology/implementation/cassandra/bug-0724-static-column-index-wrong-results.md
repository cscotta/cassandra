# Bug 0724: Static column index queries skip non-matching partition key entries

**Commit**: 41befde227 · **Ticket**: CASSANDRA-13277
**Subsystem**: Secondary indexes / CompositesSearcher
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`CompositesSearcher` handles secondary indexes on static columns differently from regular columns: because a static column is partition-wide, any index hit on a given index key triggers a full partition read rather than a per-row read. However, the code skipped the check (`isMatchingEntry`) that validates whether the index entry's partition key actually satisfies any command-level partition key constraint present in the query. As a result, when a query combined a secondary index on a static column with a partition-key restriction, rows from non-matching partitions were returned, producing incorrect query results.

### Before / After
```java
// BEFORE — no constraint check before issuing the partition read
if (isStaticColumn())
{
    dataCmd = SinglePartitionReadCommand.create(index.baseCfs.metadata, ...);
    ...
}

// AFTER — skip index hits that don't satisfy the partition key constraint
if (isStaticColumn())
{
    if (!isMatchingEntry(partitionKey, nextEntry, command)) {
        nextEntry = indexHits.hasNext() ? index.decodeEntry(indexKey, indexHits.next()) : null;
        continue;
    }
    dataCmd = SinglePartitionReadCommand.create(index.baseCfs.metadata, ...);
    ...
}
```

---

## How to Spot
Whenever a code path has two branches — one for static columns and one for regular columns — verify that correctness checks present in the regular branch are also applied in the static branch (or intentionally excluded with a clear comment explaining why). Index searchers that issue full partition reads are especially prone to skipping per-entry validation because the fetch "works" for the correct partition; the problem only surfaces when an extra restriction filters out some partitions.

---

## Test
`CassandraIndexTest` gains a test that creates a table with a static column index, populates multiple partitions, and executes queries combining the index with a partition key restriction, verifying that only the matching partition is returned.
