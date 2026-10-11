**Commit**: fd70290633 · **Ticket**: KAFKA-18486
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The included tests are as follows:

- testFencedErrorCausedByBecomeLeader
- testFetchBeyondHighWatermark
- testFetchFollowerNotAllowedForOlderClients
- testFetchFromFollowerShouldNotRunPreferLeaderSelect
- testFetchFromLeaderAlwaysAllowed
- testFetchMessagesWhenNotFollowerForOnePartition
- testFetchRequestRateMetrics
- testFetchShouldReturnImmediatelyWhenPreferredReadReplicaIsDefined
- testFollowerFetchWithDefaultSelectorNoForcedHwPropagation
- testFollowerStateNotUpdatedIfLogReadFails

I removed `testFetchMessagesWithInconsistentTopicId ` as it's no longer
needed, the "topicId" is now mandatory and cannot be null in our new
implementation. Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, L...

**Before/After**
```java
// Before
  @Test
  def testFencedErrorCausedByBecomeLeader(): Unit = {
    testFencedErrorCausedByBecomeLeader(0)
    testFencedErrorCausedByBecomeLeader(1)
    testFencedErrorCausedByBecomeLeader(10)
  }

  private[this] def testFencedErrorCausedByBecomeLeader(loopEpochChange: Int): Unit = {
      val brokerList = Seq[Integer](0, 1).asJava
      def leaderAndIsrRequest(epoch: Int): LeaderAndIsrRequest = new LeaderAndIsrRequest.Builder(0, 0, brokerEpoch,
        Seq(new LeaderAndIsrRequest.PartitionState()
          .setTopicName(topic)

// After
  @ParameterizedTest
  @ValueSource(ints = Array(0, 1, 10))
  def testFencedErrorCausedByBecomeLeader(loopEpochChange: Int): Unit = {
    val localId = 0
      val leaderDelta = topicsCreateDelta(localId, isStartIdLeader = true, topicName = topic, topicId = topicIds(topic))
      val leaderImage = imageFromTopics(leaderDelta.apply())
      replicaManager.applyDelta(leaderDelta, leaderImage)
      (1 to loopEpochChange).foreach(
        epoch => {
          val leaderDelta = topicsCreateDelta(localId, isStartIdLeader = true, topicName = topic, topicId = topicIds(topic), leaderEpoch = epoch)
          replicaManager.applyDelta(leaderDelta, imageFromTopics(leaderDelta.apply()))
        }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18486 remove replicamanager#becomeleaderorfollower from testfencederrorcausedbybecomeleader and other similar meth.

**Test**
Test modified in ReplicaManagerTest.scala covering the corrected behaviour.
