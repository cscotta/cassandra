**Commit**: 0d9ae4928b · **Ticket**: KAFKA-17302
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
In this PR, we are adding the methods required for supporting follower
fetch from Last Tiered Offset. - Added new method `shouldFetchFromLastTieredOffset` in
AbstractFetcherThread to determine if the folllower should fetch from
the last tiered offset for the remote storage enabled topic
- Added new API `fetchEarliestPendingUploadOffset` to LeaderEndPoint for
retrieving the offset from where it should start replicating data from
the leader. Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  protected def shouldFetchFromLastTieredOffset(topicPartition: TopicPartition, leaderEndOffset: Long, replicaEndOffset: Long): Boolean

  override def fetchEarliestPendingUploadOffset(topicPartition: TopicPartition, currentLeaderEpoch: Int): OffsetAndEpoch = {
    val partition = replicaManager.getPartitionOrException(topicPartition)
    val log = partition.localLogOrException

    if (!log.remoteLogEnabled()) {
      new OffsetAndEpoch(-1L, -1)
    } else {
      val highestRemoteOffset = log.highestOffsetInRemoteStorage()
      val logStartOffset = fetchEarliestOffset(topicPartition, currentLeaderEpoch)

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17302: apis for follower fetch from last tiered offset (#21153).

**Test**
Test modified in LocalLeaderEndPointTest.scala covering the corrected behaviour.
