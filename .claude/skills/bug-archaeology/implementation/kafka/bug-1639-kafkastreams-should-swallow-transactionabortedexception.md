**Commit**: 1672a4bc27 · **Ticket**: KAFKA-18476
**Subsystem**: streams/src
**Category**: Silent skip on non-match

**Root cause**
TransactionAbortedException is a follow up error to a previous error,
and such a previous error would already be handled when
`producer.abortTransaction()` is called. Thus, a
TransactionAbortedException can just be silently swallowed. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.errors.TransactionAbortedException;
        } else if (productionException instanceof TransactionAbortedException) {
            // swallow silently
            //
            // TransactionAbortedException is only thrown after `abortTransaction()` was called,
            // so it's only a followup error, and Kafka Streams is already handling the original error
import org.apache.kafka.common.errors.TransactionAbortedException;
    @Test
    public void shouldSwallowTransactionAbortedExceptionAndNotCallProductionExceptionHandler() {
        final MockProducer<byte[], byte[]> mockProducer = new MockProducer<>(
            cluster,
            false,
```

**How to spot**
When reviewing streams/src code, look for silent skip on non-match patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18476: kafkastreams should swallow transactionabortedexception (#18487).

**Test**
Test modified in RecordCollectorTest.java covering the corrected behaviour.
