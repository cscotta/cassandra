**Commit**: 51f2c7b2b6 · **Ticket**: KAFKA-14595
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
see

https://github.com/apache/kafka/blob/9570c67b8c4ed1a4c3888511adad58d9b3a8bc0f/core/src/main/scala/kafka/admin/ReassignPartitionsCommand.scala#L1208
During the rewrite for
[KAFKA-14595](https://github.com/apache/kafka/pull/13247), the relevant
condition was omitted. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            if (reassignment.addingReplicas().isEmpty() || !reassignment.removingReplicas().isEmpty())

    public void testCancellationWithAddingReplicaInIsr() throws Exception {

// After
            if (!reassignment.addingReplicas().isEmpty() || !reassignment.removingReplicas().isEmpty())

    public void testCancellationWithAddingAndRemovingReplicaInIsr() throws Exception {
    @ClusterTest
    public void testCancellationWithAddingReplicaInIsr() throws Exception {
        createTopics();
        TopicPartition foo0 = new TopicPartition("foo", 0);
        produceMessages(foo0.topic(), foo0.partition(), 200);

        // The reassignment will bring replicas 3 and 4 into the replica set.
        String assignment = "{\"version\":1,\"partitions\":" +
            "[{\"topic\":\"foo\",\"partition\":0,\"replicas\":[0,1,2,3,4],\"log_dirs\":[\"any\",\"any\",\"any\",\"any\",\"any\"]}" +
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: fix reassign command bug (#20003).

**Test**
Test modified in ReassignPartitionsCommandTest.java covering the corrected behaviour.
