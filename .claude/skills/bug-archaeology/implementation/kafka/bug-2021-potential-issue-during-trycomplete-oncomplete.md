**Commit**: 77cc8ffbab · **Ticket**: KAFKA-17948
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
import java.util.HashMap;

    private Map<TopicIdPartition, FetchRequest.PartitionData> partitionsAcquired;
    private Map<TopicIdPartition, LogReadResult> partitionsAlreadyFetched;
        if (shareFetch.isCompleted())
            return;
        Map<TopicIdPartition, FetchRequest.PartitionData> topicPartitionData;
        // tryComplete did not invoke forceComplete, so we need to check if we have any partitions to fetch.
        if (partitionsAcquired.isEmpty())
            topicPartitionData = acquirablePartitions();
        // tryComplete invoked forceComplete, so we can use the data from tryComplete.
        else

// After
import java.util.concurrent.locks.Lock;
    private LinkedHashMap<TopicIdPartition, FetchRequest.PartitionData> partitionsAcquired;
    private LinkedHashMap<TopicIdPartition, LogReadResult> partitionsAlreadyFetched;
        // We are utilizing lock so that onComplete doesn't do a dirty read for global variables -
        // partitionsAcquired and partitionsAlreadyFetched, since these variables can get updated in a different tryComplete thread.
        lock.lock();
        try {
            LinkedHashMap<TopicIdPartition, FetchRequest.PartitionData> topicPartitionData;
            // tryComplete did not invoke forceComplete, so we need to check if we have any partitions to fetch.
            if (partitionsAcquired.isEmpty())
                topicPartitionData = acquirablePartitions();
            // tryComplete invoked forceComplete, so we can use the data from tryComplete.
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17948: potential issue during trycomplete and oncomplete simultaneous calls to access global variables (#17739).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
