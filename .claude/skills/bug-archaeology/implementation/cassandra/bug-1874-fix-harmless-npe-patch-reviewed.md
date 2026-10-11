# Bug 1874: Fix Harmless NPE in MigrationManager / HintedHandOffManager

**Commit**: 59bf44dd94 · **Ticket**: CASSANDRA-5185
**Subsystem**: src/java/org/apache/cassandra/db/HintedHandOffManager.java, src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: Missing null / bounds check

---

## Root Cause

Both `HintedHandOffManager.waitForSchemaAgreement` and `MigrationManager` accessed gossip endpoint state without guarding against the endpoint having been removed from the gossiper between the check and the access. `getEndpointStateForEndpoint` returns `null` when the endpoint is no longer known, so calling `.getApplicationState(...)` on the result caused a `NullPointerException`. The fix adds `!= null` guards inside the wait loops and throws a descriptive `TimeoutException` if the endpoint vanishes instead of crashing.

### Before / After

```java
// BEFORE – HintedHandOffManager: NPE if endpoint disappears mid-loop
while (gossiper.getEndpointStateForEndpoint(endpoint)
        .getApplicationState(ApplicationState.SCHEMA) == null)
{ ... }

// BEFORE – MigrationManager: NPE if endpoint removed between scheduling and execution
VersionedValue value = Gossiper.instance
        .getEndpointStateForEndpoint(endpoint)
        .getApplicationState(ApplicationState.SCHEMA);
```

```java
// AFTER – HintedHandOffManager: null-safe loop + explicit vanished-node handling
while (gossiper.getEndpointStateForEndpoint(endpoint) != null &&
        gossiper.getEndpointStateForEndpoint(endpoint)
                .getApplicationState(ApplicationState.SCHEMA) == null)
{ ... }
if (gossiper.getEndpointStateForEndpoint(endpoint) == null)
    throw new TimeoutException("Node " + endpoint + " vanished while waiting for agreement");

// AFTER – MigrationManager: early-return on missing endpoint
EndpointState epState = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
if (epState == null)
    return;
VersionedValue value = epState.getApplicationState(ApplicationState.SCHEMA);
```

---

## How to Spot

Any code that reads from a gossip map in a loop or in a scheduled task must treat the result of `getEndpointStateForEndpoint` as potentially null; nodes can leave gossip asynchronously. Look for chained calls on the gossip result without an intervening null check.

---

## Test

No test added.
