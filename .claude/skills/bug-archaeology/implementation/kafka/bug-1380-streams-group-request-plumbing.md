**Commit**: cb7c54ccd3 · **Ticket**: KAFKA-18614
**Subsystem**: checkstyle/suppressions.xml
**Category**: Resource leak

**Root cause**
This change implements the basic RPC handling StreamsGroupHeartbeat and
StreamsGroupDescribe. This includes:
 - Adding an option to enable streams groups on the broker
- Passing describe and heartbeats to the right shard of the group
coordinator
- The handler inside the GroupMetadatManager for StreamsGroupDescribe is
fairly trivial, and is included directly in this PR. - The handler for StreamsGroupHeartbeat is complex and not included in
this PR yet. Instead, a UnsupportedOperationException is thrown.

**Before/After**
```java
// Before
              files="(RecordHelpersTest|GroupCoordinatorRecordHelpers|GroupMetadataManager|GroupMetadataManagerTest|OffsetMetadataManagerTest|GroupCoordinatorServiceTest|GroupCoordinatorShardTest|GroupCoordinatorRecordSerde|StreamsGroupTest).java"/>
import org.apache.kafka.common.message.{ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, DeleteGroupsResponseData, DescribeGroupsResponseData, DescribeShareGroupOffsetsRequestData, DescribeShareGroupOffsetsResponseData, HeartbeatRequestData, HeartbeatResponseData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupRequestData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchRequestData, OffsetFetchResponseData, ShareGroupDescribeResponseData, ShareGroupHeartbeatRequestData, ShareGroupHeartbeatResponseData, SyncGroupRequestData, SyncGroupResponseData, TxnOffsetCommitRequestData, TxnOffsetCommitResponseData}

// After
              files="(RecordHelpersTest|GroupCoordinatorRecordHelpers|GroupMetadataManager|GroupCoordinatorService|GroupMetadataManagerTest|OffsetMetadataManagerTest|GroupCoordinatorServiceTest|GroupCoordinatorShardTest|GroupCoordinatorRecordSerde|StreamsGroupTest).java"/>
import org.apache.kafka.common.message.{ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, DeleteGroupsResponseData, DescribeGroupsResponseData, DescribeShareGroupOffsetsRequestData, DescribeShareGroupOffsetsResponseData, HeartbeatRequestData, HeartbeatResponseData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupRequestData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchRequestData, OffsetFetchResponseData, ShareGroupDescribeResponseData, ShareGroupHeartbeatRequestData, ShareGroupHeartbeatResponseData, StreamsGroupDescribeResponseData, StreamsGroupHeartbeatRequestData, SyncGroupRequestData, SyncGroupResponseData, TxnOffsetCommitRequestData, TxnOffsetCommitResponseData}
import org.apache.kafka.coordinator.group.streams.StreamsGroupHeartbeatResult
  override def streamsGroupHeartbeat(
                                 context: RequestContext,
                                 request: StreamsGroupHeartbeatRequestData
                               ): CompletableFuture[StreamsGroupHeartbeatResult] = {
    FutureUtils.failedFuture(Errors.UNSUPPORTED_VERSION.exception(
      s"The old group coordinator does not support ${ApiKeys.STREAMS_GROUP_HEARTBEAT.name} API."
    ))
  }

```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18614, kafka-18613: add streams group request plumbing (#18979).

**Test**
Test modified in GroupCoordinatorAdapterTest.scala covering the corrected behaviour.
