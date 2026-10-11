**Commit**: 4de83d38c9 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR is part of KAFKA-16448 (KIP-1033) which aims to bring a ProcessingExceptionHandler to Kafka Streams in order to deal with exceptions that occur during processing. This PR actually catches processing exceptions. Authors:
@Dabz
@sebastienviale
@loicgreffier

Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.errors.ProcessingExceptionHandler;
import static org.apache.kafka.streams.StreamsConfig.PROCESSING_EXCEPTION_HANDLER_CLASS_CONFIG;
    public final Supplier<ProcessingExceptionHandler> processingExceptionHandlerSupplier;
        this.processingExceptionHandlerSupplier = () -> globalAppConfigs.getConfiguredInstance(PROCESSING_EXCEPTION_HANDLER_CLASS_CONFIG, ProcessingExceptionHandler.class);
            processingExceptionHandlerSupplier.get(),
        public final ProcessingExceptionHandler processingExceptionHandler;
                           final ProcessingExceptionHandler processingExceptionHandler,
            this.processingExceptionHandler = processingExceptionHandler;
/**
 * Default implementation of {@link ErrorHandlerContext} that provides access to the metadata of the record that caused the error.
 */
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: catch and handle processing exceptions (#16093).

**Test**
Test modified in ProcessingExceptionHandlerIntegrationTest.java covering the corrected behaviour.
