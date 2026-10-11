**Commit**: 60114a46a7 · **Ticket**: KAFKA-16822
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Abstracted code for 2 classes `ConsumerGroup` and `ConsumerGroupMember` to `ModernGroup` and `ModernGroupMember` respectively. The new abstract classes are created to share common functionality with `ShareGroup` and `ShareGroupMember` which are being introduced with KIP-932. The patch is majorly code refactoring from existing classes to abstract classes. Also created a new package called `modern` where `MemberState` class is moved, in upcoming patches, I will move common classes for `Share` and `Consumer` Group in `modern` package itself.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.consumer.MemberState;
        } else if (classicGroup.size() > consumerGroupMaxSize) {
            TargetAssignmentBuilder assignmentResultBuilder =
                new TargetAssignmentBuilder(group.groupId(), groupEpoch, consumerGroupAssignors.get(preferredServerAssignor))
                    if (classicGroup.size() > classicGroupMaxSize) {
                            " (size " + classicGroup.size() + ") is over capacity " + classicGroupMaxSize +
                    groupId, group.generationId(), group.size());

// After

    /**
     * Returns true if the member exists.
     *
     * @param memberId The member id.
     *
     * @return A boolean indicating whether the member exists or not.
     */
    boolean hasMember(String memberId);

    /**
     * Returns number of members in the group.
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16822: abstract consumer group to share functionality with share group (kip-932) (#16054).

**Test**
Test modified in CoordinatorRecordHelpersTest.java covering the corrected behaviour.
