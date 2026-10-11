**Commit**: 9f7afafefe · **Ticket**: KAFKA-16529
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
This change implements response handling for the new version of Vote, Fetch, FetchSnapshot, BeginQuorumEpoch and EndQuorumEpoch. All of these responses were extended to include the leader's endpoint when the leader is known. This change also includes sending the new version of the requests for Vote, Fetch, FetchSnapshot, BeginQuorumEpoch and EndQuorumEpoch. The two most notable changes are that:
1.

**Before/After**
```java
// Before
import java.util.Collections;
import java.util.function.UnaryOperator;
    /**
     * Creates a FetchSnapshotRequestData with a single PartitionSnapshot for the topic partition.
     *
     * The partition index will already be populated when calling operator.
     *
     * @param topicPartition the topic partition to include
     * @param operator unary operator responsible for populating all the appropriate fields
     * @return the created fetch snapshot request data
     */
    public static FetchSnapshotRequestData singleton(

// After
import org.apache.kafka.common.record.UnalignedMemoryRecords;
        } else if (records instanceof UnalignedMemoryRecords) {
            UnalignedMemoryRecords memRecords = (UnalignedMemoryRecords) records;
            writeByteBuffer(memRecords.buffer());
    private static final UnalignedMemoryRecords EMPTY = new UnalignedMemoryRecords(ByteBuffer.allocate(0));
    public static UnalignedMemoryRecords empty() {
        return EMPTY;
    }
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16529; implement raft response handling (#16454).

**Test**
Test modified in CandidateState.java covering the corrected behaviour.
