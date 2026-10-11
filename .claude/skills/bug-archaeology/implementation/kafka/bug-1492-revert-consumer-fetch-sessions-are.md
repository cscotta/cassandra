**Commit**: 7920fadbb5 · **Ticket**: KAFKA-17182
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This reverts commit 6cf54c4dab9ff39f21d7f2c9bd241b938b5651b2.

**Before/After**
```java
// Before
import java.util.Collections;
     * Return the set of <em>fetchable</em> partitions, which are the set of partitions to which we are subscribed,
     * @param buffered The set of partitions we have in our buffer
    private Set<TopicPartition> fetchablePartitions(Set<TopicPartition> buffered) {
        // This is the set of partitions that have buffered data
        Set<TopicPartition> buffered = Collections.unmodifiableSet(fetchBuffer.bufferedPartitions());
        // This is the set of partitions that do not have buffered data
        Set<TopicPartition> unbuffered = fetchablePartitions(buffered);
        if (unbuffered.isEmpty()) {
            // If there are no partitions that don't already have data locally buffered, there's no need to issue
            // any fetch requests at the present time.
            return Collections.emptyMap();

// After
     * Return the list of <em>fetchable</em> partitions, which are the set of partitions to which we are subscribed,
    private Set<TopicPartition> fetchablePartitions() {
        // This is the set of partitions we have in our buffer
        Set<TopicPartition> buffered = fetchBuffer.bufferedPartitions();

        for (TopicPartition partition : fetchablePartitions()) {
            SubscriptionState.FetchPosition position = subscriptions.position(partition);
            if (position == null)
                throw new IllegalStateException("Missing position for fetchable partition " + partition);
            Optional<Node> leaderOpt = position.currentLeader.leader;
            if (leaderOpt.isEmpty()) {
                log.debug("Requesting metadata update for partition {} since the position {} is missing the current leader node", partition, position);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: revert "kafka-17182: consumer fetch sessions are evicted too quickly with asynckafkaconsumer (#17700)".

**Test**
Test modified in FetchRequestManagerTest.java covering the corrected behaviour.
