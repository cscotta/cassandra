**Commit**: 615c8c0e11 · **Ticket**: KAFKA-17850
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Following the KIP-1033 a FailedProcessingException is passed to the Streams-specific uncaught exception handler. The goal of the PR is to unwrap a FailedProcessingException into a StreamsException when an exception occurs during the flushing or closing of a store

Reviewer: Bruno Cadonna <cadonna@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.errors.LogAndContinueProcessingExceptionHandler;
import org.apache.kafka.streams.errors.LogAndFailProcessingExceptionHandler;
    @Test
    public void shouldFailWhenProcessingExceptionOccursFromFlushingCacheIfExceptionHandlerReturnsFail() {
        final List<KeyValue<String, String>> events = Arrays.asList(
            new KeyValue<>("ID123-1", "ID123-A1"),
            new KeyValue<>("ID123-1", "ID123-A2"),
            new KeyValue<>("ID123-1", "ID123-A3"),
            new KeyValue<>("ID123-1", "ID123-A4")
        );

        final List<KeyValueTimestamp<String, String>> expectedProcessedRecords = Arrays.asList(
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17850: fix leaking internal exception in state manager (#17711).

**Test**
Test modified in ProcessingExceptionHandlerIntegrationTest.java covering the corrected behaviour.
