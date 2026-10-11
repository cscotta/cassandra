**Commit**: 2d9994e0de · **Ticket**: KAFKA-16722
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
KIP-932 introduces share groups to go alongside consumer groups. Both kinds of group use server-side assignors but it is unlikely that a single assignor class would be suitable for both. As a result, the KIP introduces specific interfaces for consumer group and share group partition assignors. This PR introduces only the consumer group interface, `o.a.k.coordinator.group.assignor.ConsumerGroupPartitionAssignor`.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.assignor.PartitionAssignor
  val consumerGroupAssignors = getConfiguredInstances(GroupCoordinatorConfig.CONSUMER_GROUP_ASSIGNORS_CONFIG, classOf[PartitionAssignor])
import org.apache.kafka.coordinator.group.assignor.PartitionAssignor;
    public final List<PartitionAssignor> consumerGroupAssignors;
        List<PartitionAssignor> consumerGroupAssignors,
import org.apache.kafka.coordinator.group.assignor.PartitionAssignor;
        private L

// After
import org.apache.kafka.coordinator.group.assignor.ConsumerGroupPartitionAssignor
  val consumerGroupAssignors = getConfiguredInstances(GroupCoordinatorConfig.CONSUMER_GROUP_ASSIGNORS_CONFIG, classOf[ConsumerGroupPartitionAssignor])
import org.apache.kafka.coordinator.group.assignor.ConsumerGroupPartitionAssignor;
    public final List<ConsumerGroupPartitionAssignor> consumerGroupAssignors;
        List<ConsumerGroupPartitionAssignor> consumerGroupAssignors,
import org.apache.kafka.coordinator.group.assignor.ConsumerGroupPartitionAssignor;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16722: introduce consumergrouppartitionassignor interface (#15998).

**Test**
Test modified in GroupCoordinatorConfigTest.java covering the corrected behaviour.
