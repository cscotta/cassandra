**Commit**: 1854d4b8a1 · **Ticket**: KAFKA-14572
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Migrate the EmbeddedKafkaCluster from the EmbeddedZookeeper to KRaft

Reviewers Bill Bejeck <bbejeck@apache.org>

**Before/After**
```java
// Before
    protected static final int TIMEOUT_MULTIPLIER = 15;
        cluster.deleteAllTopicsAndWait(120000);
        streams.start();
        streams.start();
        streams.start();
            cluster.deleteTopicAndWait(INTERMEDIATE_USER_TOPIC);
        // do not use list topics request, but read from the embedded cluster's zookeeper path directly to confirm
        properties  = mkObjectProperties(

// After
    testImplementation project(':metadata')
      <allow pkg="kafka.testkit"/>
      <allow pkg="org.apache.kafka.metadata"/>
    protected static final int TIMEOUT_MULTIPLIER = 30;
        cluster.deleteAllTopics();
        IntegrationTestUtils.startApplicationAndWaitUntilRunning(streams);
        IntegrationTestUtils.startApplicationAndWaitUntilRunning(streams);
        IntegrationTestUtils.startApplicationAndWaitUntilRunning(streams);
            cluster.deleteTopic(INTERMEDIATE_USER_TOPIC);
        properties = mkObjectProperties(
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14572: migrate embeddedkafkacluster used by streams integration tests from embeddedzookeeper to kraft (#17016).

**Test**
Test modified in AbstractResetIntegrationTest.java covering the corrected behaviour.
