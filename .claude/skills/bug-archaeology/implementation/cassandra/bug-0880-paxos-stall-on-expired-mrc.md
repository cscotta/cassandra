# Bug 0880: Paxos stalls indefinitely when Most Recent Commit has expired

**Commit**: 53f8f09575 · **Ticket**: CASSANDRA-12043
**Subsystem**: Paxos / LWT
**Category**: Logic error in condition

---

## Root Cause
When a Paxos Most Recent Commit (MRC) was old enough to have expired from some nodes' system.paxos tables (due to the TTL), replicas diverged on whether the MRC existed at all. StorageProxy detected the missing MRC and issued a repair-then-retry loop, but the retry always reproduced the same divergence because the expired replica had a tombstone hiding any re-written MRC. Additionally, different nodes used their own local wall-clock as `nowInSec` when reading Paxos state, so the TTL expiry decision was inconsistent across the cluster. The fix does two things: it derives `nowInSec` deterministically from the ballot UUID timestamp so all replicas agree on expiry, and it teaches `PrepareCallback.replicasMissingMostRecentCommit()` to return an empty set (skipping the repair loop) for an MRC that is older than the Paxos TTL.

### Before / After
```java
// BEFORE — each replica uses its own wall clock
PaxosState state = SystemKeyspace.loadPaxosState(toPrepare.update.partitionKey(), toPrepare.update.metadata());
// ...
public Iterable<InetAddress> replicasMissingMostRecentCommit() {
    // always scans for missing MRC, even if MRC is too old
    return Iterables.filter(commitsByReplica.keySet(), ...);
}

// AFTER — nowInSec derived from the ballot itself; expired MRC skipped
int nowInSec = UUIDGen.unixTimestampInSec(toPrepare.ballot);
PaxosState state = SystemKeyspace.loadPaxosState(
        toPrepare.update.partitionKey(), toPrepare.update.metadata(), nowInSec);
// ...
public Iterable<InetAddress> replicasMissingMostRecentCommit(CFMetaData metadata, int nowInSec) {
    long paxosTtlSec = SystemKeyspace.paxosTtlSec(metadata);
    if (UUIDGen.unixTimestampInSec(mostRecentCommit.ballot) + paxosTtlSec < nowInSec)
        return Collections.emptySet();   // MRC too old — skip repair loop
    ...
}
```

---

## How to Spot
Look for callers of `replicasMissingMostRecentCommit()` / `loadPaxosState()` that supply a locally-derived timestamp (e.g. `FBUtilities.nowInSeconds()` or `System.currentTimeMillis()`) rather than one derived from the ballot being processed. Any place where expiry decisions can differ between replicas due to independent clocks is a candidate for this class of bug. Also watch for unbounded retry loops in Paxos prepare/propose paths that have no age-based circuit-breaker.

---

## Test
No new test was added in this commit (the fix is validated by the absence of an infinite-loop stall in production).
