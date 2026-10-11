**Commit**: b4d5f163a9 · **Ticket**: KAFKA-17067
**Subsystem**: raft/src
**Category**: Resource leak

**Root cause**
Only voters should be able to transition to Candidate state. This removes VotedState as one of the EpochStates and moves voted information into UnattachedState. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
     * {@link QuorumState#transitionToVoted(int, ReplicaKey)} if vote is granted.
    private void transitionToVoted(ReplicaKey candidateKey, int epoch) {
        quorum.transitionToVoted(epoch, candidateKey);
        maybeFireLeaderChange();
        resetConnections();
        if (voteGranted && quorum.isUnattached()) {
            transitionToVoted(candidateKey, candidateEpoch);
    private long pollVoted(long currentTimeMs) {
        VotedState state = quorum.votedStateOrThrow();
        GracefulShutdown shutdown = this.shutdown.get();

        if (shutdown != null) {

// After
     * {@link QuorumState#transitionToUnattachedVotedState(int, ReplicaKey)} if vote is granted.
    private void transitionToUnattachedVoted(ReplicaKey candidateKey, int epoch) {
        quorum.transitionToUnattachedVotedState(epoch, candidateKey);
        if (voteGranted && quorum.isUnattachedNotVoted()) {
            transitionToUnattachedVoted(candidateKey, candidateEpoch);
 * Resigned transitions to:
 *    Candidate: After expiration of the election timeout
 *    Follower: After discovering a leader with an equal or larger epoch
 *
 * Unattached transitions to:
 *    Unattached: After learning of a new election with a higher epoch or after voting
                Optional.empty(),
```

**How to spot**
When reviewing raft/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17067; fix kraft transition to candidatestate (#16820).

**Test**
Test modified in KafkaRaftClientTest.java covering the corrected behaviour.
