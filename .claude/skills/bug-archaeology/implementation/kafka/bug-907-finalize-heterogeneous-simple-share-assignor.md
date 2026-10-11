**Commit**: 860853dba2 · **Ticket**: KAFKA-19363
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Finalise the share group SimpleAssignor for heterogeneous subscriptions. The assignor code is much more accurate about the number of partitions
assigned to each member, and the number of members assigned for each
partition. It eliminates the idea of hash-based assignment because that
has been shown to the unhelpful. The revised code is very much more
effective at assigning evenly as the number of members grows and shrinks
over time.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.api.assignor.GroupSpec;
import org.apache.kafka.coordinator.group.api.assignor.PartitionAssignorException;
import org.apache.kafka.coordinator.group.api.assignor.SubscribedTopicDescriber;
import org.apache.kafka.server.common.TopicIdPartition;
import java.util.ArrayList;
import java.util.List;
    /**
     * Computes the list of target partitions which can be assigned to members. This list includes all partitions
     * for the subscribed topic IDs, with the additional check that they must be assignable.
     * @param groupSpec                 The assignment spec which includes member metadata.
     * @param subscribedTopicIds        The set of subscribed topic IDs.
     * @param subscribedTopicDescriber  The topic and partition metadata describer.

// After
        if (groupSpec.memberIds().isEmpty()) {
        }
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19363: finalize heterogeneous simple share assignor (#20074).

**Test**
Test modified in SimpleAssignorTest.java covering the corrected behaviour.
