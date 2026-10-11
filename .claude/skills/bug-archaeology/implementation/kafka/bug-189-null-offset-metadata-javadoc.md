**Commit**: 4eac6adf91 · **Ticket**: KAFKA-19284
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/admin/ListShareGroupOffsetsResult.java, ListStreamsGroupOffsetsResult.java
**Category**: Missing null / bounds check

**Root cause**: The `partitionsToOffsetAndMetadata()` methods in `ListShareGroupOffsetsResult` and `ListStreamsGroupOffsetsResult` could return a map containing `null` values for partitions where no committed offset existed, but this behavior was completely undocumented. Callers who assumed all map values were non-null would get an NPE when dereferencing the result. The analogous `ListConsumerGroupOffsetsResult` already documented this behavior; the two newer classes simply omitted the caveat.

**Before/After**:
```java
// BEFORE – no mention of null values
/**
 * Return a future which yields a map of topic partitions to offsets for the specified group.
 */
public KafkaFuture<Map<TopicPartition, OffsetAndMetadata>> partitionsToOffsetAndMetadata(...)

// AFTER – null semantics documented
/**
 * Return a future which yields a map of topic partitions to offsets for the specified group.
 * If the group doesn't have a committed offset for a specific partition, the corresponding
 * value in the returned map will be null.
 */
```

**How to spot**: When introducing a new result class that mirrors an existing one, diff the Javadoc of each method against its counterpart. Missing paragraphs (especially null-behavior caveats) are easy to overlook during API additions.

**Test**: No test added. Pure Javadoc clarification.
