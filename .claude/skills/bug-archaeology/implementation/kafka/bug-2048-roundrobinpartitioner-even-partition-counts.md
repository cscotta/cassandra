**Commit**: 8563955e39 · **Ticket**: KAFKA-17632
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
RoundRobinPartitioner does not handle the fact that on new batch creation, the partition method is called twice. Reviewers: Viktor Somogyi-Vass <viktorsomogyi@gmail.com>, Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
    public void close() {}

// After
                // IMPORTANT NOTE: the following onNewBatch and partition calls should not interrupted to allow
                // the custom partitioner to correctly track its state
     * <p>
     * After onNewBatch, the {@link #partition(String, Object, byte[], Object, byte[], Cluster)} method is called again
     * which allows the implementation to "redirect" the message on new batch creation.
import org.apache.kafka.common.TopicPartition;
    private final ThreadLocal<TopicPartition> previousPartition = new ThreadLocal<>();
        TopicPartition prevPartition = previousPartition.get();
        if (prevPartition != null) {
            previousPartition.remove();
            if (topic.equals(prevPartition.topic())) {
                return prevPartition.partition();
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17632: fix roundrobinpartitioner for even partition counts (#17620).

**Test**
Test modified in RoundRobinPartitionerTest.java covering the corrected behaviour.
