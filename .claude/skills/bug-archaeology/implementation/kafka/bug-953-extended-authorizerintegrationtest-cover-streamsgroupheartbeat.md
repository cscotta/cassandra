**Commit**: 31a7e01769 · **Ticket**: KAFKA-19412
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Extending test coverage of authorization for streams group RPC
StreamsGroupHeartbeat. The RPC requires READ GROUP and DESCRIBE TOPIC
permissions for all topics. For creating internal topics, we require
CREATE TOPIC permission. If internal topic  creation fails, the request
does not fail, but the status reflects this problem.

**Before/After**
```java
// Before
              responseData.status().add(
                new StreamsGroupHeartbeatResponseData.Status()
                  .setStatusCode(StreamsGroupHeartbeatResponse.Status.MISSING_INTERNAL_TOPICS.code())
                  .setStatusDetail("Unauthorized to CREATE on topics " + createTopicUnauthorized.mkString(",") + ".")
              )
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AlterPartitionReassignmentsRequestData, AlterReplicaLogDirsRequestData, AlterShareGroupOffsetsRequestData, ConsumerGroupDescribeRequestData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, CreateAclsRequestData, CreatePartitionsRequestData, CreateTopicsRequestData, DeleteAclsRequestData, DeleteGroupsRequestData, DeleteRecordsRequestData, DeleteShareGroupOffsetsRequestData, DeleteShareGroupStateRequestData, DeleteTopicsRequestData, DescribeClusterRequestData, DescribeConfigsRequestData, DescribeGroupsRequestData, DescribeLogDirsRequestData, DescribeProducersRequestData, DescribeShareGroupOffsetsRequestData, DescribeTransactionsRequestData, FetchResponseData, FindCoordinatorRequestData, HeartbeatRequestData, IncrementalAlterConfigsRequestData, InitializeShareGroupStateRequestData, JoinGroupRequestData, ListPartitionReassignmentsRequestData, ListTransactionsRequestData, MetadataRequestData, OffsetCommitRequestData, OffsetFetchRequestData, OffsetFetchResponseData, ProduceRequestData, ReadShareGroupStateRequestData, ReadShareGroupStateSummaryRequestData,

// After
              val missingInternalTopicStatus =
                responseData.status().stream().filter(x => x.statusCode() == StreamsGroupHeartbeatResponse.Status.MISSING_INTERNAL_TOPICS.code()).findFirst()
              if (missingInternalTopicStatus.isPresent) {
                missingInternalTopicStatus.get().setStatusDetail(
                  missingInternalTopicStatus.get().statusDetail() + "; Unauthorized to CREATE on topics " + createTopicUnauthorized.mkString(", ") + "."
                )
              } else {
                responseData.status().add(
                  new StreamsGroupHeartbeatResponseData.Status()
                    .setStatusCode(StreamsGroupHeartbeatResponse.Status.MISSING_INTERNAL_TOPICS.code())
                    .setStatusDetail("Unauthorized to CREATE on topics " + createTopicUnauthorized.mkString(", ") + ".")
                )
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19412: extended authorizerintegrationtest to cover streamsgroupheartbeat (#19978).

**Test**
Test modified in AbstractAuthorizerIntegrationTest.scala covering the corrected behaviour.
