**Commit**: b89999b504 · **Ticket**: KAFKA-16483
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
The method createPutCacheCallback has a input argument preAppendErrors. It is used to keep the "error" happens before appending. However, it is always empty. Also, the pre-append error is handled before createPutCacheCallback by calling responseCallback.

**Before/After**
```java
// Before
import kafka.server.{LogAppendResult, ReplicaManager, RequestLocal}
                             group: GroupMetadata,
                             consumerId: String,
                             offsetMetadata: immutable.Map[TopicIdPartition, OffsetAndMetadata],
                             filteredOffsetMetadata: Map[TopicIdPartition, OffsetAndMetadata],
                             responseCallback: immutable.Map[TopicIdPartition, Errors] => Unit,
                             producerId: Long,
                             records: Map[TopicPartition, MemoryRecords],
                             preAppendErrors: Map[TopicPartition, LogAppendResult] = Map.empty): Map[TopicPartition, PartitionResponse] => Unit = {
        else if (preAppendErrors.contains(topicIdPartition.topicPartition))
          (topicIdPartition, preAppendErrors(topicIdPartition.topicPartition).error)

// After
import kafka.server.{ReplicaManager, RequestLocal}
                                     group: GroupMetadata,
                                     consumerId: String,
                                     offsetMetadata: immutable.Map[TopicIdPartition, OffsetAndMetadata],
                                     filteredOffsetMetadata: Map[TopicIdPartition, OffsetAndMetadata],
                                     responseCallback: immutable.Map[TopicIdPartition, Errors] => Unit,
                                     producerId: Long,
                                     records: Map[TopicPartition, MemoryRecords]): Map[TopicPartition, PartitionResponse] => Unit = {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16483: remove preappenderrors from createputcachecallback (#16105).

**Test**
No test added.
