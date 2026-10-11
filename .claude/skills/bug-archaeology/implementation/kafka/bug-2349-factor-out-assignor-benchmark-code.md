**Commit**: 9352faa8fc · **Ticket**: KAFKA-17495
**Subsystem**: jmh-benchmarks/src
**Category**: Logic error in condition

**Root cause**
ServerSideAssignorBenchmark and TargetAssignmentBuilderBenchmark have
the same topic and member subscription setup for the most part. Factor
out the commonality so that it's easier to share new setups between both
benchmarks. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.coordinator.group.api.assignor.GroupSpec;
import org.apache.kafka.coordinator.group.api.assignor.SubscriptionType;
import org.apache.kafka.coordinator.group.modern.Assignment;
import org.apache.kafka.coordinator.group.modern.GroupSpecImpl;
import org.apache.kafka.coordinator.group.modern.MemberSubscriptionAndAssignmentImpl;
import org.apache.kafka.coordinator.group.modern.TopicIds;
import org.apache.kafka.coordinator.group.modern.TopicMetadata;
import org.apache.kafka.coordinator.group.modern.consumer.ConsumerGroupMember;
import org.apache.kafka.image.MetadataImage;
import org.apache.kafka.image.MetadataProvenance;
import org.apache.kafka.image.TopicsImage;
import java.util.ArrayList;
```

**How to spot**
When reviewing jmh-benchmarks/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17495: factor out assignor benchmark code into utils class (#17133).

**Test**
No test added.
