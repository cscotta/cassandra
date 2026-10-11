**Commit**: 38e3359446 · **Ticket**: KAFKA-19511

**Subsystem**: Kafka Streams / Integration Tests

**Category**: Flaky test

**Root cause**
`HandlingSourceTopicDeletionIntegrationTest` was flaky because it included a new-protocol test variant that introduced timing-sensitive behavior, and its `KafkaStreams` instances were not closed in a try-with-resources block, leaving streams applications running across test boundaries and interfering with subsequent test iterations. Removing the new-protocol variant and wrapping `KafkaStreams` in try-with-resources ensures deterministic cleanup regardless of test outcome.

**Before/After**
```java
// Before: KafkaStreams not guaranteed to close on failure
KafkaStreams streams = new KafkaStreams(topology, props);
streams.start();
// ... assertions ...
streams.close();

// After: guaranteed close via try-with-resources
try (KafkaStreams streams = new KafkaStreams(topology, props)) {
    streams.start();
    // ... assertions ...
}
```

**How to spot**
Integration tests that create `KafkaStreams` instances without try-with-resources or explicit `@AfterEach` cleanup are prone to leaving threads and consumer group state behind, causing non-deterministic failures in subsequent tests.

**Test**
`HandlingSourceTopicDeletionIntegrationTest` passes reliably after removing the flaky new-protocol variant and adopting try-with-resources for all `KafkaStreams` instances.
