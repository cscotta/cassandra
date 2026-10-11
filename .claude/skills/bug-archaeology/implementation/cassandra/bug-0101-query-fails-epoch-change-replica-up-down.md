# Bug 0101: Query Failure When Epoch Changes and Replica Goes Up/Down

**Commit**: b31d15b9b5 · **Ticket**: CASSANDRA-20489
**Subsystem**: locator (ReplicaLayout / ReplicaPlan / ReplicaPlans)
**Category**: State not cleaned up

---

## Root Cause

When a `ReplicaPlan` is recomputed after a `ClusterMetadata` epoch change (because a replica went up or down mid-query), the `stillAppliesTo` check compared the old plan's `readCandidates()` — which already excluded nodes the failure detector considered down at plan-construction time — against the new plan's `readCandidates()`. If the set of live nodes changed between the two epochs, the sets differed even when the topology itself was unchanged, causing `stillAppliesTo` to return `false` and the query to be aborted with an error.

The fix separates the "all replicas regardless of liveness" set (`liveAndDown`) from the "live replicas only" contact set. `forTokenReadSorted` / `forRangeReadSorted` now return all natural replicas without filtering by the failure detector; the failure-detector filter is applied separately to compute `contacts`. `stillAppliesTo` compares `liveAndDown()` between old and new plans, which only changes when actual topology changes, not when the failure detector flips a node.

### Before / After

```java
// BEFORE — reads were filtered by FailureDetector at layout construction time
static ReplicaLayout.ForTokenRead forTokenReadLiveSorted(...) {
    ...
    replicas = replicas.filter(FailureDetector.isReplicaAlive);  // bakes liveness in
    return new ReplicaLayout.ForTokenRead(replicationStrategy, replicas);
}
// stillAppliesTo compared readCandidates() (live-only) between old and new plan
```

```java
// AFTER — layout holds all replicas; liveness filtered separately for contacts
static ReplicaLayout.ForTokenRead forTokenReadSorted(...) {
    ...
    // no failure-detector filter here
    return new ReplicaLayout.ForTokenRead(replicationStrategy, replicas);
}
// call site:
ReplicaLayout.ForTokenRead forTokenReadLive = forTokenReadLiveAndDown.filter(FailureDetector.isReplicaAlive);
EndpointsForToken candidates = candidatesForRead(..., forTokenReadLive.all());
// Plan stores liveAndDown separately; stillAppliesTo compares liveAndDown()
```

---

## How to Spot

Whenever a plan or context object is compared against a recomputed version to decide whether to abort or retry, the comparison must use data that is stable across the concern being checked (topology) and must not conflate it with transiently changing data (liveness). When the same field is used both to select contacts and to test plan stability, a topology-only test becomes a liveness test by accident. Separating "all topology replicas" from "currently-live contacts" is the correct design.

---

## Test

`FailureDetectorRecomputeTest.readTest` and `writeTest`: ByteBuddy intercepts `FailureDetector.isAlive` and `ReplicaPlan.stillAppliesTo` to simulate a replica toggling up/down on every call while 10 reads and writes execute; the test asserts no query fails.
