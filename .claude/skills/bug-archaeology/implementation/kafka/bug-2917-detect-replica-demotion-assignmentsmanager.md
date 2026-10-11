**Commit**: 5594558047 · **Ticket**: KAFKA-16886
**Subsystem**: server/src
**Category**: Logic error in condition

**Root cause**
JBOD Brokers keep the Controller up to date with replica-to-directory
placement via AssignReplicasToDirsRequest. These requests are queued,
compacted and sent by AssignmentsManager. The Controller returns the error NOT_LEADER_OR_FOLLOWER when handling
a AssignReplicasToDirsRequest from a broker that is not a replica. A partition reassignment can take place, removing the Broker
as a replica before the AssignReplicasToDirsRequest successfully
reaches the Controller.

**Before/After**
```java
// Before
                        if (error != Errors.NONE) {
                    errors.computeIfAbsent(directory.id(), d -> new HashMap<>()).put(topicIdPartition, Errors.NONE);

// After
    public void onAssignment(TopicIdPartition topicPartition, Uuid dirId, String reason) {
        onAssignment(topicPartition, dirId, reason, null);
    }

                        if (error == Errors.NOT_LEADER_OR_FOLLOWER) {
                            log.info("Dropping late directory assignment for partition {} into directory {} because this broker is no longer a replica", partition, event.dirId);
                        } else if (error != Errors.NONE) {
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
        return buildResponse(request, topicIdPartition -> Errors.NONE);
    }
```

**How to spot**
When reviewing server/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16886: detect replica demotion in assignmentsmanager (#16232).

**Test**
Test modified in AssignmentsManagerTest.java covering the corrected behaviour.
