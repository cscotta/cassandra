**Commit**: 981133d350 · **Ticket**: KAFKA-17486
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
This test has a tricky race condition. We want the restoration to go slow enough so that when a second Kafka Streams instance starts, the restoration of a given TopicPartition pauses due to task re-assignment. But after that point, we'd like the test to proceed faster to avoid any timeout assertions. To that end, here are the changes in this PR:

Increase the restore pause to 2 seconds; this should slow the restoration enough so that the process is still in progress once the second instance starts.

**Before/After**
```java
// Before
    private static final Duration RESTORATION_DELAY = Duration.ofMillis(500);
            mkEntry(StreamsConfig.CLIENT_ID_CONFIG, appId + "-ks2"),
            mkEntry(StreamsConfig.restoreConsumerPrefix(ConsumerConfig.MAX_POLL_RECORDS_CONFIG), 1)
        final List<KeyValue<Integer, Integer>> sampleData = IntStream.range(0, 500)
        final TestStateRestoreListener kafkaStreams2StateRestoreListener = new TestStateRestoreListener("ks2", RESTORATION_DELAY);
        private final Duration onBatchRestoredSleepDuration;

// After
    private static final Duration RESTORATION_DELAY = Duration.ofMillis(2000);
            mkEntry(StreamsConfig.CLIENT_ID_CONFIG, appId + "-ks2")
        final List<KeyValue<Integer, Integer>> sampleData = IntStream.range(0, 100)
        final TestStateRestoreListener kafkaStreams2StateRestoreListener = new TestStateRestoreListener("ks2", Duration.ZERO);
        private Duration onBatchRestoredSleepDuration;
            onBatchRestoredSleepDuration = Duration.ZERO;
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17486: flaky test restoreintegrationtest.shouldinvokeuserdefinedglobalstaterestorelistener (#17104).

**Test**
Test modified in RestoreIntegrationTest.java covering the corrected behaviour.
