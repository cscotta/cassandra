**Commit**: eb378da99c · **Ticket**: KAFKA-19462
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Estimate the fetch size for remote fetch to avoid to exceed the
`fetch.max.bytes` config. We don't want to query the remoteLogMetadata
during API handling, thus we assume the remote fetch can get
`max.partition.fetch.bytes` size. Tests added. Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
      limitBytes = math.max(0, limitBytes - recordBatchSize)
      val topicIds = Map(tp0.topic -> topicId).asJava

      val params = new FetchParams(replicaId, 1, 1000, 0, 100, FetchIsolation.LOG_END, Optional.empty)
      // when reading log, it'll throw OffsetOutOfRangeException, which will be handled separately
      val result = replicaManager.readFromLog(params, Seq(tidp0 -> new PartitionData(topicId, 1, 0, 100000, Optional.of[Integer](leaderEpoch), Optional.of[Integer](leaderEpoch))), UNBOUNDED_QUOTA, false)

      if (isFromFollower) {
        // expect OFFSET_MOVED_TO_TIERED_STORAGE error returned if it's from follower, since the data is already available in remote log
        assertEquals(Errors.OFFSET_MOVED_TO_TIERED_STORAGE, result.head._2.error)
      } else {
        assertEquals(Errors.NONE, result.head._2.error)

// After
      // Because we don't know how much data will be retrieved in remote fetch yet, and we don't want to block the API call
      // to query remoteLogMetadata, assume it will fetch the max bytes size of data to avoid to exceed the "fetch.max.bytes" setting.
      val estimatedRecordBatchSize = if (recordBatchSize == 0 && readResult.info.delayedRemoteStorageFetch.isPresent)
        readResult.info.delayedRemoteStorageFetch.get.fetchMaxBytes else recordBatchSize
      limitBytes = math.max(0, limitBytes - estimatedRecordBatchSize)
  private val topic2 = "test-topic2"
  private val topicId2 = Uuid.randomUuid()
    val fetchMaxBytes = 150
    val partitionMaxBytes = 100
    val tp02 = new TopicPartition(topic2, 0)
    val tidp02 = new TopicIdPartition(topicId2, tp02)
      replicaManager.createPartition(tp02).createLogIfNotExists(isNew = false, isFutureReplica = false, offsetCheckpoints, None)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19462: count fetch size when remote fetch (#20088).

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
