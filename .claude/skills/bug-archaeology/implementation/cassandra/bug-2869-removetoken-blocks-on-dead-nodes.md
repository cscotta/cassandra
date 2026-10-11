# Bug 2869: removetoken blocks indefinitely waiting for dead nodes to acknowledge re-replication

**Commit**: 10fb692e17 · **Ticket**: CASSANDRA-1605

## Detection Rule
When a CountDownLatch or similar barrier is sized from a collection of endpoints, check that dead endpoints are excluded before sizing the latch, otherwise the operation will hang until timeout.

## Code Smell
`replicatingNodes` was populated with all endpoints that would gain new ranges (via `getNewEndpoints`), including endpoints reported as down by the failure detector. The `CountDownLatch` was sized to include those dead nodes, causing `removeToken` to block indefinitely waiting for acknowledgements that would never arrive.

## Tags
- scope: cassandra
- category: hang
- code-pattern: latch-includes-dead-endpoints
- severity: hang
- subsystem: bootstrap-startup

## Root Cause
`getNewEndpoints` collected all potential new owners without checking liveness. The fix inlines the endpoint collection loop, consults `FailureDetector.isAlive` for each endpoint, excludes dead ones from `replicatingNodes`, and logs a warning. The latch is then sized to only living nodes so `removeToken` can complete.

## Example
```java
// BEFORE — includes dead nodes in latch count
replicatingNodes = Collections.synchronizedSet(getNewEndpoints(endpoint));
replicateLatch = new CountDownLatch(replicatingNodes.size());
```
```java
// AFTER — skip dead endpoints
for (InetAddress ep : changedRanges.values()) {
    if (failureDetector.isAlive(ep))
        replicatingNodes.add(ep);
    else
        logger_.warn("Endpoint " + ep + " is down ...");
}
replicateLatch = new CountDownLatch(this.replicatingNodes.size());
```
