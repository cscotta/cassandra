# Bug 0154: Peers V2 Table Corruption When Two Nodes Swap IP Addresses

**Commit**: 38512a469c · **Ticket**: CASSANDRA-19221
**Subsystem**: `tcm/Startup`, `tcm/listeners/LegacyStateListener`, `db/virtual/PeersTable`
**Category**: State not cleaned up

---

## Root Cause

When two nodes swapped their IP addresses, the `LegacyStateListener` tracked removals by `NodeId` set-difference (`prev.directory.peerIds() - next.directory.peerIds()`). After an IP swap, no `NodeId` is actually removed — only the address changes — so the old IP entries were never purged from `system.peers_v2` and `system.peers`. The stale rows persisted, causing both nodes to appear at their old addresses alongside the new ones. Additionally, `Startup` silently overwrote the local host-id whenever it differed from the directory value, allowing a node to silently steal another node's identity when it took over that node's IP address.

### Before / After

```java
// BEFORE — computes removed nodes by NodeId; address swaps produce empty set
Set<NodeId> removed = Sets.difference(prev.directory.peerIds(), next.directory.peerIds());
for (NodeId remove : removed) {
    GossipHelper.evictFromMembership(prev.directory.endpoint(remove));
    PeersTable.updateLegacyPeerTable(remove, prev, next);
}
```

```java
// AFTER — computes removed addresses directly; catches swaps
Set<InetAddressAndPort> removedAddr = Sets.difference(
    new HashSet<>(prev.directory.allAddresses()),
    new HashSet<>(next.directory.allAddresses()));
for (InetAddressAndPort remove : removedAddr) {
    GossipHelper.evictFromMembership(remove);
    PeersTable.removeFromSystemPeersTables(remove);
}
```

```java
// BEFORE — Startup silently accepted any NodeId mismatch
logger.info("NodeId is wrong, updating from {} to {}", currentHostId, nodeId.toUUID());
SystemKeyspace.setLocalHostId(nodeId.toUUID());
```

```java
// AFTER — throws if existing host-id is present but doesn't match directory
String error = String.format("NodeId does not match locally set one. Check for the IP address collision: ...");
throw new IllegalStateException(error);
```

---

## How to Spot

When tracking "what changed between two cluster metadata snapshots," using set-difference on one identifier (NodeId) to detect removals will miss topology mutations where the identifier is stable but a dependent attribute (IP address) changes. Review all snapshot-diff logic to ensure the diffed key is the attribute that actually drives the cleanup action.

---

## Test

`BounceResetHostIdTest.swapIpsDirectlyTest` starts a 3-node cluster, shuts down nodes 2 and 3, swaps their IP addresses, and asserts that startup throws `IllegalStateException` containing "NodeId does not match locally set one"; `swapIpsTest` performs a sequential (non-direct) swap and verifies `system.peers_v2` has correct host-id to IP mappings afterwards.
