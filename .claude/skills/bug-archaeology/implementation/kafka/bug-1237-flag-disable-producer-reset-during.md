**Commit**: 5eb4e116bc · **Ticket**: KAFKA-18067
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
JIRA: KAFKA-18067

Fix producer client double-closing issue in Kafka Streams. During StreamThread shutdown, TaskManager closes first, which closes the
producer client. Later, calling `unsubscribe` on the main consumer may
trigger the `onPartitionsLost` callback, attempting to reset
StreamsProducer when EOS is enabled. This causes an already closed
producer to be closed twice while the newly created producer is never
closed.

**Before/After**
```java
// Before
        streamsProducer.resetProducer(producer());

// After
    private boolean isClosed = false;

    /**
     * When {@link org.apache.kafka.streams.processor.internals.StreamThread} is shutting down,
     * subsequent calls to reInitializeProducer() will not recreate
     * the producer instance, avoiding resource leak.
     */
        if (!isClosed) {
            streamsProducer.resetProducer(producer());
        }
    // visible for test
    boolean isClosed() {
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18067: add a flag to disable producer reset during active task creator shutting down (#19269).

**Test**
Test modified in ActiveTaskCreatorTest.java covering the corrected behaviour.
