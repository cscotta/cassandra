**Commit**: 4d0c588539 · **Ticket**: KAFKA-17302
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
In this PR, we are adding the ReplicaFetcher changes to support fetching
from the last tiered offset. - We are modifying the TierStateMachine's start method signature to take
any offset on the leader's local log from which to start replicating
leader's logs. Previously, the start method used to build remote log
auxiliary state until the leader's local start offset. Going forward, it
will build the auxiliary state until the supplied offset.

**Before/After**
```java
// Before
import org.apache.kafka.common.message.FetchResponseData.PartitionData;
     * @param topicPartition the topic partition
     * @param currentFetchState the current PartitionFetchState which will
     *                          be used to derive the return value
     * @param fetchPartitionData the data from the fetch response that returned the offset moved to tiered storage error
     *
     * @return the new PartitionFetchState after the successful start of the
     *         tier state machine
                              PartitionFetchState currentFetchState,
                              PartitionData fetchPartitionData) throws Exception {
        OffsetAndEpoch epochAndLeaderLocalStartOffset = leader.fetchEarliestLocalOffset(topicPartition, currentFetchState.currentLeaderEpoch());
        int epoch = epochAndLeaderLocalStartOffset.epoch();

// After
import org.apache.kafka.common.Uuid;
     * @param topicPartition            the topic partition for which the tier state machine is to be started
     * @param topicId                   the optional unique identifier of the topic
     * @param currentLeaderEpoch        the current leader epoch of the partition
     * @param fetchStartOffsetAndEpoch  the offset on the leader's local log from which to start replicating logs
     * @param leaderLogStartOffset      the starting offset in the leader's log
     * @return the new PartitionFetchState after the successful start of the tier state machine
     * @throws Exception if an error occurs during the process, such as issues with remote storage
                              Optional<Uuid> topicId,
                              int currentLeaderEpoch,
                              OffsetAndEpoch fetchStartOffsetAndEpoch,
                              long leaderLogStartOffset) throws Exception {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17302: replicafetcher changes for fetching from tiered offset (#20428).

**Test**
Test modified in LocalLeaderEndPointTest.scala covering the corrected behaviour.
