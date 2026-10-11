## Commit

eb3714f022 — KAFKA-19160/KAFKA-19164: O(T×P) stable-offset lookup restructured to O(1)

## Ticket

KAFKA-19160, KAFKA-19164

## Subsystem

Group coordinator / offset metadata management

## Category

Incorrect filtering / wrong query result

## Root cause

`OffsetMetadataManager.fetchOffsets` determined whether an offset was "stable" (committed outside an open transaction) by iterating over all pending transactions for the group and checking each transaction's partition set. This was O(T×P) where T is the number of open transactions and P is the number of partitions being fetched. Under high transaction throughput, this inner loop caused fetch latency to grow linearly with the transaction count. Additionally, when multiple transactions touched the same partition, only the last one's offset was checked, so some offsets were returned even though they should have been filtered as pending.

## Before / After

```java
// Before — O(T×P) per fetch, wrong filter when multiple txns on same partition
Map<String, Set<Long>> openTransactionsByGroup; // groupId -> set of producerIds
for (Long producerId : openTransactionsByGroup.get(groupId)) {
    if (pendingTxn.partitions.contains(tp)) { return pendingOffset; }
}

// After — O(1) lookup with nested partition index
Map<String, Map<String, Map<Integer, Set<Long>>>> openTransactionsByGroup;
// groupId -> topic -> partition -> set of producerIds
Set<Long> producers = openTransactionsByGroup
    .getOrDefault(groupId, Map.of())
    .getOrDefault(topic, Map.of())
    .getOrDefault(partition, Set.of());
```

## How to spot

In offset-fetch paths, search for nested loops over `openTransactions` or `pendingTransactions` keyed only by group ID. Any such loop that then checks partition membership by iteration is O(T×P) and should be replaced with a partition-indexed map for O(1) lookup.

## Test

`OffsetMetadataManagerTest` was extended with high-transaction-count scenarios to verify correctness and benchmark tests confirm O(1) fetch time.
