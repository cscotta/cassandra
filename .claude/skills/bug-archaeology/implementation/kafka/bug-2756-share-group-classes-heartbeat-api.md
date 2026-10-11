**Commit**: e0dcfa7b51 · **Ticket**: KAFKA-16741
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Defined share group, member and sinmple assignor classes with API definition for Share Group Heartbeat and Describe API. The ShareGroup and ShareGroupMember extends the common ModernGroup and ModernGroupMember respectively. Reviewers: Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
              files="(GroupMetadataManager|ConsumerGroupTest|GroupMetadataManagerTest|GroupMetadataManagerTestContext|GeneralUniformAssignmentBuilder).java"/>
import org.apache.kafka.common.message.{ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, DeleteGroupsResponseData, DescribeGroupsResponseData, HeartbeatRequestData, HeartbeatResponseData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupRequestData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchRequestData, OffsetFetchResponseData, SyncGroupRequestData, SyncGroupResponseData, TxnOffsetCommitRequestData, TxnOffsetCommitResponseData}
import org.apache.kafka.common.message.{ConsumerGroupHeartbeatRequestData, DeleteGroupsResponseData, DescribeGroupsResponseData, HeartbeatRequestData, HeartbeatResponseData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupRequestData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchRequestData, OffsetFetchResponseData, SyncGroupRequestData, SyncGroupResponseData, TxnOffsetCommitRequestData, TxnOffsetCommitResponseData}

// After
              files="(GroupMetadataManager|ConsumerGroupTest|ShareGroupTest|GroupMetadataManagerTest|GroupMetadataManagerTestContext|GeneralUniformAssignmentBuilder).java"/>
import org.apache.kafka.common.message.{ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, DeleteGroupsResponseData, DescribeGroupsResponseData, HeartbeatRequestData, HeartbeatResponseData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupRequestData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchRequestData, OffsetFetchResponseData, ShareGroupDescribeResponseData, ShareGroupHeartbeatRequestData, ShareGroupHeartbeatResponseData, SyncGroupRequestData, SyncGroupResponseData, TxnOffsetCommitRequestData, TxnOffsetCommitResponseData}
  override def shareGroupHeartbeat(
    context: RequestContext,
    request: ShareGroupHeartbeatRequestData
  ): CompletableFuture[ShareGroupHeartbeatResponseData] = {
    FutureUtils.failedFuture(Errors.UNSUPPORTED_VERSION.exception(
      s"The old group coordinator does not support ${ApiKeys.SHARE_GROUP_HEARTBEAT.name} API."
    ))
  }


```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16741: add share group classes for heartbeat api (1/n) (kip-932) (#16516).

**Test**
Test modified in GroupCoordinatorAdapterTest.scala covering the corrected behaviour.
