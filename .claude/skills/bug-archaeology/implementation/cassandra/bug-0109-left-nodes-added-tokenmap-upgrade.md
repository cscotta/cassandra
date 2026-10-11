# Bug 0109: LEFT Nodes Added to tokenMap and Placements During Gossip-to-TCM Upgrade

**Commit**: 417bb21d2e · **Ticket**: CASSANDRA-20344
**Subsystem**: tcm (Transactional Cluster Metadata), compatibility
**Category**: State not cleaned up

---

## Root Cause

During a rolling upgrade from gossip-based cluster management to TCM, `GossipHelper.fromEndpointStates` converted every gossip endpoint (including nodes in `NodeState.LEFT`) into `Directory` entries and then unconditionally called `tokenMap.assignTokens` for all of them. LEFT nodes should carry no token ownership, so including them in the token map caused incorrect placement calculations. Additionally, `Directory.withNonUpgradedNode` called `withRackAndDC` for LEFT nodes, adding them to rack/DC membership structures. `ClusterMetadataService` also iterated `allAddresses()` (which includes LEFT nodes) when selecting CMS candidates. The fix skips `tokenMap.assignTokens` for LEFT nodes in `GossipHelper`, skips `withRackAndDC` in `Directory.withNonUpgradedNode` for LEFT nodes, and changes the CMS candidate selection to use `allJoinedEndpoints()`.

### Before / After

```java
// BEFORE — unconditionally assigns tokens for LEFT nodes too
directory = directory.withNonUpgradedNode(..., toNodeState(endpoint, epState), ...);
NodeId nodeId = directory.peerId(endpoint);
tokenMap = tokenMap.assignTokens(nodeId, getTokensIn(partitioner, epState));
```

```java
// AFTER — skips token assignment for LEFT nodes
NodeState nodeState = toNodeState(endpoint, epState);
directory = directory.withNonUpgradedNode(..., nodeState, ...);
if (nodeState != NodeState.LEFT)
{
    NodeId nodeId = directory.peerId(endpoint);
    tokenMap = tokenMap.assignTokens(nodeId, getTokensIn(partitioner, epState));
}
```

---

## How to Spot

Any loop that iterates "all known nodes" and applies an operation that is only valid for live/joined nodes must first filter by node state. After an assassinate or decommission, LEFT nodes remain in gossip state history; code that does not explicitly exclude them will incorporate stale token and placement data into the new cluster topology, producing incorrect reads/writes. Review every `directory.allAddresses()` / `allEndpoints()` call site and confirm that LEFT/LEAVING/REMOVED states are excluded where appropriate.

---

## Test

`ClusterMetadataUpgradeAssassinateTest.simpleUpgradeTest` upgrades a 3-node cluster where node 3 was assassinated before the upgrade, then verifies that the assassinated node's address does not appear in any placement after `cms initialize`.
