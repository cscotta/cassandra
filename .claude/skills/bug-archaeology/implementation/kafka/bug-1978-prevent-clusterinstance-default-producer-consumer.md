**Commit**: 5ad532f4ad · **Ticket**: KAFKA-18075
**Subsystem**: test-common/test-common-api
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        return new KafkaProducer<>(Map.of());
        return new KafkaConsumer<>(Map.of());

// After
        return producer(Map.of());
        return consumer(Map.of());
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.coordinator.group.GroupCoordinatorConfig;

    @ClusterTest(types = {Type.CO_KRAFT, Type.KRAFT}, serverProperties = {
        @ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_PARTITIONS_CONFIG, value = "1"),
        @ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG, value = "1")
    })
    public void testCreateDefaultProducerAndConsumer(ClusterInstance cluster) throws InterruptedException {
        String topic = "topic";
        Bytes key = Bytes.wrap("key".getBytes());
```

**How to spot**
When reviewing test-common/test-common-api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18075 prevent clusterinstance default producer and consumer initialization with empty configs (#17926).

**Test**
Test modified in ClusterInstance.java covering the corrected behaviour.
