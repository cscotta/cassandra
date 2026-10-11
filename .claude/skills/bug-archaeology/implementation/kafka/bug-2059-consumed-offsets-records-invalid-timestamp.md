**Commit**: 0bc91be145 · **Ticket**: KAFKA-17872
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
TimestampExtractor allows to drop records by returning a timestamp of -1. For this case, we still need to update consumed offsets to allows us to commit progress. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.serialization.Serdes;
    @SuppressWarnings("rawtypes")
    final InternalMockProcessorContext context = new InternalMockProcessorContext<>(
        new LogContext());
        new LogContext());
    @SuppressWarnings("unchecked")
        final byte[] key = Serdes.Long().serializer().serialize("foo", 1L);
        final byte[] value = Serdes.Long().serializer().serialize("foo", 1L);
        final byte[] key = Serdes.Long().serializer().serialize("foo", 1L);
        final byte[] value = Serdes.Long().serializer().serialize("foo", 1L);

// After
                lastCorruptedRecord = raw;
import org.apache.kafka.common.serialization.LongSerializer;
    final InternalMockProcessorContext<Integer, Integer> context = new InternalMockProcessorContext<>(
        new LogContext()
    );
        new LogContext()
    );
    private final RecordQueue queueThatSkipsInvalidTimestamps = new RecordQueue(
        new TopicPartition("topic", 1),
        mockSourceNodeWithMetrics,
        new LogAndSkipOnInvalidTimestamp(),
        new LogAndFailExceptionHandler(),
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17872: update consumed offsets on records with invalid timestamp (#17710).

**Test**
Test modified in RecordQueueTest.java covering the corrected behaviour.
