**Commit**: 177b38ad66 · **Ticket**: KAFKA-16383
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
        backup.kafka().consume(NUM_PARTITIONS * NUM_RECORDS_PER_PARTITION, RECORD_TRANSFER_DURATION_MS, backupTopic);
            assertEquals(NUM_RECORDS_PER_PARTITION, replicatedCount);
        mm2Props.put("max.tasks", "10");

// After
        String sentinelTopic = "test-topic-sentinel";
        primary.kafka().createTopic(sentinelTopic);

        String backupSentinelTopic = remoteTopicName(sentinelTopic, PRIMARY_CLUSTER_ALIAS);
        waitForTopicCreated(backup, backupSentinelTopic);

        // wait for proof that the task has managed to poll its consumers at least once;
        // this should also mean that it knows the proper end offset of the other test topic,
        // and will consume exactly the expected number of records that we produce after
        // this assertion passes
        // NOTE: this assumes that there is a single MirrorSourceTask instance running;
        // if there are multiple tasks, the logic will need to be updated to ensure that each
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16383: ensure tasks have already polled their consumers before producing verified records in mirrorconnectorsinteg.

**Test**
Test modified in MirrorConnectorsIntegrationBaseTest.java covering the corrected behaviour.
