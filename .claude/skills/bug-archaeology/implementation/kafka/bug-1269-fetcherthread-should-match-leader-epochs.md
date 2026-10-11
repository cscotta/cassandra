**Commit**: 80d99ea2ba · **Ticket**: KAFKA-18991
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
This PR fixes a potential issue where the `FetchResponse` returns
`divergingEndOffsets` with an older leader epoch. This can lead to
committed records being removed from the follower's log, potentially
causing data loss. In detail:
`processFetchRequest` gets the requested leader epoch of partition data
by `topicPartition` and compares it with the leader epoch of the current
fetch state. If they don't match, the response is ignored.

**Before/After**
```java
// Before
  private def processFetchRequest(sessionPartitions: util.Map[TopicPartition, FetchRequest.PartitionData],
            // In this case, we only want to process the fetch response if the partition state is ready for fetch and
            // the current offset is the same as the offset requested.
                        fetchPartitionData.currentLeaderEpoch.orElse(currentFetchState.currentLeaderEpoch),
}

// After
  // visible for testing
  private[server] def processFetchRequest(sessionPartitions: util.Map[TopicPartition, FetchRequest.PartitionData],
            // In this case, we only want to process the fetch response if:
            // - the partition state is ready for fetch
            // - the current offset is the same as the offset requested
            // - the current leader epoch is the same as the leader epoch requested
                fetchPartitionData.currentLeaderEpoch.map[Boolean](_ == currentFetchState.currentLeaderEpoch).orElse(true) &&
                        currentFetchState.currentLeaderEpoch,
  @Test
  def testIgnoreFetchResponseWhenLeaderEpochChanged(): Unit = {
    val newEpoch = 1
    val initEpoch = 0
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18991: fetcherthread should match leader epochs between fetch request and fetch state (#19223).

**Test**
Test modified in AbstractFetcherThreadTest.scala covering the corrected behaviour.
