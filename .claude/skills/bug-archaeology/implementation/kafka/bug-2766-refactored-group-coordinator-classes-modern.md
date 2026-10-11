**Commit**: f2dbc55d24 · **Ticket**: KAFKA-17047
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Following the discussion and suggestion by @dajac, https://github.com/apache/kafka/pull/16054#discussion_r1613638293, the PR refactors the common classes to build TargetAssignment in `modern` package. `consumer` package has been moved inside `modern` package with classes exclusive to `consumer group`. This PR completes the refactoring and base to introduce `share` package inside `modern`. The subsequent PRs will define the implementation specific to Share Groups while re-using the common functionality from `modern` package classes.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.consumer.ConsumerGroup.ConsumerGroupState
import org.apache.kafka.coordinator.group.consumer.ConsumerGroup.ConsumerGroupState
import org.apache.kafka.coordinator.group.consumer.ConsumerGroupMember;
import org.apache.kafka.coordinator.group.consumer.TopicMetadata;
import org.apache.kafka.coordinator.group.consumer.Assignment;
import org.apache.kafka.coordinator.group.consumer.ConsumerGroup;
import org.apache.kafka.coordinator.group.consumer.ConsumerGroupMember;
import org.apache.kafka.coordinator.group.consumer.CurrentAssignmentBuilder;
import org.apache.kafka.coordinator.group.consumer.TargetAssignmentBuilder;
import org.apache.kafka.coordinator.group.consumer.TopicMetadata;

// After
import org.apache.kafka.coordinator.group.modern.consumer.ConsumerGroup.ConsumerGroupState
import org.apache.kafka.coordinator.group.modern.consumer.ConsumerGroup.ConsumerGroupState
import org.apache.kafka.coordinator.group.modern.TopicMetadata;
import org.apache.kafka.coordinator.group.modern.consumer.ConsumerGroupMember;
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17047: refactored group coordinator classes to modern package (kip-932) (#16474).

**Test**
Test modified in ConsumerProtocolMigrationTest.scala covering the corrected behaviour.
