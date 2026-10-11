**Commit**: e658d545d5 · **Ticket**: KAFKA-19843
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
* KIP-1222 introduced the notion of a RENEW ack type which allows
renewal of existing acquisition lock timeout by sending the ack type
RENEW for a specific record in share consumer explicit mode. * In this PR, we have added impl in the `SharePartition` which performs
the existing lock cancellation and refresh for batches as well as per
offset. * Unit tests have been added in `SharePartitionTest`. Reviewers: Abhinav Dixit <adixit@confluent.io>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
                Map<Long, RecordState> recordStateMap;
                    recordStateMap = fetchRecordStateMapForAcknowledgementBatch(batch);
                    recordStateMap,
    private Map<Long, RecordState> fetchRecordStateMapForAcknowledgementBatch(
        ShareAcknowledgementBatch batch) {
        Map<Long, RecordState> recordStateMap = new HashMap<>();
            recordStateMap.put(batch.firstOffset() + index,
                fetchRecordState(batch.acknowledgeTypes().get(index)));
        return recordStateMap;
    }

    private static RecordState fetchRecordState(byte acknowledgeType) {

// After
import org.apache.kafka.clients.consumer.AcknowledgeType;
    /**
     * To provide static mapping between acknowledgement type bytes to RecordState.
     */
    private static final Map<Byte, RecordState> ACK_TYPE_TO_RECORD_STATE = Map.of(
        (byte) 0, RecordState.ARCHIVED,                             // Represents gap
        AcknowledgeType.ACCEPT.id, RecordState.ACKNOWLEDGED,
        AcknowledgeType.RELEASE.id, RecordState.AVAILABLE,
        AcknowledgeType.REJECT.id, RecordState.ARCHIVED
    );

                Map<Long, Byte> ackTypeMap;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19843: sharepartition renew ack impl. (#20815).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
