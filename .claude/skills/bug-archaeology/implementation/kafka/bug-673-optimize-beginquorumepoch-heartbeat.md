**Commit**: 6b187e9ff9 · **Ticket**: KAFKA-16926
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
Instead of sending out BeginQuorum requests to every voter on a cadence,
we can save on some requests by only sending to those which have not
fetched within the BeginQuorumEpoch timeout. Reviewers: José Armando García Sancio <jsancio@apache.org>, Chia-Ping
 Tsai <chia7712@apache.org>, Kuan-Po Tseng <brandboat@gmail.com>, Alyssa
 Huang <ahuang@confluent.io>

**Before/After**
```java
// Before

    private long maybeSendRequest(
            timeUntilNextBeginQuorumSend = maybeSendRequest(
                voters
                    .voterKeys()
                    .stream()
                    .filter(key -> key.id() != quorum.localIdOrThrow())
                    .collect(Collectors.toSet()),
            return maybeSendRequest(
        // begin epoch requests should be sent out every beginQuorumEpochTimeoutMs

// After

    private long maybeSendRequests(
            Set<ReplicaKey> needToSendBeginQuorumRequests = state.needToSendBeginQuorumRequests(currentTimeMs);
            timeUntilNextBeginQuorumSend = maybeSendRequests(
                needToSendBeginQuorumRequests,
            return maybeSendRequests(
    /**
     * Determines the set of replicas that should receive a {@code BeginQuorumEpoch} request
     * based on the elapsed time since their last fetch.
     * <p>
     * For each remote voter (excluding the local node), if the time since the last
     * fetch exceeds the configured {@code beginQuorumEpochTimeoutMs}, the replica
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16926: optimize beginquorumepoch heartbeat (#20318).

**Test**
Test modified in KafkaRaftClientTest.java covering the corrected behaviour.
