# Bug 0051: Replica Filtering Protection Triggers Unnecessary Short Read Protection Reads

**Commit**: e5c101673a · **Ticket**: CASSANDRA-20639
**Subsystem**: src/java/org/apache/cassandra/service/reads/ReplicaFilteringProtection.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When the Replica Filtering Protection (RFP) mechanism needed to advance the merged partition iterator to populate cached partition builders, it called `PartitionIterators.consumeNext`, which consumed an entire partition regardless of the query `LIMIT`. For single-partition queries with a `LIMIT`, this caused the underlying Short Read Protection (SRP) iterator to advance far beyond the limit, issuing unnecessary extra reads to replicas. The fix distinguishes between queries that require consuming entire partitions (unlimited or multi-partition queries, or those with static expressions) and those that should only consume up to `LIMIT` rows at a time.

### Before / After

```java
// BEFORE – always consumed the entire next partition
if (partitions.isEmpty())
    PartitionIterators.consumeNext(merged);
```

```java
// AFTER – consumes only LIMIT rows for single-partition limited queries
if (consumeEntirePartitions) {
    // consume whole partition
} else {
    // consume at most command.limits().count() rows, then checkpoint
    while (i < command.limits().count() && currentRowIterator.hasNext()) {
        currentRowIterator.next();
        i++;
    }
    if (i > 0)
        mergeListener.populate();
}
```

---

## How to Spot

When a "advance underlying iterator" helper is called inside a protection or filtering layer, verify that it respects the query's `LIMIT`. Consuming an unbounded number of rows to satisfy one internal check can cascade into SRP making additional remote reads that inflate latency and read amplification. Look for calls to iterator-consuming utilities inside RFP/SRP interplay, and confirm that consumption is bounded by the command limit for single-partition limited queries.

---

## Test

`StrictFilteringTest.testNoShortReadAtLimit` sets up replicas with divergent data, issues a `LIMIT 1` query, and asserts that `shortReadProtectionRequests` does not increase, confirming no unnecessary SRP reads occurred.
