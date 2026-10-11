**Commit**: 4ea29da3ca · **Ticket**: KAFKA-20273
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
When assignment batching or offload are enabled, the target assignment
can lag behind member subscriptions. When downgrading a consumer group
to a classic group, we must check if the target assignment is stale and
enter the PREPARING_REBALANCE state if needed. Downgrades triggered by a
member leaving the group already trigger a rebalance, so there is no
change on that path. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
     * @param hasSubscriptionChanged    The boolean indicating whether the joining member has a different subscription
     *                                  from the replaced member. Only used when joiningMember is set.
        boolean hasSubscriptionChanged,
        // If the downgrade is triggered by a member leaving the group or a static
        // member replacement with a different subscription, a rebalance should be triggered.
        if (joiningMember == null) {
            prepareRebalance(classicGroup, String.format("Downgrade group %s from consumer to classic for member leaving.", classicGroup.groupId()));
        } else if (hasSubscriptionChanged) {
            prepareRebalance(classicGroup, String.format("Downgrade group %s from consumer to classic for static member replacement with different subscription.", classicGroup.groupId()));
            if (!bumpGroupEpoch) {
                bumpGroupEpoch,
            convertToClassicGroup(group, members, null, false, records);

// After
     * @param rebalance                 Whether to trigger a rebalance after the downgrade.
     * @param rebalanceReason           The reason for the rebalance, if {@code rebalance} is {@code true}.
        boolean rebalance,
        String rebalanceReason,
        if (rebalance) {
            prepareRebalance(classicGroup, rebalanceReason);
            boolean rebalance = group.assignmentEpoch() < groupEpoch;
            if (!rebalance) {
                rebalance,
                rebalance ?
                    String.format("Downgrade group %s from consumer to classic with stale assignment.", group.groupId()) :
                    null,
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20273: rebalance on classic group downgrade with stale assignment (#21663).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
