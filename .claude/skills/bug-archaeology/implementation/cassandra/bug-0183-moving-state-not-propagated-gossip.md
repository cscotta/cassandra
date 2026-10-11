# Bug 0183: MOVING State Never Propagated to Gossip in LegacyStateListener

**Commit**: 5f0359b229 · **Ticket**: CASSANDRA-19123
**Subsystem**: tcm/listeners/LegacyStateListener
**Category**: State not cleaned up

---

## Root Cause

`LegacyStateListener.onChange` detected when a node entered the `MOVING` state and logged a debug message, but omitted the two calls that actually propagate that state to legacy Gossip and the peers table: `Gossiper.instance.mergeNodeToGossip` and `PeersTable.updateLegacyPeerTable`. Every other state transition (bootstrap, normal, left) made these calls, but the `MOVING` branch was incomplete. Nodes waiting on `assertGossipStatus` for a moving peer would loop indefinitely because the gossip state was never updated.

### Before / After

```java
// BEFORE – MOVING branch only logs
if (NodeState.isMoving(next.directory.peerState(change)))
{
    logger.debug("Node {} state MOVING, tokens {}", ...);
    // gossip and peers table never updated
}
```

```java
// AFTER – state is now propagated
if (NodeState.isMoving(next.directory.peerState(change)))
{
    logger.debug("Node {} state MOVING, tokens {}", ...);
    Gossiper.instance.mergeNodeToGossip(change, next);
    PeersTable.updateLegacyPeerTable(change, prev, next);
}
```

---

## How to Spot

When a state-machine listener has multiple branches (one per state), each branch should invoke the same set of side-effect calls. A branch that only logs while all other branches also call `mergeNodeToGossip` / `updateLegacyPeerTable` is a dead giveaway that the propagation step was accidentally omitted. During review, compare the bodies of each case in a multi-branch listener to confirm they share the same structural pattern.

---

## Test

The fix resolves `ConsistentMoveTest`, which asserted gossip status for a moving node and hung indefinitely before the fix; no new unit test was added.
