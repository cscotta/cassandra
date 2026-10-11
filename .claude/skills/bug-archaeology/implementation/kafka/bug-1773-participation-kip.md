**Commit**: 3ef10a3f61 · **Ticket**: KAFKA-15370
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This is just a mechanical change to make prepareTransitionTo method use named parameters instead of positional parameters. Reviewers: Justine Olshan <jolshan@confluent.io>, Ritika Reddy <rreddy@confluent.io>

**Before/After**
```java
// Before
              previousProducerId = RecordBatch.NO_PRODUCER_ID,
        (producerIdAndEpoch.producerId == txnMetadata.previousProducerId && TransactionMetadata.isEpochExhausted(producerIdAndEpoch.epoch))
            val retryOnOverflow = !isEpochFence && txnMetadata.previousProducerId == producerId &&
          previousProducerId = value.previousProducerId,
                                                   topicPartitions: immutable.Set[TopicPartition],
  * @param previousProducerId          producer id for the last committed transaction with this transactional ID
                                               var previousProducerId: Long,

// After
              prevProducerId = RecordBatch.NO_PRODUCER_ID,
        (producerIdAndEpoch.producerId == txnMetadata.prevProducerId && TransactionMetadata.isEpochExhausted(producerIdAndEpoch.epoch))
            val retryOnOverflow = !isEpochFence && txnMetadata.prevProducerId == producerId &&
          prevProducerId = value.previousProducerId,
                                                   topicPartitions: mutable.Set[TopicPartition],
  * @param prevProducerId              producer id for the last committed transaction with this transactional ID
                                               var prevProducerId: Long,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15370: support participation in 2pc (kip-939) (1/n) (#17687).

**Test**
Test modified in TransactionCoordinatorConcurrencyTest.scala covering the corrected behaviour.
