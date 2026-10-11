**Commit**: 0815d70592 · **Ticket**: KAFKA-18160
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                // Enable newly added partitions to start fetching and updating positions for them.
                subscriptions.enablePartitionsAwaitingCallback(addedPartitions);
        final Exception e;
        switch (methodName) {
            case ON_PARTITIONS_REVOKED:
                e = rebalanceListenerInvoker.invokePartitionsRevoked(partitions);
                break;
            case ON_PARTITIONS_ASSIGNED:
                e = rebalanceListenerInvoker.invokePartitionsAssigned(partitions);
                break;
            case ON_PARTITIONS_LOST:
                e = rebalanceListenerInvoker.invokePartitionsLost(partitions);

// After
        // Enable newly added partitions to start fetching and updating positions for them.
                // Enable assigned partitions to start fetching and updating positions for them.
                // We use assignedPartitions here instead of addedPartitions because there's a chance that the callback
                // might throw an exception, leaving addedPartitions empty. This would result in the poll operation
                // returning no records, as no topic partitions are marked as fetchable. In contrast, with the classic consumer,
                // if the first callback fails but the next one succeeds, polling can still retrieve data. To align with
                // this behavior, we rely on assignedPartitions to avoid such scenarios.
                subscriptions.enablePartitionsAwaitingCallback(toTopicPartitionSet(assignedPartitions));
import org.apache.kafka.common.errors.WakeupException;
        Exception e;
        try {
            switch (methodName) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18160 interrupting or waking up onpartitionsassigned in asyncconsumer can cause the consumerrebalancelistenercallb.

**Test**
Test modified in ConsumerMembershipManagerTest.java covering the corrected behaviour.
