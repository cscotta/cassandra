# Bug 1288: Failed bootstrap/replace attempts persisted in system.peers (2.1 branch)

**Commit**: dcde8cac0d · **Ticket**: CASSANDRA-9180
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Logic error in condition

---

## Root Cause

When a node receives a gossip state change for a remote endpoint, `updateTopologyForNewClient` unconditionally wrote that endpoint's metadata into `system.peers` regardless of whether the endpoint had actually joined the ring. If a bootstrap or replace attempt failed mid-way, the node had already gossipped some state (DC, rack, host ID, etc.) that caused other nodes to persist a stale entry for it. That ghost row stayed in `system.peers` even after the failed node was gone, because the cleanup path only fires on a proper `LEFT` gossip transition.

The fix guards every `updatePeerInfo` write with `getTokenMetadata().isMember(endpoint)`, ensuring only fully-joined nodes are written to the peers table.

### Before / After

```java
// BEFORE
switch (state)
{
    case RELEASE_VERSION:
        SystemKeyspace.updatePeerInfo(endpoint, "release_version", value.value);
        break;
    // ... other cases written unconditionally
}
```

```java
// AFTER
if (getTokenMetadata().isMember(endpoint))
{
    switch (state)
    {
        case RELEASE_VERSION:
            SystemKeyspace.updatePeerInfo(endpoint, "release_version", value.value);
            break;
        // ... only written once endpoint is a ring member
    }
}
```

---

## How to Spot

Whenever code writes persistent state (especially system table rows) in response to partial or in-progress distributed events, reviewers should ask "what happens if this operation never completes?" State written eagerly on the first gossip message will survive if the corresponding cleanup event (e.g., `LEFT`, `REMOVED`) never arrives. The fix pattern is to gate writes on a committed membership predicate rather than on mere gossip activity.

---

## Test

No test added.
