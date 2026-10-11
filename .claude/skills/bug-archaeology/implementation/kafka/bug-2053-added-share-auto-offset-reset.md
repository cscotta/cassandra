**Commit**: 9db5ed00a8 · **Ticket**: KAFKA-16726
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR adds another dynamic config share.auto.offset.reset fir share groups. Reviewers:  Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>,  Abhinav Dixit <adixit@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        return timestampAndOffset.isEmpty() ? (long) 0 : timestampAndOffset.get().offset;
                // Set the state epoch and end offset from the persisted state.
                startOffset = partitionData.startOffset() != -1 ? partitionData.startOffset() : 0;
                    updateEndOffsetAndResetFetchOffsetMetadata(partitionData.startOffset());

// After
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.errors.OffsetNotAvailableException;
import scala.Some;
        if (timestampAndOffset.isEmpty()) {
            throw new OffsetNotAvailableException("offset for Earliest timestamp not found for topic partition: " + topicIdPartition);
        }
        return timestampAndOffset.get().offset;
    }

    /**
     * The method is used to get the offset for the latest timestamp for the topic-partition.
     *
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16726: added share.auto.offset.reset dynamic config for share groups (#17573).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
