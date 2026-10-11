**Commit**: 087028c93e · **Ticket**: KAFKA-19795
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When the FETCH request read from remote log for multiple partitions,
then all the partitions are marked with minOneMessage as true. This is
not the expected behavior:

Assume that the FETCH request is configured with fetchMaxBytes as 50 MB
and max.partition.fetch.bytes as 1 MB. And, the broker is hosting 5
partitions as leader for the topic. The FETCH request might try to read
the data from remote for all the 5 partitions.

**Before/After**
```java
// Before
  private def processRemoteFetches(remoteFetchInfos: util.HashMap[TopicIdPartition, RemoteStorageFetchInfo],
    val remoteFetchInfos = new util.HashMap[TopicIdPartition, RemoteStorageFetchInfo]()
      // Once we read from a non-empty partition, we stop ignoring request and partition level size limits
      if (recordBatchSize > 0)
        minOneMessage = false
      val capturedTopicPartitions = capturedFetchInfos.map(_.topicIdPartition.topicPartition).toSet
      assertTrue(capturedTopicPartitions.contains(tp0), "Should contain " + tp0)
      val capturedTopicPartitions = capturedFetchInfos.map(_.topicIdPartition.topicPartition).toSet
      assertTrue(capturedTopicPartitions.contains(tp0), "Should contain " + tp0)

// After
  private def processRemoteFetches(remoteFetchInfos: util.LinkedHashMap[TopicIdPartition, RemoteStorageFetchInfo],
    val remoteFetchInfos = new util.LinkedHashMap[TopicIdPartition, RemoteStorageFetchInfo]()
      // Once we read from a non-empty partition, we stop ignoring request and partition level size limits
      if (estimatedRecordBatchSize > 0)
        minOneMessage = false
      val capturedTopicPartitions = capturedFetchInfos.map(_.topicIdPartition.topicPartition)
      assertEquals(tp0, capturedTopicPartitions.head, "Should contain " + tp0 + " as first item")
          assertFalse(fetchInfo.minOneMessage())
      val capturedTopicPartitions = capturedFetchInfos.map(_.topicIdPartition.topicPartition)
      assertEquals(tp0, capturedTopicPartitions.head, "Should contain " + tp0 + " as first item")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19795: mark the minonemessage as false when delayedremotefetch is present in the first partition. (#20706).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
