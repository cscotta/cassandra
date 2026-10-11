**Commit**: e4c0034679 · **Ticket**: KAFKA-18019
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
This patch contains changes to the handling of the INVALID_PRODUCER_ID_MAPPING error. Quoted from KIP-890
Since we bump epoch on abort, we no longer need to call InitProducerId to fence requests. InitProducerId will only be called when the producer starts up to fence a previous instance. With this change, some other calls to InitProducerId were inspected including the call after receiving an InvalidPidMappingException.

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.InvalidPidMappingException;
        // If the error is an INVALID_PRODUCER_ID_MAPPING error, the server will not accept an EndTxnRequest, so skip
        // directly to InitProducerId. Otherwise, we must first abort the transaction, because the producer will be
        // fenced if we directly call InitProducerId.
        if (!(lastError instanceof InvalidPidMappingException)) {
            EndTxnRequest.Builder builder = new EndTxnRequest.Builder(
                    new EndTxnRequestData()
                            .setTransactionalId(transactionalId)
                            .setProducerId(producerIdAndEpoch.producerId)
                            .setProducerEpoch(producerIdAndEpoch.epoch)
                            .setCommitted(transactionResult.id),
                    isTransactionV2Enabled

// After
        EndTxnRequest.Builder builder = new EndTxnRequest.Builder(
            new EndTxnRequestData()
                .setTransactionalId(transactionalId)
                .setProducerId(producerIdAndEpoch.producerId)
                .setProducerEpoch(producerIdAndEpoch.epoch)
                .setCommitted(transactionResult.id),
            isTransactionV2Enabled
        );
        EndTxnHandler handler = new EndTxnHandler(builder);
        enqueueRequest(handler);

        // If an epoch bump is required for recovery, initialize the transaction after completing the EndTxn request.
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18019: make invalid_producer_id_mapping a fatal error (#17822).

**Test**
Test modified in TransactionManagerTest.java covering the corrected behaviour.
