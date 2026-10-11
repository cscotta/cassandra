**Commit**: f13a22af0b · **Ticket**: KAFKA-13722
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
We want to deprecate an remove the old ProcessorContext. Thus, we need
to refactor Kafka Streams runtime code, to not make calls into the old
ProcessorContext but only into new code path. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
                valueAndTimestamp == null ? context.timestamp() : valueAndTimestamp.timestamp()
        forward(new Record<>(key, value, timestamp(), headers()));
                processorContext.timestamp(),
                processorContext.headers()
            timestamp(),
            toInternal.hasTimestamp() ? toInternal.timestamp() : timestamp(),
            // is undefined is itself not well defined. Plus, I don't think we need to worry
            if (recordContext != null && (record.timestamp() != timestamp() || record.headers() != headers())) {

// After
                valueAndTimestamp == null ? context.recordContext().timestamp() : valueAndTimestamp.timestamp()
        forward(new Record<>(key, value, recordContext().timestamp(), headers()));
                processorContext.recordContext().timestamp(),
                processorContext.recordContext().headers()
            recordContext.timestamp(),
            toInternal.hasTimestamp() ? toInternal.timestamp() : recordContext.timestamp(),
            // is undefined is itself not well-defined. Plus, I don't think we need to worry
            if (recordContext != null && (record.timestamp() != recordContext.timestamp() || record.headers() != recordContext.headers())) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13722: remove usage of old processorcontext  (#18292).

**Test**
Test modified in AbstractProcessorContextTest.java covering the corrected behaviour.
