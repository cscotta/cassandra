**Commit**: 949617b0b2 · **Ticket**: KAFKA-17747
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
* Add `RackAwareAssignor`. It uses `racksForPartition` to check the rack
id of a partition and assign it to a member which has the same rack id. * Add `ConsumerIntegrationTest#testRackAwareAssignment` to check
`racksForPartition` works correctly. Reviewers: David Jacot <djacot@confluent.io>

---------

Signed-off-by: PoAn Yang <payang@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
    implementation project(':group-coordinator:group-coordinator-api')
    <allow pkg="org.apache.kafka.clients.consumer" exact-match="true"/>
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
    @ClusterTest(
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17747: [7/n] add consumer group integration test for rack aware assignment (#19856).

**Test**
Test modified in ConsumerIntegrationTest.java covering the corrected behaviour.
