**Commit**: b74b182841 · **Ticket**: KAFKA-16786
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Remove usage of the partition.assignment.strategy config in the new consumer. This config is deprecated with the new consumer protocol, so the AsyncKafkaConsumer should not use or validate the property. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
    private final List<ConsumerPartitionAssignor> assignors;
            this.assignors = ConsumerPartitionAssignor.getAssignorInstances(
                    config.getList(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG),
                    config.originals(Collections.singletonMap(ConsumerConfig.CLIENT_ID_CONFIG, clientId))
            );
                       List<ConsumerPartitionAssignor> assignors,
        this.assignors = assignors;
                       ConsumerMetadata metadata,
                       List<ConsumerPartitionAssignor> assignors) {
        this.assignors = assignors;
    private void throwIfNoAssignorsConfigured() {

// After
                       ConsumerMetadata metadata) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16786: remove old assignment strategy usage in new consumer (#16214).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
