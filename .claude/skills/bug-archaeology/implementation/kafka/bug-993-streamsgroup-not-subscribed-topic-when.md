**Commit**: 25bc5f2cfa · **Ticket**: KAFKA-19372
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
We should behave more like a consumer group and make sure to not be
subscribed to the input topics anymore when the last member leaves the
group. We don't do this right now because our topology is still
initialized even after the last member leaves the group. This will allow:
* Offsets to expire and be cleaned up. * Offsets to be deleted through admin API calls.

**Before/After**
```java
// Before
// (no removals)

// After
import static org.apache.kafka.coordinator.group.streams.StreamsGroup.StreamsGroupState.DEAD;
        if (state.get() == EMPTY || state.get() == DEAD) {
            // No topic subscriptions if the group is empty.
            // This allows offsets to expire for empty groups.
            return false;
        }
        streamsGroup.updateMember(streamsGroup.getOrCreateDefaultMember("member-id"));


        streamsGroup.removeMember("member-id");

        assertFalse(streamsGroup.isSubscribedToTopic("test-topic1"));
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19372: streamsgroup not subscribed to a topic when empty (#19901).

**Test**
Test modified in StreamsGroupTest.java covering the corrected behaviour.
