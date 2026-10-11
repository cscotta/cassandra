**Commit**: 96196bb03b · **Ticket**: KAFKA-18736
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Adds pollOnClose() and maximumTimeToWait() to the Streams 
group heartbeat request manager. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import static org.apache.kafka.clients.consumer.internals.NetworkClientDelegate.PollResult.EMPTY;

    /**
     * Generate a heartbeat request to leave the group if the state is still LEAVING when this is
     * called to close the consumer.
     * <p/>
     * Note that when closing the consumer, even though an event to Unsubscribe is generated
     * (triggers callbacks and sends leave group), it could be the case that the Unsubscribe event
     * processing does not complete in time and moves on to close the managers (ex. calls to
     * close with zero timeout). So we could end up on this pollOnClose with the member in
     * {@link MemberState#PREPARE_LEAVING} (ex. app thread did not have the time to process the
     * event to execute callbacks), or {@link MemberState#LEAVING} (ex. the leave request could
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18736: add pollonclose() and maximumtimetowait() (#19233).

**Test**
Test modified in StreamsGroupHeartbeatRequestManagerTest.java covering the corrected behaviour.
