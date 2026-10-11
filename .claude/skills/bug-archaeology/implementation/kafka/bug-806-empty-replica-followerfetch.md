**Commit**: 614bc3a19d · **Ticket**: KAFKA-17344
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add Unit Tests for an empty follower fetch for various Leader states. | TieredStorage Enabled | Leader Log Start Offset | Leader Local Log
Start Offset | Leader Log End Offset | Remarks
|

|-----------------------|-------------------------|--------------------------------|-----------------------|---------------------------------------|
| N                     | 0                       | -
| 200                   | -                                     |  | N
| 10                      | -                              | 200
| -                                     |  | Y                     | 0
| 200                            | 200                   | No segments
deleted locally           |  |...

**Before/After**
```java
// Before
// (no removals)

// After

  private def emptyReplicaState(rlmEnabled: Boolean, partition: TopicPartition, fetcher: MockFetcherThread): PartitionState = {
    // Follower begins with an empty log
    val replicaState = PartitionState(Seq(), leaderEpoch = 0, highWatermark = 0L, rlmEnabled = rlmEnabled)
    fetcher.setReplicaState(partition, replicaState)
    fetcher.addPartitions(Map(partition -> initialFetchState(topicIds.get(partition.topic), fetchOffset = 0, leaderEpoch = 0)))
    replicaState
  }

  /**
   * Test: Empty Follower Fetch with TieredStorage Disabled and Leader LogStartOffset = 0
   *
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17344: add empty replica followerfetch tests (#16884).

**Test**
Test modified in AbstractFetcherThreadTest.scala covering the corrected behaviour.
