**Commit**: 7339d65b27 · **Ticket**: KAFKA-19354
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
Observers may get stuck fetching from bootstrap servers even on
discovery of a leader from a fetch response. Observers currently fetch from bootstrap servers once their fetch
timeout expires, and even on rediscovery of the leader (e.g. observer
receives fetch response indicating location of leader), observers will
not  attempt to resume fetching from the leader. This change simplifies observer polling logic to just rely on
maybeSendFetchToBestNode which takes care of the logic of selecting
either the leader or bootstrap servers  as the destination based on
timeouts and backoffs.

**Before/After**
```java
// Before
        fetchTimer.update(currentTimeMs);
        fetchTimer.reset(fetchTimeoutMs);
                leaderId.isPresent() &&
        if (state.hasFetchTimeoutExpired(currentTimeMs)) {
            return maybeSendFetchToAnyBootstrap(currentTimeMs);
        } else {
            return maybeSendFetchToBestNode(state, currentTimeMs);
        }
    public void testObserverSendDiscoveryFetchAfterFetchTimeout(boolean withKip853Rpc) throws Exception {
        int epoch = 5;
        context.assertFetchRequestData(fetchRequest, 0, 0L, 0, context.client.highWatermark());
        context.time.sleep(context.fetchTimeoutMs);

// After
    /**
     * Reset the fetch timeout after successful fetch from leader.
     */
        overrideFetchTimeout(currentTimeMs, fetchTimeoutMs);
    /**
     * Override the fetch timeout to a specific value. This is useful for short-circuiting followers' timeouts after
     * they receive end quorum requests
     */
            leaderId.isPresent() &&
        return maybeSendFetchToBestNode(state, currentTimeMs);
    public void testObserverUnattachedSendFetchToBootstrapServers(boolean withKip853Rpc) throws Exception {
        int epoch = 0;
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19354: kraft observer should send fetch to best node (#19854).

**Test**
Test modified in KafkaRaftClientTest.java covering the corrected behaviour.
