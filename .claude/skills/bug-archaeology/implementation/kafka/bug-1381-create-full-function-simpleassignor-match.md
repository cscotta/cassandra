**Commit**: 4b5a16bf6f · **Ticket**: KAFKA-18757
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
### About
The current `SimpleAssignor` in AK assigned all subscribed topic
partitions to all the share group members. This does not match the
description given in
[KIP-932](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=255070434#KIP932:QueuesforKafka-TheSimpleAssignor). Here are the rules as mentioned in the KIP by which the assignment
should happen. We have changed the step 3 implementation here due to the
reasons
[described](https://github.com/apache/kafka/pull/18864#issuecomment-2659266502)
-

1.

**Before/After**
```java
// Before
      val topicId = TestUtils.createTopicWithAdminRaw(
      // This is the expected assignment.
      val expectedAssignment = new ShareGroupHeartbeatResponseData.Assignment()
        .setTopicPartitions(List(new ShareGroupHeartbeatResponseData.TopicPartitions()
          .setTopicId(topicId)
          .setPartitions(List[Integer](0, 1, 2).asJava)).asJava)

        shareGroupHeartbeatResponse.data.errorCode == Errors.NONE.code &&
          shareGroupHeartbeatResponse.data.assignment == expectedAssignment
        shareGroupHeartbeatResponse.data.errorCode == Errors.NONE.code &&
          shareGroupHeartbeatResponse.data.assignment == expectedAssignment

// After
import java.util
      TestUtils.createTopicWithAdminRaw(
        shareGroupHeartbeatResponse.data.errorCode == Errors.NONE.code && shareGroupHeartbeatResponse.data.assignment != null
      val topicPartitionsAssignedToMember1 = shareGroupHeartbeatResponse.data.assignment.topicPartitions()
        shareGroupHeartbeatResponse.data.errorCode == Errors.NONE.code && shareGroupHeartbeatResponse.data.assignment !=  null
      val topicPartitionsAssignedToMember2 = shareGroupHeartbeatResponse.data.assignment.topicPartitions()
      val partitionsAssigned: util.Set[Integer] = new util.HashSet[Integer]()
      topicPartitionsAssignedToMember1.forEach(topicPartition => {
        partitionsAssigned.addAll(topicPartition.partitions())
      })
      topicPartitionsAssignedToMember2.forEach(topicPartition => {
        partitionsAssigned.addAll(topicPartition.partitions())
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18757: create full-function simpleassignor to match kip-932 description (#18864).

**Test**
Test modified in ShareGroupHeartbeatRequestTest.scala covering the corrected behaviour.
