**Commit**: 0a003d0933 · **Ticket**: KAFKA-19823
**Subsystem**: server/src/main/java/org/apache/kafka/server/share/fetch/PartitionMaxBytesStrategy.java
**Category**: Incorrect filtering / wrong query result

**Root cause**
`PartitionMaxBytesStrategy.uniformPartitionMaxBytes()` divided `requestMaxBytes` by `acquiredPartitionsSize` without checking whether the result could be zero. When `requestMaxBytes` was smaller than `acquiredPartitionsSize` — a legitimate scenario where the fetch request's byte limit is less than the number of acquired partitions — the integer division produced 0, and every partition received 0 bytes in the response, resulting in no data being delivered even when bytes were available. The fix adds three distinct cases: uniform distribution when bytes exceed partitions, greedy distribution when `requestMaxBytes >= partitions.size()`, and random-assignment when `requestMaxBytes < partitions.size()`.

**Before/After**
```java
// Before: could produce partitionMaxBytes = 0 for each partition
private static LinkedHashMap<TopicIdPartition, Integer> uniformPartitionMaxBytes(
        int requestMaxBytes, Set<TopicIdPartition> partitions, int acquiredPartitionsSize) {
    LinkedHashMap<TopicIdPartition, Integer> partitionMaxBytes = new LinkedHashMap<>();
    partitions.forEach(p -> partitionMaxBytes.put(p, requestMaxBytes / acquiredPartitionsSize));
    return partitionMaxBytes;
}

// After: handles the case requestMaxBytes < acquiredPartitionsSize
if (requestMaxBytes >= acquiredPartitionsSize) {
    return allotUniformBytesToPartitions(partitions, requestMaxBytes, acquiredPartitionsSize);
} else if (requestMaxBytes >= partitions.size()) {
    return allotUniformBytesToPartitions(partitions, requestMaxBytes, partitions.size());
} else {
    // random assignment of 1 byte each to requestMaxBytes partitions
    ...
}
```

**How to spot**
Any integer division `a / b` where `a` can be less than `b` risks producing 0 unexpectedly. In byte-budget allocation code, verify that the "allocate 0 bytes to every partition" outcome is either intentional or guarded. The test change from `1024 * 1024` to `1024 * 1020` was the trigger that exposed the zero-bytes path.

**Test**
`PartitionMaxBytesStrategyTest` covers all three new cases: `requestMaxBytes > acquiredPartitions`, `requestMaxBytes < acquiredPartitions but >= partitions.size()`, and `requestMaxBytes < partitions.size()`, verifying the sorted byte allocations match expected values.
