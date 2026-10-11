# Bug 1757: Fix Snapshot Repair Not Snapshotting Coordinator

**Commit**: 6dfca3d329 · **Ticket**: CASSANDRA-6713
**Subsystem**: src/java/org/apache/cassandra/service/AntiEntropyService.java
**Category**: Logic error in condition

---

## Root Cause

During sequential (snapshot) repair, `AntiEntropyService` built two sets: `endpoints` (remote replicas) and `allEndpoints` (remote replicas + the local coordinator). Snapshots were taken only on `endpoints`, so the coordinator node's data was never snapshotted before the Merkle-tree comparison began. This meant the repair could compare a live, mutating data set on the coordinator against stable snapshots on peers, producing incorrect or unstable repair results. The one-character fix passes `allEndpoints` to `makeSnapshots` instead of `endpoints`.

### Before / After

```java
// BEFORE
allEndpoints.add(FBUtilities.getBroadcastAddress());
if (isSequential)
    makeSnapshots(endpoints);   // coordinator excluded
```

```java
// AFTER
allEndpoints.add(FBUtilities.getBroadcastAddress());
if (isSequential)
    makeSnapshots(allEndpoints); // coordinator included
```

---

## How to Spot

When code builds an "all" set by augmenting a base set, subsequent operations should consistently use the "all" set rather than the base. Review every usage of the base set after the augmented set is constructed and verify the intent.

---

## Test

No test added.
