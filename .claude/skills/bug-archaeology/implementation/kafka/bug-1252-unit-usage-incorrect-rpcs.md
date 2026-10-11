**Commit**: e3809682e2 · **Ticket**: KAFKA-18613
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
In the GroupMetadataManager, we may call an RPC on an incorrect group
type. This adds unit tests to validate the behavior when an RPC is used
on an incorrect group type. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import java.util.Collections;
    @Test
    public void testConsumerGroupHeartbeatOnStreamsGroup() {
        String groupId = "group-foo";
        String memberId = Uuid.randomUuid().toString();

        GroupMetadataManagerTestContext context = new GroupMetadataManagerTestContext.Builder()
            .withMetadataImage(MetadataImage.EMPTY)
            .withStreamsGroup(new StreamsGroupBuilder(groupId, 1)
                .withMember(StreamsGroupMember.Builder.withDefaults(memberId)
                    .setState(org.apache.kafka.coordinator.group.streams.MemberState.STABLE)
                    .setMemberEpoch(1)
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18613: unit tests for usage of incorrect rpcs (#18383).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
