**Commit**: 5f8cf0e1f5 · **Ticket**: KAFKA-17421
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.test.api.Type;
import static org.junit.jupiter.api.Assertions.assertTrue;

    @ClusterTest(types = {Type.KRAFT}, brokers = 3)
    public void testLeaderEpoch(ClusterInstance clusterInstance) throws Exception {
        String topic = "test-topic";
        clusterInstance.createTopic(topic, 1, (short) 2);
        var msgNum = 10;
        sendMsg(clusterInstance, topic, msgNum);
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17421 add integration tests for consumerrecord#leaderepoch (#18254).

**Test**
Test modified in ConsumerIntegrationTest.java covering the corrected behaviour.
