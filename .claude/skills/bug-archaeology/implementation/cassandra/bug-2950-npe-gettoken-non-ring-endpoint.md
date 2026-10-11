# Bug 2950: NPE when getToken() called on endpoint not yet in token ring

**Commit**: 46e5f23e1b · **Ticket**: CASSANDRA-522

## Detection Rule
When iterating load-info entries to find a max-load node, check whether all entries are guaranteed to be members of the token ring before calling `getToken()` on them.

## Code Smell
`BootStrapper.guessTokenIfNotSpecified` iterates `slb.getLoadInfo().entrySet()` and calls `ss.updateToken()` whose implementation calls `tokenMetadata_.getToken(endpoint)` — which asserts `isMember(endpoint)`, causing NPE/assertion failure for endpoints that gossip load-info before they join the ring.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: unguarded-getToken-call
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
The `StorageLoadBalancer` gossips load information for all nodes it hears about, including nodes that may be bootstrapping or otherwise not yet part of the official token ring. When selecting a high-load node to take token from, the code did not filter out non-ring endpoints. Calling `tokenMetadata_.getToken()` on such an endpoint returned null (and later triggered an assertion). The fix skips entries where `!metadata.isMember(entry.getKey())`.

## Example
```java
// BEFORE: iterates all load entries without checking ring membership
for (Map.Entry<InetAddress, Double> entry : slb.getLoadInfo().entrySet()) {
    if (maxEndpoint == null || entry.getValue() > maxLoad) {
        maxEndpoint = entry.getKey();  // may not be in ring
        maxLoad = entry.getValue();
    }
}
```
```java
// AFTER: skip non-ring endpoints
for (Map.Entry<InetAddress, Double> entry : slb.getLoadInfo().entrySet()) {
    if (!metadata.isMember(entry.getKey()))
        continue;
    if (maxEndpoint == null || entry.getValue() > maxLoad) { ... }
}
```
