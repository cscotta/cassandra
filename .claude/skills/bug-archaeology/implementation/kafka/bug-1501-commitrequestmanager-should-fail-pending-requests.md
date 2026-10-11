**Commit**: 4b29fd6383 · **Ticket**: KAFKA-18034
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, Kirk True <ktrue@confluent.io>

**Before/After**
```java
// Before
        // poll only when the coordinator node is known.
        if (coordinatorRequestManager.coordinator().isEmpty())
import org.apache.kafka.clients.consumer.internals.events.BackgroundEventHandler;
import org.apache.kafka.clients.consumer.internals.events.ErrorEvent;
    private final BackgroundEventHandler backgroundEventHandler;
        final BackgroundEventHandler errorHandler,

// After
            maybePropagateCoordinatorFatalErrorEvent();
    private void maybePropagateCoordinatorFatalErrorEvent() {
        coordinatorRequestManager.getAndClearFatalError()
                .ifPresent(fatalError -> backgroundEventHandler.add(new ErrorEvent(fatalError)));
    }

        // poll when the coordinator node is known and fatal error is not present
        if (coordinatorRequestManager.coordinator().isEmpty()) {
            pendingRequests.maybeFailOnCoordinatorFatalError();
        }

        private void maybeFailOnCoordinatorFatalError() {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18034: commitrequestmanager should fail pending requests on fatal coordinator errors (#18548).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
