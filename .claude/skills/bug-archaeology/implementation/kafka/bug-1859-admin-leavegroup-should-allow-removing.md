**Commit**: 57737a357f · **Ticket**: KAFKA-18188
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
The LeaveGroup API is used by the admin client to remove static members or remove all members from the group. The latter does not work because the API does not allow removing a member using the CONSUMER protocol by member id. Moreover, the response should only include the member id if the member id was included in the request. This patch fixes both issues.

**Before/After**
```java
// Before
      val memberId = Uuid.randomUuid().toString
      assertEquals(Errors.NONE.code, consumerGroupHeartbeat(
        groupId = "group",
        memberId = memberId,
        memberEpoch = 0,
        instanceId = "instance-id",
        rebalanceTimeoutMs = 5 * 60 * 1000,
        subscribedTopicNames = List("foo"),
        topicPartitions = List.empty,
      ).errorCode)
              .setMemberId(memberId)
              .setGroupInstanceId("instance-id")

// After
    def instanceId(memberId: String): String = "instance_" + memberId
    val memberIds = Range(0, 3).map { __ =>
      Uuid.randomUuid().toString
    }

      // Join with all the members.
      memberIds.foreach { memberId =>
        assertEquals(Errors.NONE.code, consumerGroupHeartbeat(
          groupId = "group",
          memberId = memberId,
          memberEpoch = 0,
          instanceId = instanceId(memberId),
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18188; admin leavegroup should allow removing member using consumer protocol by member id (#18116).

**Test**
Test modified in LeaveGroupRequestTest.scala covering the corrected behaviour.
