**Commit**: e3c456ff0f · **Ticket**: KAFKA-19169
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Enhance AuthorizerIntegrationTest for share group APIs

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, OffsetFetchRequestData, ProduceRequestData, SyncGroupRequestData, WriteTxnMarkersRequestData}
import org.apache.kafka.common.{ElectionType, IsolationLevel, KafkaException, TopicPartition, Uuid, requests}

// After
  val shareGroup = "share-group"
  val shareGroupResource = new ResourcePattern(GROUP, shareGroup, LITERAL)
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteShareGroupOffsetsRequestData, DeleteShareGroupStateRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeShareGroupOffsetsRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, InitializeShareGroupStateRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, OffsetFetchRequestData, ProduceRequestData, ReadShareGroupStateRequestData, ReadShareGroupStateSummaryRequestData, ShareAcknowledgeRequestData, ShareFetchRequestData, ShareGroupDescribeRequestData, ShareGroupHeartbeatRequestData, SyncGroupRequestData, WriteShareGroupStateRequestData, WriteTxnMarkersRequestData}
import org.apache.kafka.common.{ElectionType, IsolationLevel, KafkaException, TopicIdPartition, TopicPartition, Uuid, requests}
  val shareGroupReadAcl = Map(shareGroupResource -> Set(new AccessControl
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19169: enhance authorizerintegrationtest for share group apis (#19540).

**Test**
Test modified in AbstractAuthorizerIntegrationTest.scala covering the corrected behaviour.
