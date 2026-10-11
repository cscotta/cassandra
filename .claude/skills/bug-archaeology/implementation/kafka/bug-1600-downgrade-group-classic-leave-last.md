**Commit**: 65b93fa8d5 · **Ticket**: KAFKA-18150
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
     * Validates the online downgrade if a consumer member is fenced from the consumer group.
     * @param fencedMemberId    The fenced member id.
    private boolean validateOnlineDowngradeWithFencedMember(ConsumerGroup consumerGroup, String fencedMemberId) {
        if (!consumerGroup.allMembersUseClassicProtocolExcept(fencedMemberId)) {
        } else if (consumerGroup.numMembers() <= 1) {
        } else if (consumerGroup.numMembers() - 1 > config.classicGroupMaxSize()) {
     * @param replacedMemberId  The replaced member id.
    private boolean validateOnlineDowngradeWithReplacedMemberId(
        String replacedMemberId
        if (!consumerGroup.allMembersUseClassicProtocolExcept(replacedMemberId)) {
     * @param leavingMemberId   The leaving member that triggers the downgrade validation.
        String leavingMemberId,

// After
     * Validates the online downgrade if consumer members are fenced from the consumer group.
     * @param fencedMembers     The fenced members.
    private boolean validateOnlineDowngradeWithFencedMembers(ConsumerGroup consumerGroup, Set<ConsumerGroupMember> fencedMembers) {
        if (!consumerGroup.allMembersUseClassicProtocolExcept(fencedMembers)) {
        } else if (consumerGroup.numMembers() - fencedMembers.size() <= 0) {
        } else if (consumerGroup.numMembers() - fencedMembers.size() > config.classicGroupMaxSize()) {
     * @param replacedMember    The replaced member.
    private boolean validateOnlineDowngradeWithReplacedMember(
        ConsumerGroupMember replacedMember
        if (!consumerGroup.allMembersUseClassicProtocolExcept(replacedMember)) {
     * @param leavingMembers    The leaving member(s) that triggered the downgrade validation.
     *                          When not null, must have an instanceId that matches an existing member.
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18150; downgrade group on classic leave of last consumer member (#18224).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
