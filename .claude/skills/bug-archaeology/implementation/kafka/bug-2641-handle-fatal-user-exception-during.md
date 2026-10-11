**Commit**: 0dc9b9e4ee · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR is part of KIP-1033 which aims to bring a ProcessingExceptionHandler to Kafka Streams in order to deal with exceptions that occur during processing. This PR catch the exceptions thrown while handling a processing exception

Co-authored-by: Dabz <d.gasparina@gmail.com>
Co-authored-by: loicgreffier <loic.greffier@michelin.com>

Reviewers: Bruno Cadonna <bruno@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            final ProcessingExceptionHandler.ProcessingHandlerResponse response = processingExceptionHandler
                .handle(errorHandlerContext, record, e);

// After
            final ProcessingExceptionHandler.ProcessingHandlerResponse response;
            try {
                response = processingExceptionHandler.handle(errorHandlerContext, record, e);
            } catch (final Exception fatalUserException) {
                throw new FailedProcessingException(fatalUserException);
            }
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertFalse;
    @Test
    public void shouldStopOnFailedProcessorWhenProcessingExceptionOccursInFailProcessingExceptionHandler() {
        final KeyValue<String, String> event = new KeyValue<>("ID123-1", "ID123-A1");
        final KeyValue<String, String> eventError = new KeyValue<>("ID123-2-ERR", "ID123-A2");
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: handle fatal user exception during processing error (#16675).

**Test**
Test modified in ProcessingExceptionHandlerIntegrationTest.java covering the corrected behaviour.
