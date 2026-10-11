**Commit**: 7495e70365 · **Ticket**: KAFKA-16532
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
The first leader of a KRaft topic partition must rewrite the content of the bootstrap checkpoint (0-0.checkpoint) to the log so that it is replicated. Bootstrap checkpoints are not replicated to the followers. The control records for KRaftVersionRecord and VotersRecord in the bootstrap checkpoint will be written in one batch along with the LeaderChangeMessage. The leader will write these control records before accepting data records from the state machine (Controller).

**Before/After**
```java
// Before
      false,
            Optional.of(VoterSet.fromInetSocketAddresses(channel.listenerName(), voterAddresses)),
            partitionState::lastVoterSet,
            partitionState::lastKraftVersion,
        state.appendLeaderChangeMessage(currentTimeMs);
            if (fetchOffset == 0 && latestSnapshotId.isPresent()) {
                // If the follower has an empty log and a snapshot exist, it is always more efficient
                    logger.info(
                        "Fetching snapshot {} from Fetch response from leader {}",
                        snapshotId,
                        quorum.leaderIdOrSentinel()
                    );

// After
import static org.apache.kafka.snapshot.Snapshots.BOOTSTRAP_SNAPSHOT_ID;
        Optional<VoterSet> staticVoters = voterAddresses.isEmpty() ?
            Optional.empty() :
            Optional.of(VoterSet.fromInetSocketAddresses(channel.listenerName(), voterAddresses));

            staticVoters,
            partitionState,
        state.appendLeaderChangeMessageAndBootstrapRecords(currentTimeMs);
            if (fetchOffset == 0 && latestSnapshotId.isPresent() && !latestSnapshotId.get().equals(BOOTSTRAP_SNAPSHOT_ID)) {
                // If the follower has an empty log and a non-bootstrap snapshot exists, it is always more efficient
                    if (state.fetchingSnapshot().isPresent()) {
                        logger.info(
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16532; support for first leader bootstrapping the voter set (#16518).

**Test**
Test modified in KafkaMetadataLogTest.scala covering the corrected behaviour.
