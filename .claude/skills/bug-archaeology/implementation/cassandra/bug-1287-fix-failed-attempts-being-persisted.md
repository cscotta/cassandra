# Bug 1287: Bootstrap/Replace Gossip State Written to `system.peers` Before Node Joins Ring

**Commit**: cba1b68dbe · **Ticket**: CASSANDRA-9180
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: State not cleaned up

---

## Root Cause

In `StorageService.onChange()`, gossip state changes (DC, rack, release version, schema, host ID) were written unconditionally to `system.peers` for any non-local, non-dead endpoint. A node that attempted to bootstrap or replace another node but ultimately failed would still emit gossip state changes during its attempt, and those changes would be persisted to `system.peers`. After the failed attempt the node was gone, but stale peer rows remained, polluting the peer table with endpoints that were never successfully part of the ring. The fix wraps the `system.peers` writes inside a `getTokenMetadata().isMember(endpoint)` guard, ensuring that only fully-joined members have their state persisted.

### Before / After

```java
// BEFORE — peer info written for any live endpoint regardless of ring membership
switch (state)
{
    case RELEASE_VERSION:
        SystemKeyspace.updatePeerInfo(endpoint, "release_version", value.value);
        break;
    // ...
}
```

```java
// AFTER — writes only happen when the endpoint has joined the ring
if (getTokenMetadata().isMember(endpoint))
{
    switch (state)
    {
        case RELEASE_VERSION:
            SystemKeyspace.updatePeerInfo(endpoint, "release_version", value.value);
            break;
        // ...
    }
}
```

---

## How to Spot

Whenever a gossip state-change handler persists data to a system table, ask: "has this endpoint completed whatever lifecycle step grants it permanent membership?" Writing metadata on any gossip event without a membership guard allows transient or failed nodes (bootstrapping, replacing, decommissioning) to create or update durable records that outlive the node's actual participation in the ring. The membership check should precede any `SystemKeyspace.updatePeerInfo` call.

---

## Test

No test added.
