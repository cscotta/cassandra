**Commit**: b0054f3a2f · **Ticket**: KAFKA-16536
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
With KIP-853, the leader's endpoint is sent to the other voters using the BeginQuorumEpoch RPC. The remote replicas never store the leader's endpoint. That means that leaders need to resend the leader's endpoint if a voter restarts. This change accomplishes this by sending the BeginQuorumEpoch as a heartbeat.

**Before/After**
```java
// Before
        long timeUntilSend = maybeSendRequests(
            currentTimeMs,
            partitionState
                .lastVoterSet()
                .voterNodes(state.nonAcknowledgingVoters().stream(), channel.listenerName()),
            this::buildBeginQuorumEpochRequest
        return Math.min(timeUntilFlush, Math.min(timeUntilSend, timeUntilCheckQuorumExpires));
    public Set<Integer> nonAcknowledgingVoters() {
        return  state.remainingBackoffMs(timeMs);

// After
    private long maybeSendBeginQuorumEpochRequests(
        LeaderState<T> state,
        long currentTimeMs
    ) {
        long timeUntilNextBeginQuorumSend = state.timeUntilBeginQuorumEpochTimerExpires(currentTimeMs);
        if (timeUntilNextBeginQuorumSend == 0) {
            VoterSet lastVoterSet = partitionState.lastVoterSet();
            timeUntilNextBeginQuorumSend = maybeSendRequests(
                currentTimeMs,
                lastVoterSet.voterNodes(lastVoterSet.voterIds().stream().filter(id -> id != quorum.localIdOrThrow()), channel.listenerName()),
                this::buildBeginQuorumEpochRequest
            );
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16536; use beginquorumepoch as leader heartbeat (#16399).

**Test**
Test modified in KafkaRaftClientTest.java covering the corrected behaviour.
