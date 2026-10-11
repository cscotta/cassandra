# Bug 2631: Hint replay triggered on gossip token flag instead of ring membership, causing premature or missed delivery

**Commit**: 152ee37e22 · **Ticket**: CASSANDRA-2928

## Detection Rule
When deciding whether to replay hints to a node that has come alive, check authoritative ring membership via `TokenMetadata.isMember()` rather than a gossip-state flag like `hasToken()`, which may be stale or set before the node is fully joined.

## Code Smell
`onAlive()` calls `state.hasToken()` on the `EndpointState` gossip object to decide whether to deliver hints; gossip state can lag behind or precede actual token metadata membership, so hints may be delivered too early or not at all.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: stale-gossip-flag-check
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
`StorageService.onAlive()` checked `state.hasToken()` (an `EndpointState` gossip flag) to determine if a newly-alive endpoint should receive replayed hints. This flag was set by gossip processing and might not accurately reflect whether the node was a full member of the token ring. The correct check is `TokenMetadata.isMember(endpoint)`, which consults the authoritative in-memory ring state. Using the gossip flag could cause hint replay to be skipped when the flag was stale-false, or triggered prematurely when gossip raced ahead of token metadata population.

## Example
```java
// BEFORE
if (!isClientMode && state.hasToken())
    deliverHints(endpoint);

// AFTER
if (!isClientMode && StorageService.instance.getTokenMetadata().isMember(endpoint))
    deliverHints(endpoint);
```
