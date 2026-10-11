**Commit**: 9fc7500684 · **Ticket**: KAFKA-18770
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
it's crucial to utilize a try-finally block to ensure proper closure of the ReplicaManager. Failing to do so can result in an unreleased thread from the purgatory, potentially leading to errors in subsequent integration tests that incorporate thread leak detection. Reviewers: poorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    val groupId = "grp"
    val tp1 = new TopicIdPartition(Uuid.randomUuid, new TopicPartition("foo1", 0))
    val partitionMaxBytes = new util.LinkedHashMap[TopicIdPartition, Integer]
    partitionMaxBytes.put(tp1, 1000)

    val sp1 = mock(classOf[SharePartition])
    val sharePartitions = new util.LinkedHashMap[TopicIdPartition, SharePartition]
    sharePartitions.put(tp1, sp1)

    val future = new CompletableFuture[util.Map[TopicIdPartition, ShareFetchResponseData.PartitionData]]
    val shareFetch = new ShareFetch(
      new FetchParams(ApiKeys.SHARE_FETCH.latestVersion, FetchRequest.ORDINARY_CONSUMER_ID, -1, 500, 1, 1024 * 1024, FetchIsolation.HIGH_WATERMARK, Optional.empty, true),

// After
    try {
      val groupId = "grp"
      val tp1 = new TopicIdPartition(Uuid.randomUuid, new TopicPartition("foo1", 0))
      val partitionMaxBytes = new util.LinkedHashMap[TopicIdPartition, Integer]
      partitionMaxBytes.put(tp1, 1000)

      val sp1 = mock(classOf[SharePartition])
      val sharePartitions = new util.LinkedHashMap[TopicIdPartition, SharePartition]
      sharePartitions.put(tp1, sp1)

      val future = new CompletableFuture[util.Map[TopicIdPartition, ShareFetchResponseData.PartitionData]]
      val shareFetch = new ShareFetch(
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18770 close the rm created by testdelayedsharefetchpurgatoryoperationexpiration (#18853).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
