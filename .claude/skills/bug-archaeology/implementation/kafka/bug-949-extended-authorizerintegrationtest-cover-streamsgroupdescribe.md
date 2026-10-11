**Commit**: 2a06335569 · **Ticket**: KAFKA-19413
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Extending test coverage of authorization for streams group RPC
StreamsGroupDescribe. The RPC requires DESCRIBE GROUP and DESCRIBE TOPIC
permissions for all topics. Reviewers: Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
import java.util.{Comparator, Optional, Properties}
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, AlterShareGroupOffsetsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteShareGroupOffsetsRequestData, DeleteShareGroupStateRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeShareGroupOffsetsRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, InitializeShareGroupStateRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, OffsetFetchRequestData, OffsetFetchResponseData, ProduceRequestData, ReadShareGroupStateRequestData, ReadShareGroupStateSummaryRequestData, ShareAcknowledgeRequestData, ShareFetchRequestData, ShareGroupDescribeRequestData, ShareGroupHeartbeatRequestData, StreamsGroupHeartbeatRequestData, StreamsGroupHeartbeatResponseData, SyncGroupRequestData, WriteShareGroupStateRequestData, WriteTxnMarkersRequestData}

// After
import java.util.{Comparator, Optional, Properties, UUID}
import org.apache.kafka.clients.consumer.internals.{StreamsRebalanceData, StreamsRebalanceListener}
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, AlterShareGroupOffsetsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteShareGroupOffsetsRequestData, DeleteShareGroupStateRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeShareGroupOffsetsRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, InitializeShareGroupStateRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, OffsetFetchRequestData, OffsetFetchResponseData, ProduceRequestData, ReadShareGroupStateRequestData, ReadShareGroupStateSummaryRequestData, ShareAcknowledgeRequestData, ShareFetchRequestData, ShareGroupDescribeRequestData, ShareGroupHeartbeatRequestData, StreamsGroupDescribeRequestData, StreamsGroupHeartbeatRequestData, StreamsGroupHeartbeatResponseData, SyncGroupRequestData, WriteShareGroupStateRequestData, WriteTxnMarkersRequestData}
  val streamsGroupDescribeAcl = Map(streamsGroupResource -> Set(new AccessControlEntry(clientPrincipalString, WILDCARD_HOST, DESCRIBE, ALLOW)))
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19413: extended authorizerintegrationtest to cover streamsgroupdescribe (#19981).

**Test**
Test modified in AuthorizerIntegrationTest.scala covering the corrected behaviour.
