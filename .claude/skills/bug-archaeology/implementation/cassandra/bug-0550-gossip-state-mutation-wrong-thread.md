# Bug 0550: Gossip State Mutations From Wrong Threads Cause Data Races

**Commit**: c3ce32e239 · **Ticket**: CASSANDRA-15059
**Subsystem**: `src/java/org/apache/cassandra/gms/Gossiper.java`, `service/StorageService.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

`Gossiper` is documented as single-threaded (all mutations must occur on the gossip stage), but several callers — including `StorageService`, the failure detector's `convict` callback, and the fat-client eviction loop — mutated gossip state directly from arbitrary threads. Because `endpointStateMap`, `unreachableEndpoints`, and related fields are not thread-safe collections, concurrent modifications could corrupt endpoint state or lose updates. The fix routes all mutations through a `runInGossipStageBlocking` helper that dispatches to the gossip stage executor if the caller is not already on that thread, and adds a `checkProperThreadForStateMutation` guard that logs or throws (under strict-checks mode) when a state change is attempted from an unexpected thread.

### Before / After

```java
// BEFORE (StorageService.java)
Gossiper.instance.removeEndpoint(endpoint);

// BEFORE (Gossiper.convict, called from FailureDetector thread)
EndpointState epState = endpointStateMap.get(endpoint);
// ... direct mutation
markDead(endpoint, epState);
```

```java
// AFTER
Gossiper.runInGossipStageBlocking(() -> Gossiper.instance.removeEndpoint(endpoint));

// AFTER (convict)
runInGossipStageBlocking(() -> {
    EndpointState epState = endpointStateMap.get(endpoint);
    // ...
    markDead(endpoint, epState);
});
```

---

## How to Spot

Classes documented as single-threaded or "stage-confined" need a thread-ownership check at every entry point that mutates shared state. Look for callers that directly invoke instance methods on a stage-confined object from code that runs on a different executor or callback thread (failure detectors, timer callbacks, RPC handlers). A quick audit is to search for all call sites of the class's mutation methods and verify each is either already on the correct stage or routes through a dispatch helper.

---

## Test

Existing gossip and locator unit tests now disable thread validation via `Gossiper.Props.DISABLE_THREAD_VALIDATION` so they continue to work in single-threaded test environments; the strict check is enabled in build.xml for CI runs.
