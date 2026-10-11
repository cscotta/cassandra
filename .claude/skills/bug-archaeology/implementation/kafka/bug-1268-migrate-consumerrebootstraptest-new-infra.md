**Commit**: 1547204baa · **Ticket**: KAFKA-18914
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Migrate ConsumerRebootstrapTest to the new test infra and remove the old
Scala test. The PR changed three things. * Migrated `ConsumerRebootstrapTest` to new test infra and removed the
old Scala test. * Updated the original test case to cover rebootstrap scenarios.

**Before/After**
```java
// Before
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) REPLICAS)));
        admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) REPLICAS)));
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) REPLICAS)));
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) REPLICAS)));

// After
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.GroupProtocol;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.test.TestUtils;
    private static final int PARTITIONS = 1;
            admin.createTopics(List.of(new NewTopic(TOPIC, PARTITIONS, (short) REPLICAS)));
        admin.createTopics(List.of(new NewTopic(TOPIC, PARTITIONS, (short) REPLICAS)));
            admin.createTopics(List.of(new NewTopic(TOPIC, PARTITIONS, (short) REPLICAS)));
            admin.createTopics(List.of(new NewTopic(TOPIC, PARTITIONS, (short) REPLICAS)));

    public void consumerRebootstrap(ClusterInstance clusterInstance, GroupProtocol groupProtocol) throws InterruptedException, ExecutionException {
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18914 migrate consumerrebootstraptest to use new test infra (#19154).

**Test**
Test modified in ClientRebootstrapTest.java covering the corrected behaviour.
