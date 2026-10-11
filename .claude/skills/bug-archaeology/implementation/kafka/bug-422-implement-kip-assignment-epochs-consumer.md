**Commit**: 9c2a0965b5 · **Ticket**: KAFKA-20066
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
# Summary
This PR moves `assignedPartitions` out of `ModernConsumerMember`
interface, add it as independent properties for `ShareGroupMember` and
`ConsumerGroupMember`. ## Reason for the change
In an upcoming PR, the structure of
`ConsumerGroupMember#assignedPartitions` and
`ConsumerGroupMember#partitionsPendingRevocation` will be changed to
include epoch information as
```
Map<Uuid, Map<Integer, Integer>>
```
This differs from the `ShareGroupMember#assignedPartitions` structure,
which remains `Map<Uuid, Set<Integer>>`. Therefore, it is no longer
appropriate to have this as a shared field in the base class. Reviewers: Sean Quah <squah@confluent.io>, Lucas Brutschy
 <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import static org.apache.kafka.coordinator.group.modern.consumer.ConsumerGroupMember.hasAssignedPartitionsChanged;
        if (memberEpoch == 0 || isFullRequest || hasAssignedPartitionsChanged(member, updatedMember)) {
        if (memberEpoch == 0 || isFullRequest || hasAssignedPartitionsChanged(member, updatedMember)) {
import org.apache.kafka.common.Uuid;

import java.util.Map;
    /**
     * The partitions assigned to this member.
     */
    protected Map<Uuid, Set<Integer>> assignedPartitions;

        MemberState state,

// After
        if (memberEpoch == 0 || isFullRequest || ConsumerGroupMember.hasAssignedPartitionsChanged(member, updatedMember)) {
        if (memberEpoch == 0 || isFullRequest || ShareGroupMember.hasAssignedPartitionsChanged(member, updatedMember)) {
        MemberState state
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20066: implement kip-1251: assignment epochs for consumer groups [1/n] (#21557).

**Test**
No test added.
