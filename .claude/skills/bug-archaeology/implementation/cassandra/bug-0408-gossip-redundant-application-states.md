# Bug 0408: Gossip Processes Redundant Legacy Application States, Triggering Spurious State Changes

**Commit**: 64f54f9fb0 · **Ticket**: CASSANDRA-16381
**Subsystem**: src/java/org/apache/cassandra/gms/Gossiper.java
**Category**: State not cleaned up

---

## Root Cause

When a 4.0 endpoint sends gossip state that includes both the newer `STATUS_WITH_PORT` / `INTERNAL_ADDRESS_AND_PORT` / `NATIVE_ADDRESS_AND_PORT` states and the legacy `STATUS` / `INTERNAL_IP` / `RPC_ADDRESS` equivalents, the receiving node processes all of them. The deduplication filter was placed inside `applyNewStates`, which runs only after the endpoint's local copy is checked for versioning—meaning the legacy state was applied first and could trigger `onStatusChange` listeners unnecessarily or cause address-and-port state to be overwritten by an incomplete legacy value.

### Before / After

```java
// BEFORE — filtering happened inside applyNewStates (after local-state comparison)
EndpointState remoteState = entry.getValue();
// ... later, inside applyNewStates:
switch (entry.getKey()) {
    case INTERNAL_IP:
        if (remoteState.getApplicationState(INTERNAL_ADDRESS_AND_PORT) != null) return false;
        ...
}
```

```java
// AFTER — filtering applied to the whole EndpointState before any comparison
EndpointState remoteState = removeRedundantApplicationStates(entry.getValue());

private static EndpointState removeRedundantApplicationStates(EndpointState remoteState) {
    // strips STATUS, INTERNAL_IP, RPC_ADDRESS when their 4.0 equivalents are present
    ...
}
```

---

## How to Spot

When a filter that guards against a superseded value is applied only during the per-field comparison step rather than at the outer ingestion boundary, the superseded entries still participate in coarser-grained logic (e.g., deciding whether the endpoint state has changed at all). Always strip redundant or deprecated keys at the point where the full payload is received, before any downstream logic inspects individual fields.

---

## Test

No dedicated new test added; existing gossip tests cover normal state propagation.
