# Bug 2868: Token not removed from SystemTable on decommission and removetoken

**Commit**: 9170ea25f1 · **Ticket**: CASSANDRA-1609

## Detection Rule
When multiple code paths handle the same lifecycle event (node leaving, node removed), check that each path calls the complete set of cleanup operations including persistent-storage cleanup.

## Code Smell
`handleStateLeft` and the `REMOVED_TOKEN` branch of `handleStateRemoving` each had their own partial copy of the cleanup sequence but omitted `SystemTable.removeToken` and/or `Gossiper.instance.removeEndpoint`, so token state leaked into the system table across restarts.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: partial-cleanup-in-duplicated-code
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
Token cleanup logic was copy-pasted across three methods (`handleStateLeft`, `handleStateRemoving`, `removeToken`) with each copy missing one or more steps. Extracting a single `excise(token, endpoint)` helper that always calls `Gossiper.removeEndpoint`, `tokenMetadata_.removeEndpoint`, `HintedHandOffManager.deleteHints`, `tokenMetadata_.removeBootstrapToken`, `calculatePendingRanges`, and `SystemTable.removeToken` fixed all three call sites.

## Example
```java
// BEFORE (handleStateLeft) — missing Gossiper.removeEndpoint and SystemTable.removeToken
tokenMetadata_.removeEndpoint(endpoint);
HintedHandOffManager.deleteHintsForEndPoint(endpoint);
tokenMetadata_.removeBootstrapToken(token);
calculatePendingRanges();
```
```java
// AFTER — all paths delegate to excise()
private void excise(Token token, InetAddress endpoint) {
    Gossiper.instance.removeEndpoint(endpoint);
    tokenMetadata_.removeEndpoint(endpoint);
    HintedHandOffManager.deleteHintsForEndPoint(endpoint);
    tokenMetadata_.removeBootstrapToken(token);
    calculatePendingRanges();
    if (!isClientMode) SystemTable.removeToken(token);
}
```
