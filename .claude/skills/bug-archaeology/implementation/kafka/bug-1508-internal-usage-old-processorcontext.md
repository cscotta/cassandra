**Commit**: 1123a76110 · **Ticket**: KAFKA-13722
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
     * You can retrieve all generated internal topic names via {@link Topology#describe()}.
    private InternalProcessorContext internalProcessorContext;
        internalProcessorContext = asInternalProcessorContext((org.apache.kafka.streams.processor.ProcessorContext) context);
        internalProcessorContext = asInternalProcessorContext((org.apache.kafka.streams.processor.ProcessorContext) context());
    @SuppressWarnings("unchecked")
@SuppressWarnings("deprecation")
 * A functional interface used as an argument to {@link ProcessorContext#schedule(Duration, PunctuationType, Punctuator)}.
     * <p> If this method accesses {@link ProcessorContext} or
     * <p> Furthermore, for any record that is sent downs

// After
     * <p>You can retrieve all generated internal topic names via {@link Topology#describe()}.
    private InternalProcessorContext<K1, VOut> internalProcessorContext;
        internalProcessorContext = asInternalProcessorContext(context);
@SuppressWarnings("deprecation") // Not deprecating the old context, since it is used by Transformers. See KAFKA-10603.
/*
 * When we deprecate `ProcessorContext` can also deprecate `To` class,
 * as it is only used in the `ProcessorContext#forward` method.
 */
 * A functional interface used as an argument to
 * {@link org.apache.kafka.streams.processor.api.ProcessorContext#schedule(Duration, PunctuationType, Punctuator)}.
     * <p> If this method accesses {@link org.apache.kafka.streams.processor.api.ProcessorContext} or
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13722: remove internal usage of old processorcontext (#18698).

**Test**
Test modified in KTableImplTest.java covering the corrected behaviour.
