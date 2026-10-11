**Commit**: 4817eb9227 · **Ticket**: KAFKA-15344
**Subsystem**: streams/src
**Category**: State not cleaned up

**Root cause**
This PR augments Streams messages with leader epoch. In case of empty buffer queues, the last offset and leader epoch are retrieved from the streams task 's cache of nextOffsets. Co-authored-by: Lucas Brutschy <lbrutschy@confluent.io>
Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import java.util.Optional;
                Optional.empty()

// After
import java.util.Optional;
    abstract Optional<Integer> headRecordLeaderEpoch(final TopicPartition partition);

import java.util.Optional;
    @Override
    Optional<Integer> headRecordLeaderEpoch(final TopicPartition partition) {
        final RecordQueue recordQueue = partitionQueues.get(partition);

        if (recordQueue == null) {
            throw new IllegalStateException("Partition " + partition + " not found.");
        }

```

**How to spot**
When reviewing streams/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15344: streams task should cache consumer nextoffsets (#17091).

**Test**
Test modified in PartitionGroupTest.java covering the corrected behaviour.
