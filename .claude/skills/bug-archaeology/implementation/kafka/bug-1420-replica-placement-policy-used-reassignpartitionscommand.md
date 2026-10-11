**Commit**: 1dcdbf78bb · **Ticket**: KAFKA-18798
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.admin.AdminUtils;
@SuppressWarnings("ClassDataAbstractionCoupling")
            Map<Integer, List<Integer>> assignedReplicas = AdminUtils.
                assignReplicasToBrokers(brokerMetadatas, assignment.size(), replicas.size());
            assignedReplicas.forEach((partition, replicas0) ->
                proposedAssignments.put(new TopicPartition(topic, partition), replicas0));
            assertStartsWith("Replication factor: 3 larger than available brokers: 2",

// After
import org.apache.kafka.common.DirectoryId;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.metadata.placement.ClusterDescriber;
import org.apache.kafka.metadata.placement.PlacementSpec;
import org.apache.kafka.metadata.placement.ReplicaPlacer;
import org.apache.kafka.metadata.placement.StripedReplicaPlacer;
import org.apache.kafka.metadata.placement.TopicAssignment;
import org.apache.kafka.metadata.placement.UsableBroker;
import java.util.Random;
@SuppressWarnings({"ClassDataAbstractionCoupling", "ClassFanOutComplexity"})
    private static final ReplicaPlacer REPLICA_PLACER = new StripedReplicaPlacer(new Random());

```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18798 the replica placement policy used by reassignpartitionscommand is not aligned with kraft controller (#18914).

**Test**
Test modified in ReassignPartitionsUnitTest.java covering the corrected behaviour.
