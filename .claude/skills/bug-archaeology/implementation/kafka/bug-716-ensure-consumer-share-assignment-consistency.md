**Commit**: 71a7d85955 · **Ticket**: KAFKA-19431
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Filter out unsubscribed topics during reconciliation. This eliminates the window where a consumer group assignment could
contain unsubscribed topics when a member unsubscribes from a topic
while it has unrevoked partitions. We also apply filtering in a few other cases that would arise when
client-side assignors are implemented, since new assignments would no
longer be available immediately. This is important for mixed groups,
since clients on the classic protocol will rejoin if they receive a
topic in their assignment that is no longer in their subscription.

**Before/After**
```java
// Before
        // If the group is newly created, we must ensure that it moves away from
        // epoch 0 and that it is fully initialized.
        boolean bumpGroupEpoch = group.groupEpoch() == 0;

        bumpGroupEpoch |= hasMemberSubscriptionChanged(

        bumpGroupEpoch |= maybeUpdateRegularExpressions(

// After
        boolean subscribedTopicNamesChanged = hasMemberSubscriptionChanged(
        UpdateRegularExpressionsResult updateRegularExpressionsResult = maybeUpdateRegularExpressions(
        // The subscription has changed when either the subscribed topic names or subscribed topic
        // regex has changed.
        boolean hasSubscriptionChanged = subscribedTopicNamesChanged || updateRegularExpressionsResult.regexUpdated();
        boolean bumpGroupEpoch =
            // If the group is newly created, we must ensure that it moves away from
            // epoch 0 and that it is fully initialized.
            groupEpoch == 0 ||
            // Bumping the group epoch signals that the target assignment should be updated. We bump
            // the group epoch when the member has changed its subscribed topic names or the member
            // has changed its subscribed topic regex to a regex that is already resolved. We avoid
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19431: ensure consumer and share assignment consistency with subscriptions (#20055).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
