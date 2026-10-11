**Commit**: 9dd73d43b0 · **Ticket**: KAFKA-18569
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>, Kirk True <ktrue@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (this.coordinator != null)
        COMMIT_ON_CLOSE, CREATE_FETCH_REQUESTS, LEAVE_GROUP_ON_CLOSE,

// After
import org.apache.kafka.clients.consumer.internals.events.StopFindCoordinatorOnCloseEvent;
        swallow(log, Level.ERROR, "Failed to stop finding coordinator",
            this::stopFindCoordinatorOnClose, firstException);
    private void stopFindCoordinatorOnClose() {
        if (groupMetadata.get().isEmpty())
            return;
        log.debug("Stop finding coordinator during consumer close");
        applicationEventHandler.add(new StopFindCoordinatorOnCloseEvent());
    }

    private boolean closing = false;
    @Override
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18569: new consumer close may wait on unneeded findcoordinator (#18590).

**Test**
Test modified in CoordinatorRequestManagerTest.java covering the corrected behaviour.
