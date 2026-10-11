**Commit**: cbea4f69bd · **Ticket**: KAFKA-19546
**Subsystem**: group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java
**Category**: Logic error in condition

**Root cause**
During online protocol downgrade (consumer group → classic group), when a static member with the same instance ID replaced the last consumer-protocol member, the code checked only whether a member was leaving (`joiningMember == null`) to decide whether to trigger a rebalance. If the replacing static member had a different topic subscription than the replaced member, no rebalance was triggered, so the new member's subscription change was silently ignored and partitions could remain incorrectly assigned.

**Before/After**
```java
// BEFORE – rebalance only on member leave, not on subscription change
if (joiningMember == null) {
    prepareRebalance(classicGroup, "Downgrade group ...");
}

// AFTER – also triggers rebalance when subscription changed
if (joiningMember == null) {
    prepareRebalance(classicGroup, "Downgrade ... for member leaving.");
} else if (hasSubscriptionChanged) {
    prepareRebalance(classicGroup, "Downgrade ... for static member replacement with different subscription.");
}
```

**How to spot**
Multi-condition rebalance triggers are prone to missing branches. Whenever a protocol downgrade is implemented, enumerate all conditions that should cause a rebalance (member leave, member join with different subscription, group epoch change) and verify each has an explicit branch.

**Test**
Existing downgrade integration tests were extended; the `convertToClassicGroup` method now takes an explicit `hasSubscriptionChanged` boolean parameter that is threaded through from the join handler.
