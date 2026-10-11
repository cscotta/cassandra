# Bug 0881: Paxos stalls indefinitely when Most Recent Commit has expired (2.1 backport)

**Commit**: 017ec3e99e · **Ticket**: CASSANDRA-12043
**Subsystem**: Paxos / LWT
**Category**: Logic error in condition

---

## Root Cause
This is the 2.1-branch backport of the same fix as bug-0880. The root cause is identical: `PrepareCallback.replicasMissingMostRecentCommit()` had no age check, so a Most Recent Commit (MRC) that had expired from some nodes' system.paxos rows caused an infinite repair-and-retry loop in `StorageProxy.beginAndRepairPaxos()`. Each replica also used its own wall-clock time as `now` when reading Paxos state, making TTL-expiry decisions inconsistent across the ring. The fix pins `now` to the ballot's embedded timestamp (via `UUIDGen.unixTimestamp(ballot)`) so all replicas agree on expiry, and bails out of the MRC-repair loop when the commit is older than the Paxos TTL.

### Before / After
```java
// BEFORE (2.1) — replica uses its own System.currentTimeMillis() as now
PaxosState state = SystemKeyspace.loadPaxosState(toPrepare.key, toPrepare.update.metadata());
// ...
public Iterable<InetAddress> replicasMissingMostRecentCommit() {
    return Iterables.filter(commitsByReplica.keySet(), ...);
}

// AFTER — now derived from ballot; expired MRC skipped
long now = UUIDGen.unixTimestamp(toPrepare.ballot);
PaxosState state = SystemKeyspace.loadPaxosState(toPrepare.key, toPrepare.update.metadata(), now);
// ...
public Iterable<InetAddress> replicasMissingMostRecentCommit(CFMetaData metadata, long now) {
    long paxosTtlMicros = SystemKeyspace.paxosTtl(metadata) * 1000 * 1000L;
    if (UUIDGen.microsTimestamp(mostRecentCommit.ballot) + paxosTtlMicros < now)
        return Collections.emptySet();
    ...
}
```

---

## How to Spot
Identical pattern to 0880 — look for Paxos paths that read state with a locally-derived clock and for missing TTL age-checks in the MRC-repair loop. The 2.1 API uses `long` microseconds while the 3.x version uses `int` seconds, so check the unit carefully when reviewing the comparison.

---

## Test
No new test was added in this commit.
