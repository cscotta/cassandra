**Commit**: 8ff7addcec · **Ticket**: KAFKA-19974
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Previously, when transitioning from UNREVOKED_PARTITIONS to
UNREVOKED_PARTITIONS, we bumped the member epoch by 1. After the change
for KAFKA-19431, we could also enter UNREVOKED_PARTITIONS due to
unsubscribing from a topic. This introduced a new class of epoch bumps,
which allowed a partition to be owned by different members at the same
epoch (though not at the same time). We would like to preserve the
invariant that partitions can only be owned by a single member at any
given epoch, so we remove the partition bump entirely.

**Before/After**
```java
// Before
                // When the member has revoked all the pending partitions, it can
                // transition to the next epoch (current + 1) and we can reconcile
                // its state towards the latest target assignment.
                    // When we enter UNREVOKED_PARTITIONS due to a subscription change,
                    // we must not advance the member epoch when the new target
                    // assignment is not available yet.
                    Math.min(member.memberEpoch() + 1, targetAssignmentEpoch),
    @ParameterizedTest
    @CsvSource({
        "10, 12, 11",
        "10, 10, 10", // The member epoch must not advance past the target assignment epoch.
    })

// After
                // When the member has revoked all the pending partitions, we can
                // reconcile its state towards the latest target assignment.
                    member.memberEpoch(),
    @Test
    public void testUnrevokedPartitionsToUnrevokedPartitions() {
            .setMemberEpoch(10)
            .setPreviousMemberEpoch(10)
            .withTargetAssignment(12, new Assignment(mkAssignment(
                .setMemberEpoch(10)
                .setPreviousMemberEpoch(10)
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19974: do not bump member epoch when exiting unrevoked_partitions (#21107).

**Test**
Test modified in CurrentAssignmentBuilderTest.java covering the corrected behaviour.
