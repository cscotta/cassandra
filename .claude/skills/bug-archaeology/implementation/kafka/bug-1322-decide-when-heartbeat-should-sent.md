**Commit**: 59e5890505 · **Ticket**: KAFKA-18736
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This commit adds the conditions to decide when a Streams group heartbeat
should be sent. A heartbeat should be sent when:
- the group coordinator is available
- the member is part of the Streams group or wants to join it
- the heartbeat interval expired or the member is leaving the group or
acknowledging the assginment

This commit does not implement:
- not sending fields that did not change
- handling errors

Reviewers: Zheguang Zhao <zheguang.zhao@alumni.brown.edu>, Lucas
Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before


// After
import org.apache.kafka.clients.consumer.internals.events.BackgroundEventHandler;
import org.apache.kafka.clients.consumer.internals.events.ErrorEvent;
import org.apache.kafka.common.utils.Timer;
/**
 * <p>Manages the request creation and response handling for the Streams group heartbeat. The class creates a
 * heartbeat request using the state stored in the membership manager. The requests can be retrieved
 * by calling {@link StreamsGroupHeartbeatRequestManager#poll(long)}. Once the response is received, it updates the
 * state in the membership manager and handles any errors.
 *
 * <p>The heartbeat manager generates heartbeat requests based on the member state. It's also responsible
 * for the timing of the heartbeat requests to ensure they are sent according to the heartbeat interval
 * (while the member state is stable) or on demand (while the member is acknowledging an assignment or
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18736: decide when a heartbeat should be sent (#19121).

**Test**
Test modified in StreamsGroupHeartbeatRequestManagerTest.java covering the corrected behaviour.
