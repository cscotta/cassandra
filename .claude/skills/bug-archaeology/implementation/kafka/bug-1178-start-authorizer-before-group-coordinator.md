**Commit**: 4b2a3102da · **Ticket**: KAFKA-19147
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
[KAFKA-18813](https://issues.apache.org/jira/browse/KAFKA-18813) added
`Topic:Describe` authorization of topics matching regex patterns to the
group coordinator since it was difficult to authorize these in the
broker when processing consumer heartbeats using the new protocol. But
group coordinator is started in `BrokerServer` before the authorizer is
created. And hence group coordinator doesn't have an authorizer and
never performs authorization. As a result, topics that are not
authorized for `Describe` may be assigned to consumers.

**Before/After**
```java
// Before
      // Create and initialize an authorizer if one is configured.
      authorizerPlugin = config.createNewAuthorizer(metrics, ProcessRole.BrokerRole.toString)

import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, ProduceRequestData, SyncGroupRequestData, WriteTxnMarkersRequestData}

// After
      // Create and initialize an authorizer if one is configured.
      authorizerPlugin = config.createNewAuthorizer(metrics, ProcessRole.BrokerRole.toString)

import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, ProduceRequestData, SyncGroupRequestData, WriteTxnMarkersRequestData}
import org.apache.kafka.common.network.ListenerName
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19147: start authorizer before group coordinator to ensure coordinator authorizes regex topics (#19488).

**Test**
Test modified in AuthorizerIntegrationTest.scala covering the corrected behaviour.
