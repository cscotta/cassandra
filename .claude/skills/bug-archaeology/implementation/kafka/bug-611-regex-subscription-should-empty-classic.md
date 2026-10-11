**Commit**: 0f4dbf7fd7 · **Ticket**: KAFKA-19720
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
We don't recompute the assignment on consumer -> classic member
replacement when the consumer member had a regex subscription and the
classic member does not. This PR sets regex subscription to empty during the replacement. Reviewers: Sean Quah <squah@confluent.io>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
        boolean bumpGroupEpoch = hasMemberSubscriptionChanged(

// After
            .setSubscribedTopicRegex("") // Regex subscription is not supported for classic member.
        boolean hasMemberSubscriptionChanged = hasMemberSubscriptionChanged(
        // Maybe create tombstone for the regex if the joining member replaces a static member
        // with regex subscription.
        UpdateRegularExpressionsResult updateRegularExpressionsResult = maybeUpdateRegularExpressions(
            context,
            group,
            member,
            updatedMember,
            records
        );

```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19720: regex subscription should be empty for classic members joining mixed group (#20626).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
