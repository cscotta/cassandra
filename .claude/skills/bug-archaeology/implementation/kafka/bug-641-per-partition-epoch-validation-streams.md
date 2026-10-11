**Commit**: d5d9892413 · **Ticket**: KAFKA-19779
**Subsystem**: build.gradle
**Category**: State not cleaned up

**Root cause**
This change enhances the offset commit validation logic in streams
groups to validate against per-partition assignment epochs. When a
member attempts to commit offsets with an older member epoch, the logic
now validates that the epoch is not older than the assignment epoch for
each individual partition being committed. The implementation adds a new `createAssignmentEpochValidator` method
that creates partition-level validators, checking each partition against
its assignment epoch from either assigned tasks or tasks pending
revocation. We extend the SmokeTestDriverIntegrationTest to detect if we have
processed more records than needed, which, in this restricted scenario,
should only happen wh...

**Before/After**
```java
// Before
    testImplementation project(':test-common:test-common-util')
        validateMemberEpoch(memberEpoch, member.memberEpoch());
        return CommitPartitionValidator.NO_OP;

// After
import org.apache.kafka.coordinator.group.generated.StreamsGroupTopologyValue;
        if (memberEpoch == member.memberEpoch()) {
            return CommitPartitionValidator.NO_OP;
        }

        if (memberEpoch > member.memberEpoch()) {
            throw new StaleMemberEpochException(String.format("Received member epoch %d is newer than " +
                "current member epoch %d.", memberEpoch, member.memberEpoch()));
        }

        // Member epoch is older; validate against per-partition assignment epochs.
        return createAssignmentEpochValidator(member, memberEpoch);
```

**How to spot**
When reviewing build.gradle code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19779: add per-partition epoch validation to streams groups  [4/n] (#20760).

**Test**
Test modified in OffsetMetadataManagerTest.java covering the corrected behaviour.
