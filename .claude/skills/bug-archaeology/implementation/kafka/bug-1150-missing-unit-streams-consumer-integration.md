**Commit**: 3fae785ea0 · **Ticket**: KAFKA-19110
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
- Construct `AsyncKafkaConsumer` constructor and verify that the
`RequestManagers.supplier()` contains Streams-specific data structures. - Verify that `RequestManagers` constructs the Streams request managers
correctly
- Test `StreamsGroupHeartbeatManager#resetPollTimer()`
- Test `StreamsOnTasksRevokedCallbackCompletedEvent`,
`StreamsOnTasksAssignedCallbackCompletedEvent`, and
`StreamsOnAllTasksLostCallbackCompletedEvent` in
`ApplicationEventProcessor`
- Test `DefaultStreamsRebalanceListener`
- Test `StreamThread`. - Test `handleStreamsRebalanceData`. - Test `StreamsRebalanceData`.

**Before/After**
```java
// Before
            Optional.empty()

// After
import static java.util.Collections.unmodifiableList;


    // visible for testing
    List<MemberStateListener> stateListeners() {
        return unmodifiableList(stateUpdatesListeners);
    }
import java.util.UUID;
        return newConsumerWithStreamRebalanceData(props, null);
    }

    private AsyncKafkaConsumer<String, String> newConsumerWithStreamRebalanceData(
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19110: add missing unit test for streams-consumer integration (#19457).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
