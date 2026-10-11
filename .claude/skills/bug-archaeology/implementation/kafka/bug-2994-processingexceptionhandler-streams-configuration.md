**Commit**: 3f70c46874 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
This PR is part of KAFKA-16448 which aims to bring a ProcessingExceptionHandler to Kafka Streams in order to deal with exceptions that occur during processing. This PR brings ProcessingExceptionHandler in Streams configuration. Co-authored-by: Dabz <d.gasparina@gmail.com>
Co-authored-by: sebastienviale <sebastien.viale@michelin.com>

Reviewer: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.errors.LogAndFailProcessingExceptionHandler;
import org.apache.kafka.streams.errors.ProcessingExceptionHandler;
    /** {@code processing.exception.handler} */
    @SuppressWarnings("WeakerAccess")
    public static final String PROCESSING_EXCEPTION_HANDLER_CLASS_CONFIG = "processing.exception.handler";
    public static final String PROCESSING_EXCEPTION_HANDLER_CLASS_DOC = "Exception handling class that implements the <code>org.apache.kafka.streams.errors.ProcessingExceptionHandler</code> interface.";

            .define(PROCESSING_EXCEPTION_HANDLER_CLASS_CONFIG,
                    Type.CLASS,
                    LogAndFailProcessingExceptionHandler.class.getName(),
                    Importance.MEDIUM,
                    PROCESSING_EXCEPTION_HANDLER_CLASS_DOC)
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: add processingexceptionhandler in streams configuration (#16092).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
