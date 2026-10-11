# Bug 2789: Decommission Race — Endpoint State Removed Too Early

**Commit**: 15cf72c55f · **Ticket**: CASSANDRA-2072

## Detection Rule
When `removeEndpoint()` deletes endpoint state from `endpointStateMap` immediately rather than deferring removal until after the quarantine period expires, other nodes can re-gossip the endpoint back as live before the quarantine is over.

## Code Smell
`removeEndpoint()` contains `endpointStateMap_.remove(endpoint)` alongside `justRemovedEndpoints_.put(endpoint, ...)`. The quarantine logic in the task scheduler also eventually fires `justRemovedEndpoints_.remove(entry.getKey())` but without the paired `endpointStateMap_.remove`.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: premature-state-removal
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
During decommission, a node broadcasts its LEFT status and then `removeEndpoint` is called locally. The original code removed the endpoint's gossip state immediately from `endpointStateMap_`. However, other nodes still gossip this endpoint's last known state, which caused the local node to re-accept gossip updates for the departed endpoint (rehydrating its state map entry) and potentially route traffic to it. By deferring the `endpointStateMap_.remove()` until the quarantine delay expires (moved to the scheduled task that clears `justRemovedEndpoints_`), the endpoint remains in the map (as removed/LEFT) long enough to block re-insertion. Also, `decommission()` was sleeping only `2 * intervalInMillis` (~2 seconds) before LEFT status propagated; the fix changes this to the full `RING_DELAY` (30 s by default).

## Example
```java
// BEFORE: state removed immediately, quarantine has nothing to clean up
private void removeEndpoint(InetAddress endpoint) {
    liveEndpoints_.remove(endpoint);
    unreachableEndpoints_.remove(endpoint);
    endpointStateMap_.remove(endpoint);   // too early
    FailureDetector.instance.remove(endpoint);
    justRemovedEndpoints_.put(endpoint, System.currentTimeMillis());
}
// quarantine expiry: justRemovedEndpoints_.remove(entry.getKey()); (no map cleanup needed)
```
```java
// AFTER: state kept until quarantine expires
private void removeEndpoint(InetAddress endpoint) {
    liveEndpoints_.remove(endpoint);
    unreachableEndpoints_.remove(endpoint);
    // do not remove endpointState until the quarantine expires
    FailureDetector.instance.remove(endpoint);
    justRemovedEndpoints_.put(endpoint, System.currentTimeMillis());
}
// quarantine expiry:
justRemovedEndpoints_.remove(entry.getKey());
endpointStateMap_.remove(entry.getKey()); // now deferred here
```
