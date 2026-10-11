**Commit**: 8bdafb49ee · **Ticket**: KAFKA-7699
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR implements Processor API interfaces for using Anchored
punctuation. As described in

https://cwiki.apache.org/confluence/display/KAFKA/KIP-1146%3A+Anchored+punctuation,
anchored punctuations allows the users of the PAPI to set both a start
time and an interval for when the punctuations should be triggered. The
changes have been based on all the places where

`org.apache.kafka.streams.processor.internals.ProcessorContext.schedule()`
is used. The changes consist of introducing new interfaces as the punctuation
time logic in the
`org.apache.kafka.streams.processor.internals.PunctuationSchedule`
already supports calculating a new punctuation time based on a start
time and an interval.

**Before/After**
```java
// Before
// (no removals)

// After
import java.time.Instant;
    /**
     * Schedule a periodic operation for processors. A processor may call this method during a
     * {@link org.apache.kafka.streams.kstream.KTable#transformValues(ValueTransformerWithKeySupplier, String...)}'s
     * {@link org.apache.kafka.streams.kstream.ValueTransformerWithKey#init(ProcessorContext) initialization} or
     * {@link org.apache.kafka.streams.kstream.ValueTransformerWithKey#transform(Object, Object) processing} to
     * schedule a periodic callback &mdash; called a punctuation &mdash; to {@link Punctuator#punctuate(long)}.
     * The type parameter controls what notion of time is used for punctuation:
     * <ul>
     *   <li>{@link PunctuationType#STREAM_TIME} &mdash; uses "stream time", which is advanced by the processing of messages
     *   in accordance with the timestamp as extracted by the {@link TimestampExtractor} in use.
     *   The first punctuation will be triggered by the first record that is processed.
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-7699: anchored punctuation (#19937).

**Test**
Test modified in AbstractProcessorContextTest.java covering the corrected behaviour.
