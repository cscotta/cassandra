**Commit**: 7d702e1760 · **Ticket**: KAFKA-16749
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Share group consumers use the ShareFetch API to retrieve messages they've claimed (acquired records) from the leader brokers of share partitions. The replica manager provides an API to retrieve messages directly from the underlying topic partition. The implementation of the fetch messages uses replica manager to fetch messages from specific offset known by share partition leader. The requests are sent to a queue and processed asynchronously.

**Before/After**
```java
// Before
import kafka.server.ReplicaManager;

    /**
     * The replica manager is used to get the earliest offset of the share partition, so we can adjust the start offset.
     */
    private final ReplicaManager replicaManager;

        Persister persister,
        ReplicaManager replicaManager
        this.replicaManager = replicaManager;
     * Checks if the number of records between startOffset and endOffset exceeds the record max
     * in-flight limit.

// After
        Persister persister
     * Checks if the records can be acquired for the share partition. The records can be acquired if
     * the number of records in-flight is less than the max in-flight messages. Or if the fetch is
     * to happen somewhere in between the record states cached in the share partition i.e. re-acquire
     * the records that are already fetched before.
     * @return A boolean which indicates whether more records can be acquired or not.
    boolean canAcquireRecords() {
        if (nextFetchOffset() != endOffset() + 1) {
            return true;
        }

import kafka.server.QuotaFactory;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16749: implemented share fetch messages (kip-932) (#16377).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
