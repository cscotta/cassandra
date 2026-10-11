# Bug 2302: nodetool ring Displays Token Key Instead of Endpoint Address; Token Metadata Not Cloned

**Commit**: c22dd08218 · **Ticket**: CASSANDRA-4429 (CASSANDRA-3047)

## Detection Rule

When iterating a `BiMap<InetAddress, Token>` (or similar inverted map) to print per-endpoint information, verify that the loop variable (not the map entry key) is used in the output format call; and verify that live `TokenMetadata` is cloned before iteration to avoid ConcurrentModificationException.

## Code Smell

`outs.printf(format, entry.getKey(), ...)` where `entry.getKey()` is a `Token` (from the inverted map) but the format expects an `InetAddress`; separately, `tokenMetadata.getTopology()` is called on the live object rather than a snapshot, risking concurrent modification.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: inverted-map-key-used-as-value
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

`printRing` iterated `endpointsToTokens` (a `BiMap<InetAddress, Token>` keyed by endpoint) but called `outs.printf(format, entry.getKey(), ...)` — `entry.getKey()` is the endpoint address. Wait: looking more carefully — the inverted map is keyed by `InetAddress`, but the loop uses a separate `endpoint` variable derived from the entry. The bug is that `entry.getKey()` (token) was used in printf instead of `endpoint` (address). Additionally, `tokenMetadata` was accessed directly without cloning, risking CME during topology changes; the fix clones it via `cloneOnlyTokenMap()` before the operation.

## Example

```java
// BEFORE — uses entry.getKey() (a Token) where InetAddress is expected; live metadata
final BiMap<InetAddress, Token> endpointsToTokens =
    ImmutableBiMap.copyOf(tokenMetadata.getTokenToEndpointMapForReading()).inverse();
// ...
outs.printf(format, entry.getKey(), rack, status, state, load, owns, token);  // entry.getKey() = Token!
```

```java
// AFTER — uses endpoint variable; operates on a cloned snapshot
TokenMetadata metadata = tokenMetadata.cloneOnlyTokenMap();
final BiMap<InetAddress, Token> endpointsToTokens =
    ImmutableBiMap.copyOf(metadata.getTokenToEndpointMapForReading()).inverse();
// ...
outs.printf(format, endpoint, rack, status, state, load, owns, token);  // correct variable
```
