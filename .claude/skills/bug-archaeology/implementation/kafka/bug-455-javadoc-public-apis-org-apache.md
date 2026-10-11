**Commit**: 70e7cddb9d · **Ticket**: KAFKA-20137
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
## Summary

This PR adds missing Javadocs to public APIs within the
`org.apache.kafka.clients.producer` package to improve documentation
coverage and  consistency with project standards. ## Changes

Added missing Javadocs to public classes, methods, and fields in 4 files
- BufferExhaustedException
- PreparedTxnState
- MockProducer
- ProducerConfig

## Test Plan
- No functional changes, documentation only
- Verified that Javadoc formatting follows existing Kafka conventions

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
     * @param injectTimeoutExceptionCounter use -1 for infinite

// After
    /**
     * Constructs a new BufferExhaustedException with the specified detail message.
     *
     * @param message The error message
     */
    /** Exception to throw when {@link #initTransactions(boolean)} is called */
    /** Exception to throw when {@link #beginTransaction()} is called */
    /** Exception to throw when {@link #sendOffsetsToTransaction(Map, ConsumerGroupMetadata)} is called */
    /** Exception to throw when {@link #commitTransaction()} is called */
    /** Exception to throw when {@link #abortTransaction()} is called */
    /** Exception to throw when {@link #send(ProducerRecord)} or {@link #send(ProducerRecord, Callback)} is called */
    /** Exception to throw when {@link #flush()} is called */
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20137: add javadoc to public apis in org.apache.kafka.clients.producer (#21445).

**Test**
No test added.
