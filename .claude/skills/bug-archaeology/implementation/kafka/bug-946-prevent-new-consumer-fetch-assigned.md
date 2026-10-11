**Commit**: ce996b34e9 · **Ticket**: KAFKA-19356
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Fix to ensure assigned partitions whose topics are not in the consumer
explicit subscription are considered not fetchable (so that no records
are returned on poll for them)

This scenario could happen in the new async consumer (using the Consumer
rebalance protocol) when the subscription changes, because the consumer
will keep its assignment until the coordinator sends a new one (broker
drives assignments). This does not happen in the classic consumer because the assignment
logic lives on the client-side, so the consumer pro-actively updates
assignment as needed. This PR validates assignment vs subscription on fetch for explicit
subscription  only. Regular expressions, shared subscription  r...

**Before/After**
```java
// Before
            // poll call or if the offset is being reset
            if ((subscriptionType.equals(SubscriptionType.AUTO_TOPICS_SHARE) || topicPartitionState.isFetchable())
        TopicPartitionState assignedOrNull = assignedStateOrNull(tp);
        return assignedOrNull != null && assignedOrNull.isFetchable();

// After
            // poll call or if the offset is being reset.
            // It can also happen under the Consumer rebalance protocol, when the consumer changes its subscription.
            // Until the consumer receives an updated assignment from the coordinator, it can hold assigned partitions
            // that are not in the subscription anymore, so we make them not fetchable.
            if ((subscriptionType.equals(SubscriptionType.AUTO_TOPICS_SHARE) || isFetchableAndSubscribed(topicPartition, topicPartitionState))
    /**
     * Check if the partition is fetchable.
     * If the consumer has explicitly subscribed to a list of topic names,
     * this will also check that the partition is contained in the subscription.
     */
    private synchronized boolean isFetchableAndSubscribed(TopicPartition topicPartition, TopicPartitionState topicPartitionState) {
        if (subscriptionType.equals(SubscriptionType.AUTO_TOPICS) && !subscription.contains(topicPartition.topic())) {
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19356: prevent new consumer fetch assigned partitions not in explicit subscription  (#19983).

**Test**
Test modified in SubscriptionStateTest.java covering the corrected behaviour.
