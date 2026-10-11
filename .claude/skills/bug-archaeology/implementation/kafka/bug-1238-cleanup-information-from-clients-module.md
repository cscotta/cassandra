**Commit**: a4375045d6 · **Ticket**: KAFKA-19055
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Removes outdated references to Kafka 0.10.x in the clients module
documentation. Since the baseline version is now 2.1, any mentions of
versions earlier than this are unnecessary and have been removed or
updated accordingly. Changes:
- Updated `ClusterResource`, `ClusterResourceListener`, and
`DescribeClusterResult` Javadoc to reflect the minimum supported broker
version as 2.1. - Updated `TopicConfig` documentation: Removed references to consumers
older than 0.10.2.

**Before/After**
```java
// Before
     * Returns a future which yields the current cluster id. The future value will be non-null if the
     * broker version is 0.10.1.0 or higher and null otherwise.
     * metadata request was sent to a broker without support for cluster ids. The first version of Kafka
     * to support cluster id is 0.10.1.0.
     * support for cluster ids. The first version of Kafka to support cluster id is 0.10.1.0.
 * Note that the cluster id may be null when the Kafka broker version is below 0.10.1.0. If you receive a null cluster id, you can expect it to always be null unless you have a cluster with multiple broker versions which can happen if the cluster is being upgraded while the client is running.
        "If this is increased and there are consumers older than 0.10.2, the consumers' fetch " +
        "size must also be increased so that they can fetch record batches this large. " +

// After
     * Returns a future which yields the current cluster id.
     * metadata request was sent to a broker without support for cluster ids.
     * support for cluster ids.
 * Note the minimum supported broker version is 2.1.
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19055 cleanup the 0.10.x information from clients module (#19320).

**Test**
No test added.
