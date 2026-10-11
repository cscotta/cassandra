**Commit**: cbea4f69bd · **Ticket**: KAFKA-19546
**Subsystem**: group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java
**Category**: Logic error in condition

**Root cause**
During an online downgrade from the consumer protocol to the classic protocol triggered by a static member replacement, `convertToClassicGroup` was called unconditionally without a rebalance when `joiningMember != null`. The code assumed the replacement member had the same subscription as the replaced member. When the subscription had actually changed, the new classic group started with a stale assignment and no rebalance was triggered to correct it. The fix threads a `hasSubscriptionChanged` boolean through `convertToClassicGroup` and triggers `prepareRebalance` when both `joiningMember != null` and the subscription has changed.

**Before/After**
```java
// Before: no rebalance on static member replacement, even with different subscription
if (joiningMember == null) {
    prepareRebalance(classicGroup, "Downgrade group ...");
}
// (joiningMember != null → no rebalance regardless of subscription change)

// After: rebalance triggered when subscription changed
if (joiningMember == null) {
    prepareRebalance(classicGroup, "Downgrade ... for member leaving.");
} else if (hasSubscriptionChanged) {
    prepareRebalance(classicGroup, "Downgrade ... for static member replacement with different subscription.");
}
```

**How to spot**
Wherever a code path has a branch for "replacement member" that skips a state transition that the "leaving member" path would have triggered, verify whether the replacement member's attributes (subscription, configuration) can differ from the replaced member's. If so, the state transition may still be needed.

**Test**
`testLastStaticConsumerProtocolMemberReplacedByClassicProtocolMember` was converted to a `@ParameterizedTest` with `@ValueSource(booleans = {true, false})`. When `isSubscriptionChanged=true`, the test asserts the classic group enters `PREPARING_REBALANCE`; when `false`, no rebalance is triggered.
