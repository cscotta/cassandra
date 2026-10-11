**Commit**: 9df616da76 · **Ticket**: KAFKA-19397
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- Metadata doesn't have the full view of topicNames to ids during
rebootstrap of client or when topic has been deleted/recreated. The
solution is to pass down topic id and stop trying to figure it out later
in the logic. ---------

Co-authored-by: Kirk True <kirk@kirktrue.pro>

**Before/After**
```java
// Before
    private void handleProduceResponse(ClientResponse response, Map<TopicPartition, ProducerBatch> batches, long now) {
                    // Version 13 drop topic name and add support to topic id. However, metadata can be used to map topic id to topic name.
                    String topicName = metadata.topicNames().getOrDefault(r.topicId(), r.name());
                    TopicPartition tp = new TopicPartition(topicName, p.index());

// After
import org.apache.kafka.common.Uuid;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
    @ClusterTest
    public void testSendWhileTopicGetRecreated() {
        int maxNumTopicRecreationAttempts = 5;
        var recreateTopicFuture = CompletableFuture.supplyAsync(() -> {
            var topicIds = new HashSet<Uuid>();
            while (topicIds.size() < maxNumTopicRecreationAttempts) {
                try (var admin = cluster.admin()) {
                    if (admin.listTopics().names().get().contains(topic)) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19397: ensure consistent metadata usage in produce request and response (#19964).

**Test**
Test modified in ProducerSendWhileDeletionTest.java covering the corrected behaviour.
