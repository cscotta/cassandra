**Commit**: d5d9868568 · **Ticket**: KAFKA-20431
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
The `Assignment` field in the `ConsumerGroupDescribe` response only
included `assignedPartitions`, omitting `partitionsPendingRevocation`. This caused partitions to disappear from the response during
reconciliation. The fix merges both into the `Assignment` field since
the member is still responsible for those partitions until revocation
completes. Reviewers: Sean Quah <squah@confluent.io>, Lianet Magrans
<lmagrans@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.{ConsumerGroupDescribeRequestData, ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatResponseData}
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig

// After
import org.apache.kafka.common.message.{ConsumerGroupDescribeRequestData, ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData}
import org.apache.kafka.coordinator.group.{Assertions, GroupCoordinatorConfig}

  @ClusterTest
  def testConsumerGroupDescribeIncludesPartitionsPendingRevocation(): Unit = {
    // Creates the __consumer_offsets topics because it won't be created automatically
    // in this test because it does not use FindCoordinator API.
    createOffsetsTopic()

    val topicId = createTopic(
      topic = "foo",
      numPartitions = 3
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20431: consumergroupdescribe api does not return partitions being revoked (#22015).

**Test**
Test modified in ConsumerGroupDescribeRequestTest.scala covering the corrected behaviour.
