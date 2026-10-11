**Commit**: 6610a4d46f · **Ticket**: KAFKA-17527
**Subsystem**: streams/src
**Category**: Missing null / bounds check

**Root cause**
Reviewers: Bruno Cadonna <bruno@confluent.io>

**Before/After**
```java
// Before
 * This interface allows user code to inspect the context of a record that has failed processing.
     * Return the current timestamp.
     * current rawRecord
     * {@link org.apache.kafka.clients.consumer.ConsumerRecord ConsumerRecord}
    private ProcessorContext processorContext;

// After
 * This interface allows user code to inspect the context of a record that has failed during processing.
 *
 * <p> {@code ErrorHandlerContext} instances are passed into {@link DeserializationExceptionHandler},
 * {@link ProcessingExceptionHandler}, or {@link ProductionExceptionHandler} dependent on what error occurred.
     * Additionally, when writing into a changelog topic, there is no associated input record,
     * and thus no topic name is available.
     * Additionally, when writing into a changelog topic, there is no associated input record,
     * and thus no partition is available.
     * Additionally, when writing into a changelog topic, there is no associated input record,
     * and thus no offset is available.
     * Additionally, when writing into a changelog topic, there is no associated input record,
     * and thus no headers are available.
```

**How to spot**
When reviewing streams/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17527: fix npe for null recordcontext (#17169).

**Test**
Test modified in RecordCollectorTest.java covering the corrected behaviour.
