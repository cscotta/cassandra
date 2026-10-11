**Commit**: 61f61d6240 · **Ticket**: KAFKA-14569
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Omnia Ibrahim <o.g.h.ibrahim@gmail.com>, Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    testImplementation project(':group-coordinator')
import org.apache.kafka.clients.producer.RecordMetadata;
import java.util.concurrent.Future;
        // make sure the topic is auto-created in the other cluster
        waitForTopicCreated(primary, reverseTopic1);
        waitForTopicCreated(backup, backupTopic1);
        assertEquals(TopicConfig.CLEANUP_POLICY_COMPACT, getTopicConfig(backup.kafka(), backupTopic1, TopicConfig.CLEANUP_POLICY_CONFIG),
                "topic config was not synced");

// After
    testImplementation project(':server-common')
    testImplementation project(':server-common').sourceSets.test.output
    testImplementation project(':server-common').sourceSets.test.output
    testImplementation project(':server-common').sourceSets.test.output
        <allow pkg="kafka.testkit" />
      <allow pkg="org.apache.kafka.server.config" />
      <allow pkg="kafka.server"/>
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.test.TestCondition;
import java.util.stream.IntStream;

        TestCondition assertBackupTopicConfig = () -> {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14569: migrate connect's integration test embeddedkafkacluster from zk to kraft mode (#16599).

**Test**
Test modified in MirrorConnectorsIntegrationBaseTest.java covering the corrected behaviour.
