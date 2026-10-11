**Commit**: 5c8e3c58da · **Ticket**: KAFKA-20268
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Once assignment offload is implemented, newly created groups may not
have their first assignment at epoch 1 available yet. We cannot give the
member an epoch of 0, since it is reserved for new members joining a
group. The new member's epoch must be greater than 0 and less than the
group epoch. To resolve this, we initialize groups at epoch 1 with target assignment
epoch 1.

**Before/After**
```java
// Before
          assertEquals(Optional.of(3), testGroupDescription.groupEpoch)
          assertEquals(Optional.of(3), testGroupDescription.targetAssignmentEpoch)
          .setGroupEpoch(1)
          .setAssignmentEpoch(1)
    assertEquals(1, consumerGroupHeartbeatResponse.data.memberEpoch)
    assertEquals(2, consumerGroupHeartbeatResponse.data.memberEpoch)
    assertEquals(1, consumerGroupHeartbeatResponse.data.memberEpoch)
    assertEquals(2, consumerGroupHeartbeatResponse.data.memberEpoch)
    assertEquals(2, consumerGroupHeartbeatResponse.data.memberEpoch)
    ass

// After
          assertEquals(Optional.of(4), testGroupDescription.groupEpoch)
          assertEquals(Optional.of(4), testGroupDescription.targetAssignmentEpoch)
          .setGroupEpoch(2)
          .setAssignmentEpoch(2)
    assertEquals(2, consumerGroupHeartbeatResponse.data.memberEpoch)
    assertEquals(3, consumerGroupHeartbeatResponse.data.memberEpoch)
    assertEquals(2, consumerGroupHeartbeatResponse.data.memberEpoch)
    assertEquals(3, consumerGroupHeartbeatResponse.data.memberEpoch)
    assertEquals(3, consumerGroupHeartbeatResponse.data.memberEpoch)
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20268: initialize group epochs to 1 for assignment offload (#21700).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
