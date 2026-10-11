**Commit**: 346fdbafc5 · **Ticket**: KAFKA-17912
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private TopicPartition topicPartitionFor(SharePartitionKey key) {
        return new TopicPartition(Topic.SHARE_GROUP_STATE_TOPIC_NAME, partitionFor(key.toString()));

// After
  // For key type SHARE (2), the coordinator key format is "groupId:topicId:partition".
    TopicPartition topicPartitionFor(SharePartitionKey key) {
        return new TopicPartition(Topic.SHARE_GROUP_STATE_TOPIC_NAME, partitionFor(key.asCoordinatorKey()));
import org.apache.kafka.common.TopicIdPartition;
import org.apache.kafka.server.share.SharePartitionKey;

    @Test
    public void testTopicPartitionFor() {
        CoordinatorRuntime<ShareCoordinatorShard, CoordinatorRecord> runtime = mockRuntime();
        ShareCoordinatorService service = new ShareCoordinatorService(
            new LogContext(),
            ShareCoordinatorConfigTest.createConfig(ShareCoordinatorConfigTest.testConfigMap()),
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17912 align string representations of sharepartitionkey (#17656).

**Test**
Test modified in ShareCoordinatorServiceTest.java covering the corrected behaviour.
