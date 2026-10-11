**Commit**: cdc2d957ed · **Ticket**: KAFKA-16505

**Subsystem**: Kafka Streams / Dead Letter Queue (DLQ)

**Category**: Missing error handler / unimplemented code path

**Root cause**
Kafka Streams lacked a built-in dead-letter-queue mechanism for deserialization failures. When `DeserializationExceptionHandler` returned `FAIL`, the topology stopped; when it returned `CONTINUE`, the record was silently dropped. There was no way to route poisoned records to a separate topic. The fix introduces `DeserializationExceptionHandler.Response` / `Result` types with a `DEAD_LETTER_QUEUE` option and a `handleError()` method, along with DLQ topic configuration, so failed records are forwarded rather than silently discarded.

**Before/After**
```java
// Before: handler could only FAIL or CONTINUE
public enum DeserializationHandlerResponse { FAIL, CONTINUE }

// After: handler can route to DLQ
public enum Response { FAIL, CONTINUE, DEAD_LETTER_QUEUE }

DeserializationHandlerResponse handleError(...)
    // becomes:
Result handleError(ProcessorContext context, byte[] key, byte[] value, Exception e);
```

**How to spot**
If a Streams topology silently drops records that fail deserialization, inspect the `DeserializationExceptionHandler` implementation for `CONTINUE` returns with no side-effect; there is no guarantee the record is observable after that point without DLQ support.

**Test**
New `DeadLetterQueueTest` (or `DeserializationExceptionHandlerTest`) verifies that records triggering deserialization exceptions are forwarded to the configured DLQ topic with the original key, value, and exception metadata as headers.
