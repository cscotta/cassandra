**Commit**: f3a93551fa · **Ticket**: KAFKA-18034
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This reverts commit 70d6312a3a69496a949cc87055e9b5ea73f1e6c5. Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
            maybePropagateCoordinatorFatalErrorEvent();
    private void maybePropagateCoordinatorFatalErrorEvent() {
        coordinatorRequestManager.fatalError()
                .ifPresent(fatalError -> backgroundEventHandler.add(new ErrorEvent(fatalError)));
    }

        // poll when the coordinator node is known and fatal error is not present
        if (coordinatorRequestManager.coordinator().isEmpty()) {
            pendingRequests.maybeFailOnCoordinatorFatalError();
        }

        private void maybeFailOnCoordinatorFatalError() {

// After
        // poll only when the coordinator node is known.
        if (coordinatorRequestManager.coordinator().isEmpty())
import org.apache.kafka.clients.consumer.internals.events.BackgroundEventHandler;
import org.apache.kafka.clients.consumer.internals.events.ErrorEvent;
    private final BackgroundEventHandler backgroundEventHandler;
        final BackgroundEventHandler errorHandler,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: revert "kafka-18034: commitrequestmanager should fail pending requests on fatal coordinator errors (#18050)" (#18544).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
