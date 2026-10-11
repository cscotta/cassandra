**Commit**: 2a05c0cdd6 · **Ticket**: KAFKA-19001
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Implements the use of session timeout, standby tasks and heartbeat
interval
configurations in the streams group heartbeat. Piggy-backed is another test that streams groups react to changes
in the topic metadata. Reviewers: Bill Bejeck <bbejeck@apache.org>, Bruno Cadonna
<cadonna@apache.org>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
        final StreamsGroup group = isJoining ? getOrCreateStreamsGroup(groupId) : getStreamsGroupOrThrow(groupId);
        return 45000;
        return 5000;
        return Map.of("group.streams.num.standby.replicas", "0");


// After
    /**
     * Checks whether the streams group can accept a new member or not based on the
     * max group size defined.
     *
     * @param group     The streams group.
     *
     * @throws GroupMaxSizeReachedException if the maximum capacity has been reached.
     */
    private void throwIfStreamsGroupIsFull(
        StreamsGroup group
    ) throws GroupMaxSizeReachedException {
        // If the streams group has reached its maximum capacity, the member is rejected if it is not
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19001: use streams group-level configurations in heartbeat (#19219).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
