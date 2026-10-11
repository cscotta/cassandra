**Commit**: 6c0778aeae · **Ticket**: KAFKA-20245
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
* PR adds code in `SharePartition` to support DLQ of records (batch and
per offset) which have been REJECT acknowledged by the client. * Appropriate unit tests have been added where applicable. * The dynamic `ShareVersion` feature has been used to detect DLQ
support. * Current impl hardcodes `NoOpShareGroupDLQManager`.

**Before/After**
```java
// Before
        SharePartitionListener listener
            new SharePartitionMetrics(groupId, topicIdPartition.topic(), topicIdPartition.partition()));
        SharePartitionMetrics sharePartitionMetrics
                    offsetState.getKey(), updateResult.state().id(), (short) updateResult.deliveryCount())));
                inFlightBatch.lastOffset(), updateResult.state().id(), (short) updateResult.deliveryCount())));
                    RecordState re

// After
import org.apache.kafka.server.share.dlq.NoOpShareGroupDLQManager;
import org.apache.kafka.server.share.dlq.ShareGroupDLQ;
import org.apache.kafka.server.share.dlq.ShareGroupDLQRecordParameter;
import java.util.function.Supplier;
    /**
     * Reference to the dlq manager implementation.
     */
    private final ShareGroupDLQ shareGroupDLQ = new NoOpShareGroupDLQManager();

    /**
     * Supplier to toggle dlq support.
     */
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20245: dlq share group client reject ack records. [2/n] (#21793).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
