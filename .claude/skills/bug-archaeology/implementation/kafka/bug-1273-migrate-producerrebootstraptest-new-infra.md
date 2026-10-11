**Commit**: fef9aebb19 · **Ticket**: KAFKA-18276
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
The PR changed three things. * Migrated `ProducerRebootstrapTest` to new test infra and removed the
old Scala test. * Updated the original test case to cover rebootstrap scenarios. * Integrated `ProducerRebootstrapTest` into `ClientRebootstrapTest` in
the `client-integration-tests` module.

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.TopicConfig;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.assertEquals;

    @ClusterTest(
        brokers = REPLICAS,
        types = {Type.KRAFT},
        serverProperties = {
            @ClusterConfigProperty(key = TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG, value = "true"),
            @ClusterConfigProperty(key = GroupCoordinatorConfig.OFFSETS_TOPIC_REPLICATION_FACTOR_CONFIG, value = "2")
        }
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18276 migrate producerrebootstraptest to new test infra (#19046).

**Test**
Test modified in ClientRebootstrapTest.java covering the corrected behaviour.
