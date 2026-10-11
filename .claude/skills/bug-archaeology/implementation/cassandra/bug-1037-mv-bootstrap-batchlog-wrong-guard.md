# Bug 1037: MV Mutations Written to Batchlog Too Early During Bootstrap

**Commit**: 02a53fa2b7 · **Ticket**: CASSANDRA-10621
**Subsystem**: Materialized Views / Bootstrap
**Category**: Logic error in condition

---

## Root Cause
`StorageProxy.performWrite` used `!Gossiper.instance.isEnabled()` to decide whether to route MV mutations through the batchlog during node startup. However, Gossiper is enabled very early in the startup sequence — well before the node has joined the ring or finished bootstrapping — so this condition evaluated to `false` (Gossiper is enabled) even while the node was in JOINING or MOVING state. As a result, MV mutations were sent directly to paired replicas whose token assignments were not yet stable, silently bypassing the batchlog safety net that is meant to protect against data loss during ring transitions.

The fix replaces the Gossiper-based guard with an explicit check of `StorageService` operation mode: mutations are sent via the batchlog whenever the local node is STARTING, JOINING, or MOVING. Two new convenience methods — `isJoining()` and `isMoving()` — were added to `StorageService` to complete the check.

### Before / After
```java
// BEFORE – Gossiper is enabled long before the ring is stable; condition fires too rarely
if (!Gossiper.instance.isEnabled())
{
    BatchlogManager.store(Batch.createLocal(..., mutations), ...);
}

// AFTER – check the node's actual operational state
if (StorageService.instance.isStarting()
    || StorageService.instance.isJoining()
    || StorageService.instance.isMoving())
{
    BatchlogManager.store(Batch.createLocal(..., mutations), ...);
}
```

---

## How to Spot
Any conditional that uses Gossiper liveness as a proxy for "the node has not yet joined the ring" is fragile, because Gossiper starts before ring membership is established. In review, prefer explicit `StorageService` mode checks (`isJoining`, `isMoving`, `isStarting`) whenever the intent is to guard against partially-bootstrapped state. Also watch for places that send mutations to remote replicas without verifying ring stability.

---

## Test
No test added.
