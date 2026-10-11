**Commit**: 4690527fab · **Ticket**: KAFKA-19362
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Finalise the share group SimpleAssignor for homogeneous subscriptions. The assignor code is much more accurate about the number of partitions
assigned to each member, and the number of members assigned for each
partition. It eliminates the idea of hash-based assignment because that
has been shown to the unhelpful. The revised code is very much more
effective at assigning evenly as the number of members grows and shrinks
over time.

**Before/After**
```java
// Before
import java.util.ListIterator;
 * based on the rules defined in KIP-932 to different members. It is not rack-aware.


// After
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
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19362: finalize homogeneous simple share assignor (#19977).

**Test**
Test modified in SimpleAssignorTest.java covering the corrected behaviour.
