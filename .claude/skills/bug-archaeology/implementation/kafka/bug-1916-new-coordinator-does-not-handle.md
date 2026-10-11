**Commit**: ac8b3dfbf0 · **Ticket**: KAFKA-18060
**Subsystem**: group-coordinator/src
**Category**: Resource leak

**Root cause**
There are two issues in KAFKA-18060:

1) New coordinator can't handle the TxnOffsetCommitRequest with empty member id, and TxnOffsetCommitRequest v0-v2 do definitely has an empty member ID, causing ConsumerGroup#validateOffsetCommit to throw an UnknownMemberIdException. This prevents the old producer from calling sendOffsetsToTransaction. Note that TxnOffsetCommitRequest versions v0-v2 are included in KIP-896, so it seems the new coordinator should support that operations

2) The deprecated API Producer#sendOffsetsToTransaction does not use v0-v2 to send TxnOffsetCommitRequest with an empty member ID. Unfortunately, it has been released for a while.

**Before/After**
```java
// Before
import org.apache.kafka.common.message.{ConsumerGroupDescribeRequestData, ConsumerGroupDescribeResponseData, ConsumerGroupHeartbeatRequestData, ConsumerGroupHeartbeatResponseData, DeleteGroupsRequestData, DeleteGroupsResponseData, DescribeGroupsRequestData, DescribeGroupsResponseData, HeartbeatRequestData, HeartbeatResponseData, JoinGroupRequestData, JoinGroupResponseData, LeaveGroupResponseData, ListGroupsRequestData, ListGroupsResponseData, OffsetCommitRequestData, OffsetCommitResponseData, OffsetDeleteRequestData, OffsetDeleteResponseData, OffsetFetchResponseData, ShareGroupDescribeRequestData, ShareGroupDescribeResponseData, ShareGroupHeartbeatRequestData, ShareGroupHeartbeatResponseData, SyncGroupRequestData, SyncGroupResponseData}
import org.apache.kafka.common

// After
import org.apache.kafka.common.errors.UnsupportedVersionException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

    @Test
    public void testVersionSupportForGroupMetadata() {
        for (short version : ApiKeys.TXN_OFFSET_COMMIT.allVersions()) {
            assertDoesNotThrow(() -> builder.build(version));
            if (version >= 3) {
                assertDoesNotThrow(() -> builderWithGroupMetadata.build(version));
            } else {
                assertEquals("Broker doesn't support group metadata commit API on version " + version +
```

**How to spot**
When reviewing group-coordinator/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18060 new coordinator does not handle txnoffsetcommitrequest with empty member id when using consumer group (#1791.

**Test**
Test modified in TxnOffsetCommitRequestTest.java covering the corrected behaviour.
