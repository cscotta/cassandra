**Commit**: 057460e807 · **Ticket**: KAFKA-17182
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Jun Rao <jun@confluent.io>, Lianet Magrans <lmagrans@confluent.io>, Jeff Kim <jeff.kim@confluent.io>

**Before/After**
```java
// Before
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

// After
import org.apache.kafka.clients.consumer.Consumer;
import java.util.Collection;
import java.util.Collections;
     * Return the set of <em>fetchable</em> partitions, which are the set of partitions to which we are subscribed,
     * @param buffered The set of partitions we have in our buffer
    private Set<TopicPartition> fetchablePartitions(Set<TopicPartition> buffered) {
        // This is the set of partitions that have buffered data
        Set<TopicPartition> buffered = Collections.unmodifiableSet(fetchBuffer.bufferedPartitions());

        // This is the set of partitions that do not have buffered data
        Set<TopicPartition> unbuffered = fetchablePartitions(buffered);
        if (unbuffered.isEmpty()) {
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17182: consumer fetch sessions are evicted too quickly with asynckafkaconsumer (#18795).

**Test**
Test modified in FetchRequestManagerTest.java covering the corrected behaviour.
