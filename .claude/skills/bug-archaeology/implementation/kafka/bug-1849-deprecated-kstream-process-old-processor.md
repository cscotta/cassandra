**Commit**: ab2facca58 · **Ticket**: KAFKA-12829
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
    /**
     * Process all records in this stream, one record at a time, by applying a
     * {@link org.apache.kafka.streams.processor.Processor} (provided by the given
     * {@link org.apache.kafka.streams.processor.ProcessorSupplier}).
     * Attaching a state store makes this a stateful record-by-record operation (cf. {@link #foreach(ForeachAction)}).
     * If you choose not to attach one, this operation is similar to the stateless {@link #foreach(ForeachAction)}
     * but allows access to the {@code ProcessorContext} and record metadata.
     * This is essentially mixing the Processor API into the DSL, and provides all the functionality of the PAPI.
     * Furthermore, via {@link org.apache.kafka.streams.processor.Punctuator#punctuate(long)} the processing progress
     * can be observed and additional periodic actions can be performed.
     * Note that this is a terminal operation that returns void.
     * <p>

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12829: remove deprecated kstream.process() for old processor api (#18088).

**Test**
Test modified in KStreamImplTest.java covering the corrected behaviour.
