**Commit**: 661825ad28 · **Ticket**: KAFKA-16993
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Increased the number of records while decreasing the restore batch size to ensure the restoration does not complete before the second Kafka Streams instance starts up. Reviewers: Matthias J. Sax <mjsax@apache.org>

**Before/After**
```java
// Before
            mkEntry(StreamsConfig.restoreConsumerPrefix(ConsumerConfig.MAX_POLL_RECORDS_CONFIG), 5)
            mkEntry(StreamsConfig.restoreConsumerPrefix(ConsumerConfig.MAX_POLL_RECORDS_CONFIG), 5)
        final List<KeyValue<Integer, Integer>> sampleData = IntStream.range(0, 100)
                             IntegrationTestUtils.DEFAULT_TIMEOUT,

// After
            mkEntry(StreamsConfig.restoreConsumerPrefix(ConsumerConfig.MAX_POLL_RECORDS_CONFIG), 1)
            mkEntry(StreamsConfig.restoreConsumerPrefix(ConsumerConfig.MAX_POLL_RECORDS_CONFIG), 1)
        final List<KeyValue<Integer, Integer>> sampleData = IntStream.range(0, 500)
                             90_000,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16993: flaky test shouldinvokeuserdefinedglobalstaterestorelistener (#16970).

**Test**
Test modified in RestoreIntegrationTest.java covering the corrected behaviour.
