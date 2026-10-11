**Commit**: 6744a718c2 · **Ticket**: KAFKA-17066
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Fix for the known issue that the logic for updating fetch positions in the new consumer was being performed partly in the app thread, party in the background thread, potentially leading to race conditions on the subscription state. This PR moves the logic for updateFetchPositions to the background thread as a single event (instead of triggering separate events to validate, fetchOffsets, listOffsets). A new UpdateFetchPositionsEvent is triggered from the app thread and processed in the background, where it performs those same operations and updates the subscription state accordingly, without blocking the background thread. This PR maintains the existing logic for keeping a pendingOffsetFetchR...

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.internals.events.ResetPositionsEvent;
import org.apache.kafka.clients.consumer.internals.events.ValidatePositionsEvent;
import static org.apache.kafka.clients.consumer.internals.ConsumerUtils.refreshCommittedOffsets;
    private FetchCommittedOffsetsEvent pendingOffsetFetchEvent;

     * @throws AuthenticationException If authentication fails. See the exception for more details
     * @throws NoOffsetForPartitionException If no offset is stored for a given partition and no offset reset policy is
     *             defined
            // Validate positions using the partition leader end offsets, to detect if any partition
            // has been truncated due to a leader change. This will trigger an OffsetForLeaderEpoch
            // request, retrieve the partition end offsets, and validate the current position against it.
            applicationEventHandler.addAndGet(ne

// After
import org.apache.kafka.clients.consumer.internals.events.CheckAndUpdatePositionsEvent;

     * @throws AuthenticationException       If authentication fails. See the exception for more details
     * @throws NoOffsetForPartitionException If no offset is stored for a given partition and no offset reset policy is
     *                                       defined
        cachedSubscriptionHasAllFetchPositions = false;
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17066 new consumer updatefetchpositions all in background thread (#16885).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
