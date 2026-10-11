**Commit**: 0707c1fde2 · **Ticket**: KAFKA-17908
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    " is owned by member {}.", groupId, receivedMemberId, receivedInstanceId, member.memberId());
                log.info("[GroupId {}] Static Member {} with instance id {} temporarily left the consumer group.",
                log.info("[GroupId {}] Static Member {} with instance id {} left the consumer group.",
            () -> consumerGroupFenceMemberOperation(groupId, memberId, "the member session expired.")
            () -> shareGroupFenceMemberOperation(groupId, memberId, "the member session expired.")
            () -> consumerGroupFenceMemberOperation(groupId, memberId, "the classic member failed to join within the rebalance timeout.")
            () -> consumerGroupFenceMemberOperation(groupId, memberId, "the member failed to sync within timeout.")
                    log.info("[Group {}] Dynamic Member {} has left group " +
                    log.info("[Group {}] Static Member {} with instance id {} has left group " +

// After
                    " owned by member {}.", groupId, receivedMemberId, receivedInstanceId, member.memberId());
                log.info("[GroupId {}] Static member {} with instance id {} temporarily left the consumer group.",
                log.info("[GroupId {}] Static member {} with instance id {} left the consumer group.",
            () -> consumerGroupFenceMemberOperation(groupId, memberId, "the member session expired")
            () -> shareGroupFenceMemberOperation(groupId, memberId, "the member session expired")
            () -> consumerGroupFenceMemberOperation(groupId, memberId, "the classic member failed to join within the rebalance timeout")
            () -> consumerGroupFenceMemberOperation(groupId, memberId, "the member failed to sync within timeout")
                    log.info("[Group {}] Dynamic member {} has left group " +
                    log.info("[Group {}] Static member {} with instance id {} has left group " +
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17908 tweak log messages in group metadata manager (#17652).

**Test**
No test added.
