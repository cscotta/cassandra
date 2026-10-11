**Commit**: c7c7bb72c6 · **Ticket**: KAFKA-20066
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
# Summary
This PR changes the structure of
`ConsumerGroupMember#assignedPartitions` from `Map<Uuid, Set<Integer>>`
to `Map<Uuid, Map<Integer, Integer>>`, to include per-partition epoch. This PR didn't change the fencing logic introduced in KIP-1251 yet. This
will be addressed in the next PR. Reviewers: Sean Quah <squah@confluent.io>, Lucas Brutschy
 <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import java.util.Set;
        Map<Uuid, Set<Integer>> assignment
                .setPartitions(new ArrayList<>(keyValue.getValue())))
        Map<Uuid, Set<Integer>> topicPartitions
    ) {
        List<ConsumerGroupCurrentMemberAssignmentValue.TopicPartitions> topics = new ArrayList<>(topicPartitions.size());
        topicPartitions.forEach((topicId, partitions) ->
                .setPartitions(new ArrayList<>(partitions)))
     * @param target                The partitions that the member should have.
        Map<Uuid, Set<Integer>> target
            Set<Integer> partitions = target.get(topicPartitions.topicId());

// After
        Map<Uuid, Map<Integer, Integer>> assignment
                .setPartitions(new ArrayList<>(keyValue.getValue().keySet())))
        Map<Uuid, Map<Integer, Integer>> assignment
    ) {
        List<ConsumerGroupCurrentMemberAssignmentValue.TopicPartitions> topics = new ArrayList<>(assignment.size());
        assignment.forEach((topicId, partitionEpochs) -> {
            List<Integer> partitionList = new ArrayList<>(partitionEpochs.keySet());
            List<Integer> epochList = partitionList.stream()
                .map(partitionEpochs::get)
                .toList();
                .setPartitions(partitionList)
                .setAssignmentEpochs(epochList));
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20066: implement kip-1251: assignment epochs for consumer groups [2/n] (#21558).

**Test**
Test modified in AssignmentTestUtil.java covering the corrected behaviour.
