**Commit**: 6cf54c4dab · **Ticket**: KAFKA-17182
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
This change reduces fetch session cache evictions on the broker for AsyncKafkaConsumer by altering its logic to determine which partitions it includes in fetch requests. Background
Consumer implementations fetch data from the cluster and temporarily buffer it in memory until the user next calls Consumer.poll(). When a fetch request is being generated, partitions that already have buffered data are not included in the fetch request. The ClassicKafkaConsumer performs much of its fetch logic and network I/O in the application thread.

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

// After
import java.util.Collections;
     * Return the set of <em>fetchable</em> partitions, which are the set of partitions to which we are subscribed,
     * @param buffered The set of partitions we have in our buffer
    private Set<TopicPartition> fetchablePartitions(Set<TopicPartition> buffered) {
        // This is the set of partitions that have buffered data
        Set<TopicPartition> buffered = Collections.unmodifiableSet(fetchBuffer.bufferedPartitions());
        // This is the set of partitions that do not have buffered data
        Set<TopicPartition> unbuffered = fetchablePartitions(buffered);
        if (unbuffered.isEmpty()) {
            // If there are no partitions tha
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17182: consumer fetch sessions are evicted too quickly with asynckafkaconsumer (#17700).

**Test**
Test modified in FetchRequestManagerTest.java covering the corrected behaviour.
