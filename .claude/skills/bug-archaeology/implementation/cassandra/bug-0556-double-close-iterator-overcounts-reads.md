# Bug 0556: Double-Close of Partition Iterator Overcounts Read Requests

**Commit**: 78776d3534 · **Ticket**: CASSANDRA-15058
**Subsystem**: `src/java/org/apache/cassandra/db/partitions/UnfilteredPartitionIterators.java`
**Category**: Resource leak

---

## Root Cause

`UnfilteredPartitionIterators.digest` used a try-with-resources block that both owned and iterated the provided `UnfilteredPartitionIterator`. The caller was also closing the same iterator (as part of its own try-with-resources or explicit close call). The double-close triggered the iterator's close path twice, which decremented the read-request metric counter an extra time per query, causing the per-node read counts to diverge between coordinator and replicas. The fix removes the try-with-resources from inside `digest` (making the caller solely responsible for closing) and adds a Javadoc contract note to that effect.

### Before / After

```java
// BEFORE
public static void digest(..., UnfilteredPartitionIterator iterator, ...) {
    try (UnfilteredPartitionIterator iter = iterator) {
        while (iter.hasNext()) {
            try (UnfilteredRowIterator partition = iter.next()) {
                UnfilteredRowIterators.digest(command, partition, digest, version);
            }
        }
    }
}
```

```java
// AFTER — caller must close the iterator
public static void digest(..., UnfilteredPartitionIterator iterator, ...) {
    while (iterator.hasNext()) {
        try (UnfilteredRowIterator partition = iterator.next()) {
            UnfilteredRowIterators.digest(command, partition, digest, version);
        }
    }
}
```

---

## How to Spot

When a method receives an `AutoCloseable` or iterator parameter and wraps it in try-with-resources, check whether the caller also closes it. Ownership of `AutoCloseable` objects must be documented (or enforced via type system). Any method that takes an iterator by parameter and closes it without a clear ownership contract is a candidate for double-close bugs.

---

## Test

A distributed test `metricsCountQueriesTest` was added to `DistributedReadWritePathTest` that executes 100 single-row reads at `ConsistencyLevel.ALL` across two nodes and asserts that the read-latency counter on each node is exactly 100.
