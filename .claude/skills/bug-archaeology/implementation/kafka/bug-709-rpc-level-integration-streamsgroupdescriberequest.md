**Commit**: 2938c4242e · **Ticket**: KAFKA-19754
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Test the `StreamsGroupDescribeRequest` RPC and corresponding responses
for situations where
- `streams.version` not upgraded to 1
- `streams.version` enabled, multiple groups listening to the same
topic. Reviewers: Lucas Brutschy <lucasbru@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AddOffsetsToTxnResponseData, ConsumerGroupDescribeRequestData, ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, DeleteGroupsRequestData, DeleteGroupsResponseData, DescribeGroupsRequestData, DescribeGroupsResponseData, EndTxnRequestData, HeartbeatRequestData, HeartbeatResponseData, InitProducerIdRequestData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchRequestData, OffsetFetchResponseData, ShareGroupDescribeRequestData, ShareGroupDescribeResponseData, ShareGroupHeartbeatRequestData, ShareGroupHeartbeatResponseData, SyncGroupRequestData, SyncGroupResponseData, TxnOffsetCommitRequestData, TxnOffsetCommitResponseData, WriteTxnMarkersRequestData}
import org.apache.kafka.common.requests.{AbstractRequest, AbstractResponse, AddOffsetsToTxnRequest, AddOffsetsToTxnResponse, ConsumerGroupDescribeRequest, ConsumerGroupDescribeResponse, ConsumerGroupHeartbeatRequest, ConsumerGroupHeartbeatResponse, DeleteGroupsRequest, DeleteGroupsResponse, DescribeGroupsRequest, DescribeGroupsResponse, EndTxnRequest, EndTxnResponse, HeartbeatRequest, HeartbeatResponse, InitProducerIdRequest, InitProducerIdResponse, JoinGroupRequest, JoinGroupResponse, LeaveGroupRequest, LeaveGroupResponse, ListGroupsRequest, ListGroupsResponse, OffsetCommitRequest, OffsetCommitResponse, OffsetDeleteRequest, OffsetDeleteResponse, OffsetFetchRequest, OffsetFetchResponse, ShareGroupDescribeRequest, ShareGroupDescribeResponse, ShareGroupHeartbeatRequest, ShareGroupHeartbeatResponse, SyncGroupRequest, SyncGroupResponse, TxnOffsetCommitRequest, TxnOffsetCommitResponse, WriteTxnMarkersRequest, WriteTxnMarkersResponse}

// After
import org.apache.kafka.common.message.{AddOffsetsToTxnRequestData, AddOffsetsToTxnResponseData, ConsumerGroupDescribeRequestData, ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, DeleteGroupsRequestData, DeleteGroupsResponseData, DescribeGroupsRequestData, DescribeGroupsResponseData, EndTxnRequestData, HeartbeatRequestData, HeartbeatResponseData, InitProducerIdRequestData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchRequestData, OffsetFetchResponseData, ShareGroupDescribeRequestData, ShareGroupDescribeResponseData, ShareGroupHeartbeatRequestData, ShareGroupHeartbeatResponseData, StreamsGroupDescribeRequestData, StreamsGroupDescribeResponseData, StreamsGroupHeartbeatRequestData, StreamsGroupHeartbeatResponseData, SyncGroupRequestData, SyncGroupResponseData, TxnOffsetCommitRequestData, TxnOffsetCommitResponseData, WriteTxnMarkersRequestData}
import org.apache.kafka.common.requests.{AbstractRequest, AbstractResponse, AddOffsetsToTxnRequest, AddOffsetsToTxnResponse, ConsumerGroupDescribeRequest, ConsumerGroupDescribeResponse, ConsumerGroupHeartbeatRequest, ConsumerGroupHeartbeatResponse, DeleteGroupsRequest, DeleteGroupsResponse, DescribeGroupsRequest, DescribeGroupsResponse, EndTxnRequest, EndTxnResponse, HeartbeatRequest, HeartbeatResponse, InitProducerIdRequest, InitProducerIdResponse, JoinGroupRequest, JoinGroupResponse, LeaveGroupRequest, LeaveGroupResponse, ListGroupsRequest, ListGroupsResponse, OffsetCommitRequest, OffsetCommitResponse, OffsetDeleteRequest, OffsetDeleteResponse, OffsetFetchRequest, OffsetFetchResponse, ShareGroupDescribeRequest, ShareGroupDescribeResponse, ShareGroupHeartbeatRequest, ShareGroupHeartbeatResponse, StreamsGroupDescribeRequest, StreamsGroupDescribeResponse, StreamsGroupHeartbeatRequest, StreamsGroupHeartbeatResponse, SyncGroupRequest, SyncGroupResponse, TxnOffsetCommitRequest, TxnOffsetCommitResponse, WriteTxnMarkersRequest, WriteTxnMarkersResponse}
  protected def streamsGroupDescribe(
    groupIds: List[String],
    includeAuthorizedOperations: Boolean = false,
    version: Short = ApiKeys.STREAMS_GROUP_DESCRIBE.latestVersion(isUnstableApiEnabled)
  ): List[StreamsGroupDescribeResponseData.DescribedGroup] = {
    val streamsGroupDescribeRequest
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19754: add rpc-level integration test for streamsgroupdescriberequest  (#20632).

**Test**
Test modified in GroupCoordinatorBaseRequestTest.scala covering the corrected behaviour.
