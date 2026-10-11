**Commit**: 366aa1014c · **Ticket**: KAFKA-17317
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This implementation doesn't change the existing downgrade path. In `classicGroupJoinToConsumerGroup`, if the group should be downgraded, it will be converted to a classic group at the end of the method. The returned records will be the records from GroupJoin plus the records from conversion. No rebalance will be triggered in the newly converted group.

**Before/After**
```java
// Before
     * @param consumerGroup The ConsumerGroup.
     * @param memberId      The fenced member id.
    private boolean validateOnlineDowngrade(ConsumerGroup consumerGroup, String memberId) {
        if (!consumerGroup.allMembersUseClassicProtocolExcept(memberId)) {
     * @param response          The response of the returned CoordinatorResult.
     * @return A CoordinatorResult.
    private <T> CoordinatorResult<T, CoordinatorRecord> convertToClassicGroup(
        T response
        List<CoordinatorRecord> records = new ArrayList<>();
        consumerGroup.createGroupTombstoneRecords(records);
        // Set the appendFuture to prevent the records from being replayed.
        prepareRebalance(classicGroup, String.format("Downgrade group %s from consumer to classic.", classicGroup.groupId()));

// After
     * @param consumerGroup     The ConsumerGroup.
     * @param fencedMemberId    The fenced member id.
    private boolean validateOnlineDowngradeWithFencedMember(ConsumerGroup consumerGroup, String fencedMemberId) {
        if (!consumerGroup.allMembersUseClassicProtocolExcept(fencedMemberId)) {
    /**
     * Validates whether the group id is eligible for an online downgrade if an existing
     * static member is replaced by another new one uses the classic protocol.
     *
     * @param consumerGroup     The group to downgrade.
     * @param replacedMemberId  The replaced member id.
     *
     * @return A boolean indicating whether it's valid to online downgrade the consumer group.
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular:  kafka-17317; validate and maybe trigger downgrade after static member replacement (#17306).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
