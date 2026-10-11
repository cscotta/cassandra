**Commit**: be4762e401 · **Ticket**: KAFKA-19342
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add integration tests for alter share group offsets API. Reviewers: Lan Ding <53332773+DL1231@users.noreply.github.com>, Sushant
 Mahajan <smahajan@confluent.io>, Apoorv Mittal
 <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteShareGroupOffsetsRequestData, DeleteShareGroupStateRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeShareGroupOffsetsRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, InitializeShareGroupStateRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, OffsetFetchRequestData, OffsetFetchResponseData, ProduceRequestData, ReadShareGroupStateRequestData, ReadShareGroupStateSummaryRequestData, ShareAcknowledgeRequestData, ShareFetchRequestData, ShareGroupDescribeRequestData, ShareGroupHeartbeatRequestData, SyncGroupRequestData, WriteShareGroupStateRequestData, WriteTxnMarkersRequestData}
    ApiKeys.DELETE_SHARE_GROUP_OFFSETS -> (shareGroupDeleteAcl ++ topicReadAcl)

// After
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, AlterShareGroupOffsetsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteShareGroupOffsetsRequestData, DeleteShareGroupStateRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeShareGroupOffsetsRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, InitializeShareGroupStateRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, OffsetFetchRequestData, OffsetFetchResponseData, ProduceRequestData, ReadShareGroupStateRequestData, ReadShareGroupStateSummaryRequestData, ShareAcknowledgeRequestData, ShareFetchRequestData, ShareGroupDescribeRequestData, ShareGroupHeartbeatRequestData, SyncGroupRequestData, WriteShareGroupStateRequestData, WriteTxnMarkersRequestData}
      resp.data.errorCode)),
    ApiKeys.ALTER_SHARE_GROUP_OFFSETS -> ((resp: AlterShareGroupOffsetsResponse) => Errors.forCode(
    ApiKeys.DELETE_SHARE_GROUP_OFFSETS -> (shareGroupDeleteAcl ++ topicReadAcl),
    ApiKeys.ALTER_SHARE_GROUP_OFFSETS -> (shareGroupReadAcl ++ topicReadAcl)
  private def alterShareGroupOffsetsRequest = {
    val data = new AlterShareGroupOffsetsRequestData
    val topicCollection = new AlterShareGroupOffsetsRequestData.AlterShareGroupOffsetsRequestTopicCollection()
    topicCollection.add(new AlterShareGroupOffsetsRequestData.AlterShareGroupOffsetsRequestTopic()
      .setTopicName(topic)
      .setPartitions(List(new AlterShareGroupOffsetsRequestData.AlterShareGroupOffsetsRequestPartition()
        .setPartitionIndex(part)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19342: authorization tests for alter share-group offsets (#19826).

**Test**
Test modified in AuthorizerIntegrationTest.scala covering the corrected behaviour.
