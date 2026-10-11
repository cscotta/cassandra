# Bug 1290: Failed bootstrap/replace attempts persisted in system.peers (2.0 branch)

**Commit**: ac46747f84 · **Ticket**: CASSANDRA-9180
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Logic error in condition

---

## Root Cause

On the 2.0.x branch, the same `updateTopologyForNewClient` handler in `StorageService` unconditionally wrote gossip state into `system.peers` for any endpoint that sent a gossip application state, regardless of whether it had successfully joined the ring. A node that started bootstrapping (and thus began gossiping DC, rack, host ID, etc.) but then failed would leave a permanent ghost row in `system.peers` on all other nodes, since there was no subsequent `LEFT` event to trigger cleanup.

The fix is identical to the 2.1 branch fix: wrapping all `updatePeerInfo` calls inside an `if (getTokenMetadata().isMember(endpoint))` guard so that persistent peer metadata is only written for fully joined nodes.

### Before / After

```java
// BEFORE
switch (state)
{
    case RELEASE_VERSION:
        SystemKeyspace.updatePeerInfo(endpoint, "release_version", quote(value.value));
        break;
    // ... all cases written for any gossiping endpoint
}
```

```java
// AFTER
if (getTokenMetadata().isMember(endpoint))
{
    switch (state)
    {
        case RELEASE_VERSION:
            SystemKeyspace.updatePeerInfo(endpoint, "release_version", quote(value.value));
            break;
        // ... only persisted after successful ring join
    }
}
```

---

## How to Spot

Any code that responds to distributed lifecycle events (gossip, heartbeat, join notifications) by writing to persistent storage should always check whether the event represents a committed final state. Writing on the first partial signal—rather than on a confirmed terminal state—leaves orphaned records when operations fail partway through. The pattern to check is: is there a corresponding delete/cleanup path that fires reliably for every write path, including failure cases?

---

## Test

No test added.
