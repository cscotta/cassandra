**Commit**: e979fce94e · **Ticket**: KAFKA-17030
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
Because the set of voters are dynamic (KIP-953), it is possible for a replica to believe they are a voter while the current leader doesn't have that replica in the voter set. In this replicated state, the leader will not sent BeginQuorumEpoch requests to such a replica. This means that such replicas will not be able to discover the leader. This change will help Unattached rediscover the leader by sending Fetch requests to the the bootstrap servers.

**Before/After**
```java
// Before
    private long maybeSendAnyVoterFetch(long currentTimeMs) {
            backoffMs = maybeSendFetchOrFetchSnapshot(state, currentTimeMs);
            return maybeSendAnyVoterFetch(currentTimeMs);
            final long backoffMs;

            // If the current leader is backing off due to some failure or if the
            // request has timed out, then we attempt to send the Fetch to another
            // voter in order to discover if there has been a leader change.
            Node leaderNode = state.leaderNode(channel.listenerName());
            if (requestManager.hasRequestTimedOut(leaderNode, currentTimeMs)) {
                // Once the request has timed out backoff the connection
                requestManager.reset(leaderNode);

// After
    private long maybeSendFetchToAnyBootstrap(long currentTimeMs) {
            backoffMs = maybeSendFetchToBestNode(state, currentTimeMs);
            return maybeSendFetchToAnyBootstrap(currentTimeMs);
            return maybeSendFetchToBestNode(state, currentTimeMs);
        }
    }
    private long maybeSendFetchToBestNode(FollowerState state, long currentTimeMs) {
        // If the current leader is backing off due to some failure or if the
        // request has timed out, then we attempt to send the Fetch to another
        // voter in order to discover if there has been a leader change.
        final long backoffMs;
        Node leaderNode = state.leaderNode(channel.listenerName());
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17030; unattached voters will fetch from bootstrap servers (#17352).

**Test**
Test modified in KafkaRaftClientTest.java covering the corrected behaviour.
