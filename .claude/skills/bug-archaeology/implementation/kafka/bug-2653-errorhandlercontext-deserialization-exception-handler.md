**Commit**: faaef527d7 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This PR is part of KIP1033 which aims to bring a ProcessingExceptionHandler to Kafka Streams in order to deal with exceptions that occur during processing. This PR expose the new ErrorHandlerContext as a parameter to the Deserialization exception handlers and deprecate the previous handle signature. Co-authored-by: Dabz <d.gasparina@gmail.com>
Co-authored-by: loicgreffier <loic.greffier@michelin.com>

Reviewers: Bruno Cadonna <bruno@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before

    @SuppressWarnings("deprecation") // Old PAPI. Needs to be migrated.
    DeserializationHandlerResponse handle(final ProcessorContext context,
                                          final ConsumerRecord<byte[], byte[]> record,
                                          final Exception exception);

// After
import org.apache.kafka.streams.errors.internals.DefaultErrorHandlerContext;
     * @deprecated Since 3.9. Use Please {@link #handle(ErrorHandlerContext, ConsumerRecord, Exception)}
     */
    @Deprecated
    default DeserializationHandlerResponse handle(final ProcessorContext context,
                                                  final ConsumerRecord<byte[], byte[]> record,
                                                  final Exception exception) {
        throw new UnsupportedOperationException();
    }

    /**
     * Inspect a record and the exception received.
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: add errorhandlercontext in deserialization exception handler (#16432).

**Test**
Test modified in RecordDeserializerTest.java covering the corrected behaviour.
