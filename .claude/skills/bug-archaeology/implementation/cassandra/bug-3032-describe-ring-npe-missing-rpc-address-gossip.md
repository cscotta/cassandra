# Bug 3032: NPE in describe_ring when endpoint has no RPC_ADDRESS gossip state

**Commit**: 428eaec338 · **Ticket**: CASSANDRA-3032

## Detection Rule
When reading an `ApplicationState` from gossip for an endpoint in a mixed-version cluster, check that both the endpoint state and the specific application state are non-null before accessing `.value`.

## Code Smell
`Gossiper.instance.getEndpointStateForEndpoint(endpoint).getApplicationState(ApplicationState.RPC_ADDRESS).value` without null checks on the `ApplicationState` result will NPE for old-version nodes that don't gossip an `RPC_ADDRESS` state entry.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: gossip-state-null-dereference-mixed-cluster
- severity: crash
- subsystem: gossip

## Root Cause
`StorageService.getRangeToRpcaddressMap()` read `RPC_ADDRESS` from gossip state for each non-local endpoint. In a mixed-version cluster, older nodes do not gossip `RPC_ADDRESS`, so `getApplicationState(RPC_ADDRESS)` returns null. The code dereferenced `.value` unconditionally, causing NPE. The fix adds an `else if` branch that falls back to `endpoint.getHostAddress()` when the gossip state is missing.

## Example
```java
// BEFORE
rpcaddrs.add(Gossiper.instance.getEndpointStateForEndpoint(endpoint)
    .getApplicationState(ApplicationState.RPC_ADDRESS).value);
```
```java
// AFTER
else if (Gossiper.instance.getEndpointStateForEndpoint(endpoint)
        .getApplicationState(ApplicationState.RPC_ADDRESS) == null)
    rpcaddrs.add(endpoint.getHostAddress());
else
    rpcaddrs.add(Gossiper.instance.getEndpointStateForEndpoint(endpoint)
        .getApplicationState(ApplicationState.RPC_ADDRESS).value);
```
