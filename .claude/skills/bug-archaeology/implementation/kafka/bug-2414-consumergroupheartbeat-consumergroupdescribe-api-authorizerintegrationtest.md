**Commit**: f629b14890 · **Ticket**: KAFKA-14772
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
The patch adds ConsumerGroupHeartbeat and ConsumerGroupDescribe API to AuthorizerIntegrationTest. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, ControlledShutdownRequestData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeTransactionsRequestData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, ProduceRequestData, SyncGroupRequestData, WriteTxnMarkersRequestData}
    })
    ApiKeys.DESCRIBE_TRANSACTIONS -> transactionalIdDescribeAcl
    if (!isKRaftTest()) {

// After
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ControlledShutdownRequestData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeTransactionsRequestData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, ProduceRequestData, SyncGroupRequestData, WriteTxnMarkersRequestData}
    }),
    ApiKeys.CONSUMER_GROUP_HEARTBEAT -> ((resp: ConsumerGroupHeartbeatResponse) => Errors.forCode(resp.data.errorCode)),
    ApiKeys.CONSUMER_GROUP_DESCRIBE -> ((resp: ConsumerGroupDescribeResponse) =>
      Errors.forCode(resp.data.groups.asScala.find(g => group == g.groupId).head.errorCode))
    ApiKeys.DESCRIBE_TRANSACTIONS -> transactionalIdDescribeAcl,
    ApiKeys.CONSUMER_GROUP_HEARTBEAT -> groupReadAcl,
    ApiKeys.CONSUMER_GROUP_DESCRIBE -> groupDescribeAcl
  private def consumerGroupHeartbeatRequest = new ConsumerGroupHeartbeatRequest.Builder(
    new ConsumerGroupHeartbeatRequestData()
      .setGroupId(group)
      .setMemberEpoch(0)).build()
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14772; add consumergroupheartbeat and consumergroupdescribe api to authorizerintegrationtest (#17044).

**Test**
Test modified in AuthorizerIntegrationTest.scala covering the corrected behaviour.
