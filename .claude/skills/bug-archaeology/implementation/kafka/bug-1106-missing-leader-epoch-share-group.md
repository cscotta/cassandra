**Commit**: 7d027a4d83 · **Ticket**: KAFKA-19218
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
When the persister is responding to a read share-group state summary
request, it has no way of including the leader epoch in its response,
even though it has the information to hand. This means that the leader
epoch information is not initialised in the admin client operation to
list share group offsets, and this then means that the information
cannot be displayed in kafka-share-groups.sh. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Sushant Mahajan
 <smahajan@confluent.io>

**Before/After**
```java
// Before
        SimpleAdminApiFuture<CoordinatorKey, Map<TopicPartition, Long>> future = ListShareGroupOffsetsHandler.newFuture(groupSpecs.keySet());
    private final Map<String, KafkaFuture<Map<TopicPartition, Long>>> futures;
    ListShareGroupOffsetsResult(final Map<CoordinatorKey, KafkaFuture<Map<TopicPartition, Long>>> futures) {
    public KafkaFuture<Map<String, Map<TopicPartition, Long>>> all() {
                Map<String, Map<TopicPartition, Long>> offsets = new HashMap<>(futures.size());
    public KafkaFuture<Map<TopicPartition, Long>> partitionsToOffset(String groupId) {

// After
        SimpleAdminApiFuture<CoordinatorKey, Map<TopicPartition, OffsetAndMetadata>> future = ListShareGroupOffsetsHandler.newFuture(groupSpecs.keySet());
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
    private final Map<String, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> futures;
    ListShareGroupOffsetsResult(final Map<CoordinatorKey, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> futures) {
    public KafkaFuture<Map<String, Map<TopicPartition, OffsetAndMetadata>>> all() {
                Map<String, Map<TopicPartition, OffsetAndMetadata>> offsets = new HashMap<>(futures.size());
    public KafkaFuture<Map<TopicPartition, OffsetAndMetadata>> partitionsToOffsetAndMetadata(String groupId) {
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import java.util.Optional;
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19218: add missing leader epoch to share group state summary response (#19602).

**Test**
Test modified in AdminClientTestUtils.java covering the corrected behaviour.
