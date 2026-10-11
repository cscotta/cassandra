**Commit**: 44481caa6b · **Ticket**: KAFKA-19775
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/StreamTask.java
**Category**: Missing null / bounds check

**Root cause**
`StreamTask.findOffsetAndMetadata()` looked up the current partition in `nextOffsetsAndMetadataToBeConsumed` and threw an `IllegalStateException` (treated as fatal) if the key was absent. Before the caching change, positions were always available because `mainConsumer.position()` was called synchronously. After the caching change, the map is populated only when records have actually been consumed; topics with no messages yet (e.g., a newly matched regex topic) never appear in the map, causing a false fatal error.

**Before/After**
```java
// BEFORE – absence treated as fatal bug
private OffsetAndMetadata findOffsetAndMetadata(TopicPartition partition) {
    if (nextOffsetsAndMetadataToBeConsumed.containsKey(partition)) {
        ...
    } else {
        throw new IllegalStateException("Stream task " + id +
            " does not know the partition: " + partition);
    }
}

// AFTER – absence treated as "nothing to commit yet"
private Optional<OffsetAndMetadata> findOffsetAndMetadata(TopicPartition partition) {
    OffsetAndMetadata offsetAndMetadata = nextOffsetsAndMetadataToBeConsumed.get(partition);
    if (offsetAndMetadata == null) {
        return Optional.empty();  // nothing consumed yet
    }
    ...
}
```

**How to spot**
Whenever a cache is introduced to replace a synchronous lookup, every call site that previously asserted the result was always present must be re-evaluated. If the new code path can miss the cache (e.g., for partitions with no data), an unconditional presence check will become a spurious fatal error.

**Test**
`RegexSourceIntegrationTest.shouldNotCrashIfPatternMatchesTopicHasNoData`: creates two topics, subscribes via regex, produces only to one topic, and verifies the application does not crash when the second topic has no data at commit time.
