**Commit**: 6c4e777079 · **Ticket**: KAFKA-16954
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This PR fixes consumer close to avoid updating the subscription state object in the app thread. Now the close simply triggers an UnsubscribeEvent that is handled in the background to trigger callbacks, clear assignment, and send leave heartbeat. Note that after triggering the event, the unsubscribe will continuously process background events until the event completes, to ensure that it allows for callbacks to run in the app thread. The logic around what happens if the unsubscribe fails remain unchanged: close will log, keep the first exception and carry on.

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.internals.events.LeaveOnCloseEvent;
import org.apache.kafka.common.utils.Utils;
import java.util.TreeSet;
        releaseAssignmentAndLeaveGroup(closeTimer, firstException);
        closeTimer.update();
     * 2. revoke all partitions
     * 3. if partition revocation completes successfully, send leave group
    void releaseAssignmentAndLeaveGroup(final Timer timer, final AtomicReference<Throwable> firstException) {
        completeQuietly(() -> maybeRevokePartitions(),
            "Failed to execute callback to release assignment", firstException);
        completeQuietly(() -> applicationEventHandler.addAndGet(new LeaveOnCloseEvent(calculateDeadlineMs(timer))),
            "Failed to send leave group heartbeat with a timeout(ms)=" + timer.timeoutMs(), firstException);

// After
        swallow(log, Level.ERROR, "Failed to release assignment before closing consumer",
            () -> releaseAssignmentAndLeaveGroup(closeTimer), firstException);
     * 2. release assignment. This is done via a background unsubscribe event that will
     * trigger the callbacks, clear the assignment on the subscription state and send the leave group request to the broker
    private void releaseAssignmentAndLeaveGroup(final Timer timer) {

        log.info("Releasing assignment and leaving group before closing consumer");
        UnsubscribeEvent unsubscribeEvent = new UnsubscribeEvent(calculateDeadlineMs(timer));
        applicationEventHandler.add(unsubscribeEvent);
        try {
            processBackgroundEvents(unsubscribeEvent.future(), timer);
            log.info("Completed releasing assignment and sending leave group to close consumer");
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16954: fix consumer close to release assignment in background (#16343).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
