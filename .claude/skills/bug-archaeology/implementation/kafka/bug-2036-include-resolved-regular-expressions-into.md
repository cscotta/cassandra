**Commit**: a802865aad · **Ticket**: KAFKA-17593
**Subsystem**: group-coordinator/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
This patch does a few things:
* Refactors the `TargetAssignmentBuilder` to use inheritance to differentiate Consumer and Share groups. * Introduces `UnionSet` to lazily aggregate the subscriptions for a given member. * Wires the resolved regular expressions in the `GroupMetadataManager`. At the moment, they are only used when the target assignment is computed.

**Before/After**
```java
// Before
            TargetAssignmentBuilder<ConsumerGroupMember> assignmentResultBuilder =
                new TargetAssignmentBuilder<ConsumerGroupMember>(group.groupId(), groupEpoch, consumerGroupAssignors.get(preferredServerAssignor))
            TargetAssignmentBuilder<ShareGroupMember> assignmentResultBuilder =
                new TargetAssignmentBuilder<ShareGroupMember>(group.groupId(), groupEpoch, shareGroupAssignor)
                    .withTargetAssignmentRecordBuilder(GroupCoordinatorRecordHelpers::newShareGroupTargetAssignmentRecord)
                    .withTargetAssignmentEpochRecordBuilder(GroupCoordinatorRecordHelpers::newShareGroupTargetAssignmentEpochRecord)
public class TargetAssignmentBuilder<T extends ModernGroupMember> {

// After
            TargetAssignmentBuilder.ConsumerTargetAssignmentBuilder assignmentResultBuilder =
                new TargetAssignmentBuilder.ConsumerTargetAssignmentBuilder(group.groupId(), groupEpoch, consumerGroupAssignors.get(preferredServerAssignor))
                    .withResolvedRegularExpressions(group.resolvedRegularExpressions())
            TargetAssignmentBuilder.ShareTargetAssignmentBuilder assignmentResultBuilder =
                new TargetAssignmentBuilder.ShareTargetAssignmentBuilder(group.groupId(), groupEpoch, shareGroupAssignor)
import org.apache.kafka.coordinator.group.modern.consumer.ConsumerGroupMember;
import org.apache.kafka.coordinator.group.modern.consumer.ResolvedRegularExpression;
import org.apache.kafka.coordinator.group.modern.share.ShareGroupMember;
public abstract class TargetAssignmentBuilder<T extends ModernGroupMember, U extends TargetAssignmentBuilder<T, U>> {
    public static class ConsumerTargetAssignmentBuilder extends TargetAssignmentBuilder<ConsumerGroupMember, ConsumerTargetAssignmentBuilder> {

        /**
```

**How to spot**
When reviewing group-coordinator/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [5/n] include resolved regular expressions into target assignment computation (#17750).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
