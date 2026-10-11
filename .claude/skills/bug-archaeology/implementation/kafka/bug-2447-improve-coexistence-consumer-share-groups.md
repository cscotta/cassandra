**Commit**: b0d0956b20 · **Ticket**: KAFKA-17425
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This PR ensures that using the various group RPCs work properly when issued against the wrong type of group, such as DescribeConsumerGroups for a share group, or ConsumerGroupHeartbeat for a share group. There are no changes to the RPC error codes required. The significant code changes are:

Making sure that the group coordinator does not assume that only classic and consumer groups exist. This was the cause of a ClassCastException when ConsumerGroupHeartbeat was being used against a share group.

**Before/After**
```java
// Before
            } else if (createIfNotExists && validateOnlineUpgrade((ClassicGroup) group)) {
        if (group != null && group.type() == CONSUMER && !group.isEmpty()) {
            // classicGroupJoinToConsumerGroup takes the join requests to non-empty consumer groups.
            // The empty consumer groups should be converted to classic groups in classicGroupJoinToClassicGroup.
            return classicGroupJoinToConsumerGroup((ConsumerGroup) group, context, request, responseFuture);
        } else {
        } else {
        } else {

// After
            } else if (createIfNotExists && group.type() == CLASSIC && validateOnlineUpgrade((ClassicGroup) group)) {
        if (group != null) {
            if (group.type() == CONSUMER && !group.isEmpty()) {
                // classicGroupJoinToConsumerGroup takes the join requests to non-empty consumer groups.
                // The empty consumer groups should be converted to classic groups in classicGroupJoinToClassicGroup.
                return classicGroupJoinToConsumerGroup((ConsumerGroup) group, context, request, responseFuture);
            } else if (group.type() == CONSUMER || group.type() == CLASSIC) {
                return classicGroupJoinToClassicGroup(context, request, responseFuture);
            } else {
                // Group exists but it's not a consumer group
                responseFuture.complete(new JoinGroupResponseData()
                    .setMemberId(UNKNOWN_MEMBER_ID)
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17425: improve coexistence of consumer and share groups (#17039).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
