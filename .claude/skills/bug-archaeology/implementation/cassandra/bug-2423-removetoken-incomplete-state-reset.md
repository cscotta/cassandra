# Bug 2423: Forced removetoken leaves node in inconsistent state — removingNode not cleared

**Commit**: 63da8815aa · **Ticket**: CASSANDRA-3876

## Detection Rule

When a forced/timeout path in a state machine performs cleanup, it must reset all state fields that were set at the start of the operation, not just the ones updated by the normal completion path.

## Code Smell

The timeout handler for `removetoken` calls `tokenMetadata_.removeEndpoint(endpoint)` without calling `excise()` (which also updates gossip and replication) and does not clear `removingNode`, leaving the node permanently stuck in "removing" state.

## Tags

- scope: cassandra
- category: state-not-cleaned
- code-pattern: incomplete-state-reset-on-timeout
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause

When `removetoken` timed out waiting for confirmation from other nodes, the code called `Gossiper.instance.advertiseTokenRemoved` and `tokenMetadata_.removeEndpoint`, but did not call `excise()` (which removes the endpoint from gossip state, pending ranges, etc.) and did not reset `removingNode = null`. This left the cluster in an inconsistent state: the token was removed from the metadata map but not from gossip ring state, and subsequent `removetoken` operations would be blocked because `removingNode` was still set. The fix replaces `removeEndpoint` with `excise(token, endpoint)` and adds `removingNode = null` after the loop.

## Example

```java
// BEFORE — incomplete cleanup on timeout
for (InetAddress endpoint : tokenMetadata_.getLeavingEndpoints()) {
    Gossiper.instance.advertiseTokenRemoved(endpoint, tokenMetadata_.getToken(endpoint));
    tokenMetadata_.removeEndpoint(endpoint);   // misses excise() gossip cleanup
}
replicatingNodes.clear();
// removingNode never cleared — blocks future removetoken
```

```java
// AFTER
for (InetAddress endpoint : tokenMetadata_.getLeavingEndpoints()) {
    Token token = tokenMetadata_.getToken(endpoint);
    Gossiper.instance.advertiseTokenRemoved(endpoint, token);
    excise(token, endpoint);   // full cleanup including gossip
}
replicatingNodes.clear();
removingNode = null;   // reset so future removals can proceed
```
