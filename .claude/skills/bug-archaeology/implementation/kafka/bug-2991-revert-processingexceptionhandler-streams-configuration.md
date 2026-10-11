**Commit**: fea3eeb7f7 · **Ticket**: KAFKA-16448
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
This reverts commit 3f70c46874e1dd9591443392f51ff1efc9fdc40e. Reviewer: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
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

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: revert "kafka-16448: add processingexceptionhandler in streams configuration (#16092)" (#16141).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
