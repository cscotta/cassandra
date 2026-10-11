**Commit**: b1ea280ab1 · **Ticket**: KAFKA-19250
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
## Problem
When an `txnProducer.abortTransaction()`operation encounters a
`TRANSACTION_ABORTABLE` error, it currently tries to transition to
`ABORTABLE_ERROR` state. This can create an infinite retry loop since:
1. The abort operation fails with `TRANSACTION_ABORTABLE`
2. We transition to `ABORTABLE_ERROR` state
3.

**Before/After**
```java
// Before


// After
            boolean isAbort = !builder.data.committed();
            } else if (isAbort && error.exception() instanceof TransactionAbortableException) {
                // When aborting a transaction, we must convert TRANSACTION_ABORTABLE errors to KafkaException
                // because if an abort operation itself encounters an abortable error, retrying the abort would create a cycle.
                // Instead, we treat this as fatal error at the application layer to ensure the transaction can be cleanly terminated.
                fatalError(new KafkaException("Failed to abort transaction", error.exception()));

    @Test
    public void testAbortableErrorIsConvertedToFatalErrorDuringAbort() throws Exception {

        // Initialize and begin transaction
        TransactionManager transactionManager = new TransactionManager(logContext, "testAbortableErrorIsConvertedToFatalErrorDuringAbort", 60000, 100, apiVersions, false);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19250 : txnproducer.aborttransaction() api should not return abortable exception (#19656).

**Test**
Test modified in SenderTest.java covering the corrected behaviour.
