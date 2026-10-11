**Commit**: b4f47aeff5 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Part of KIP-1033. Co-authored-by: Dabz <d.gasparina@gmail.com>
Co-authored-by: loicgreffier <loic.greffier@michelin.com>

Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
                                      final TaskId taskId) {
                internalProcessorContext.taskId());

// After
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;


    /**
     * Return the current timestamp.
     *
     * <p> If it is triggered while processing a record streamed from the source processor,
     * timestamp is defined as the timestamp of the current input record; the timestamp is extracted from
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: add timestamp to error handler context (#17054).

**Test**
Test modified in ProcessingExceptionHandlerIntegrationTest.java covering the corrected behaviour.
