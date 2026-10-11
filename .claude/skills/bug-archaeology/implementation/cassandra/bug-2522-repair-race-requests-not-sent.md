# Bug 2522: Race in AntiEntropyService where Merkle tree reply arrives before requests are fully dispatched

**Commit**: c93ca8e1a9 · **Ticket**: CASSANDRA-3400

## Detection Rule
When a background thread dispatches requests to N endpoints and a concurrent callback processes replies that reference the request set, use a condition variable or latch to ensure all requests are registered before any reply is processed.

## Code Smell
`addTree()` is `synchronized` and checks `requestedEndpoints.remove(request.endpoint)` but `requestedEndpoints` is populated in `run()` which runs on a different thread — a very fast remote node can call back and invoke `addTree()` before `run()` finishes adding all endpoints, causing the endpoint to be removed and the tree count to be wrong.

## Tags
- scope: distributed-systems
- category: race-condition
- code-pattern: reply-before-request-fully-registered
- severity: silent-wrong-result
- subsystem: repair

## Root Cause
`RepairJob.run()` iterates `requestedEndpoints` and sends a validation request to each node. A reply from a fast node would invoke `addTree()`, which removes the responding endpoint from `requestedEndpoints` and checks whether all trees have arrived. If `run()` has not yet dispatched all requests at that point, the count is off: the job may incorrectly conclude that all trees have been received and start syncing with an incomplete set of Merkle trees. The fix adds a `SimpleCondition requestsSent` that `run()` signals after the dispatch loop, and `addTree()` awaits it before processing.

## Example
```java
// BEFORE — addTree can run before all requests are sent
public synchronized int addTree(TreeRequest request, MerkleTree tree) {
    trees.put(request.endpoint, tree);
    requestedEndpoints.remove(request.endpoint);
    return requestedEndpoints.size();
}
```
```java
// AFTER — addTree waits until run() has finished dispatching
public synchronized int addTree(TreeRequest request, MerkleTree tree) {
    requestsSent.await();   // blocks until all requests dispatched
    trees.put(request.endpoint, tree);
    requestedEndpoints.remove(request.endpoint);
    return requestedEndpoints.size();
}
```
