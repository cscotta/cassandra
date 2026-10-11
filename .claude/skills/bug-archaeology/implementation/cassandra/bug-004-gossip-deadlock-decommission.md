# Bug 004: Remove potential gossip deadlock during decommission

**Commit**: 59d57e3eb7 · **Ticket**: CASSANDRA-21143
**Subsystem**: Gossip / StorageService
**Category**: Deadlock

---

## Root Cause

During decommission, when broadcasting the `LEFT` status to other nodes, the code called `Gossiper.instance.addLocalApplicationState(...)` while already holding the gossip lock. `addLocalApplicationState` itself acquires the same gossip lock internally, creating a potential deadlock. The subsequent re-fetch of the updated value was also unnecessary because the version bump from `addLocalApplicationState` only needs to be consistent for the local state, not when constructing an outgoing `GossipDigestAck2` message for a remote peer.

### Before / After

```java
// BEFORE — calls addLocalApplicationState (re-acquires gossip lock) inside gossip-locked path
VersionedValue value = StorageService.instance.valueFactory.left(tokens, computeExpireTime());

if (left.equals(getBroadcastAddressAndPort()))
{
    Gossiper.instance.addLocalApplicationState(ApplicationState.STATUS_WITH_PORT, value);
    value = Gossiper.instance.endpointStateMap.get(getBroadcastAddressAndPort())
                                              .getApplicationState(ApplicationState.STATUS_WITH_PORT);
}

toSend.addApplicationState(ApplicationState.STATUS_WITH_PORT, value);

// AFTER — uses computed value directly; no nested lock acquisition
VersionedValue value = StorageService.instance.valueFactory.left(tokens, computeExpireTime());
toSend.addApplicationState(ApplicationState.STATUS_WITH_PORT, value);
```

---

## How to Spot

When reviewing code that operates inside a lock (especially gossip's `synchronized` blocks), look for any call that delegates to a public API method on the same singleton — these methods often acquire the same lock independently. The pattern of "update shared state then immediately re-read it" inside a locked section is a strong signal: if the update method also locks, there is a latent deadlock waiting for a concurrent gossip round to trigger it.

---

## Test

No test added.
