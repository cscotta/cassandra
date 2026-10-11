**Commit**: 1eb7644349 · **Ticket**: KAFKA-16845
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.server.QuorumTestHarness;
import kafka.utils.EmptyTestInfo;
import org.apache.kafka.clients.admin.AdminClientConfig;
import java.util.Collection;
import java.util.Properties;
import scala.collection.Seq;
import scala.jdk.javaapi.CollectionConverters;
 *
            experiment.setUp(new EmptyTestInfo());
    static class Experiment extends QuorumTestHarness {

// After
    testImplementation project(':test-common')
    <allow pkg="org.apache.kafka.metadata" />
    <allow pkg="org.apache.kafka.image" />
import kafka.server.BrokerServer;
import kafka.server.KafkaBroker;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.test.KafkaClusterTestKit;
import org.apache.kafka.common.test.TestKitNodes;
import org.apache.kafka.image.TopicImage;
import org.apache.kafka.metadata.PartitionRegistration;
 * <p>
    static class Experiment {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16845 migrate replicationquotastestrig to new test infra (#17089).

**Test**
Test modified in ReplicationQuotasTestRig.java covering the corrected behaviour.
