**Commit**: 6c3995b954 · **Ticket**: KAFKA-18569
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
ShareConsumers` may wait on an unneeded `FindCoordinator` during
`close()`(i.e after the acknowledgements are sent). https://github.com/apache/kafka/pull/18590 added the
`StopFindCoordinatorOnClose` event and was used by the regular
consumers. We are using the same event in `ShareConsumers` as well to
prevent sending this event when coordinator is no longer needed. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.consumer.internals.events.StopFindCoordinatorOnCloseEvent;
        swallow(log, Level.ERROR, "Failed to stop finding coordinator",
                this::stopFindCoordinatorOnClose, firstException);
    private void stopFindCoordinatorOnClose() {
        log.debug("Stop finding coordinator during consumer close");
        applicationEventHandler.add(new StopFindCoordinatorOnCloseEvent());
    }

import org.apache.kafka.clients.consumer.internals.events.StopFindCoordinatorOnCloseEvent;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
    @Test
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: port changes from kafka-18569 for shareconsumers (#19402).

**Test**
Test modified in ShareConsumerImplTest.java covering the corrected behaviour.
