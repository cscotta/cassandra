**Commit**: e68781414e · **Ticket**: KAFKA-19204
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
* Currently in the share group heartbeat flow, if we see a TP subscribed
for the first time, we move that TP to initializing state in GC and let
the GC send a persister request to share group initialize the
aforementioned TP. * However, if the coordinator runtime request for share group heartbeat
times out (maybe due to restarting/bad broker), the future completes
exceptionally resulting in persiter request to not be sent. * Now, we are in a bad state since the TP is in initializing state in GC
but not persister initialized. Future heartbeats for the same share
partitions will also not help since we do not allow retrying persister
request for initializing TPs.

**Before/After**
```java
// Before
        assertTrue(prodState.count().get() < complexCons1.recordsRead());
        Map<Uuid, Set<Integer>> alreadyInitialized = info == null ? new HashMap<>() : mergeShareGroupInitMaps(info.initializedTopics(), info.initializingTopics());
    public void testShareGroupHeartbeatNoPersisterRequestWithInitializing() {
        assertFalse(result.records().contains(
            Map.of(),
            0,
            false

// After
        assertTrue(prodState.count().get() < complexCons1.recordsRead(),
            String.format("Producer (%d) and share consumer (%d) record count mismatch.", prodState.count().get(), complexCons1.recordsRead()));

        // We are only considering initialized TPs here. This is because it could happen
        // that some topics have been moved to initializing but the corresponding persister request
        // could not be made/failed (invoked by the group coordinator). Then there would be no way to try
        // the persister call. This way we get the opportunity to retry.
        Map<Uuid, Set<Integer>> alreadyInitialized = info == null ? new HashMap<>() : info.initializedTopics();
    public void testShareGroupHeartbeatPersisterRequestWithInitializing() {
        assertTrue(result.records().contains(
            Map.of(t1Uuid, Set.of(0, 1)),
            1,
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19204: allow persister retry of initializing topics. (#19603).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
