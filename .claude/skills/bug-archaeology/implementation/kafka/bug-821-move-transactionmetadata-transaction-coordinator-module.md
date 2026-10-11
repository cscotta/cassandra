**Commit**: 990cb5c06c · **Ticket**: KAFKA-18884
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
1. Move TransactionMetadata to transaction-coordinator module. 2. Rewrite TransactionMetadata in Java.

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.transaction.{ProducerIdManager, TransactionLogConfig, TransactionState, TransactionStateManagerConfig, TxnTransitMetadata}
import scala.jdk.CollectionConverters._
            val createdMetadata = new TransactionMetadata(transactionalId = transactionalId,
              producerId = producerIdManager.generateProducerId(),
              prevProducerId = RecordBatch.NO_PRODUCER_ID,
              nextProducerId = RecordBatch.NO_PRODUCER_ID,
              producerEpoch = RecordBatch.NO_PRODUCER_EPOCH,
              lastProducerEpoch = RecordBatch.NO_PRODUCER_EPOCH,
              txnTimeoutMs = resolvedTxnTimeoutMs,
              state = TransactionState.EMPTY,
              topicPartitions = collection.mutable.Set.empty[TopicPartition],
              txnLastUpdateTimestamp = time.milliseconds(),

// After
import org.apache.kafka.coordinator.transaction.{ProducerIdManager, TransactionLogConfig, TransactionMetadata, TransactionState, TransactionStateManagerConfig, TxnTransitMetadata}
import java.util
import scala.jdk.OptionConverters._
            val createdMetadata = new TransactionMetadata(transactionalId,
              producerIdManager.generateProducerId(),
              RecordBatch.NO_PRODUCER_ID,
              RecordBatch.NO_PRODUCER_ID,
              RecordBatch.NO_PRODUCER_EPOCH,
              RecordBatch.NO_PRODUCER_EPOCH,
              resolvedTxnTimeoutMs,
              TransactionState.EMPTY,
              util.Set.of(),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18884 move transactionmetadata to transaction-coordinator module (#19699).

**Test**
Test modified in TransactionCoordinatorConcurrencyTest.scala covering the corrected behaviour.
