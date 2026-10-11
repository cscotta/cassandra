# Bug 0102: Mixed-Mode Paxos Hang Due to Missing MRC TTL Check

**Commit**: cf60eb8672 · **Ticket**: CASSANDRA-20514
**Subsystem**: service/paxos (mixed-mode upgrade, legacy Paxos)
**Category**: State not cleaned up

---

## Root Cause

CEP-14 (CASSANDRA-17094) changed how legacy Paxos state TTLs are calculated and removed the check from `PrepareCallback.replicasMissingMostRecentCommit` that ignored an MRC older than the Paxos TTL window (originally added by CASSANDRA-12043). That check was safe to remove for nodes running the new Paxos state purging logic, but it became a problem again during mixed-mode operation with 4.0 replicas.

When a 4.1+ coordinator runs a Paxos operation with two 4.0 replicas: replica A returns an MRC whose ballot is within the TTL window on that replica, but replica B has already TTL'd the same commit and converted the expired cells into tombstones. The 4.1 coordinator, lacking the TTL guard, sends the "missing" commit to replica B. Because tombstones win reconciliation over live cells, the write is a no-op, and replica B continues to report no MRC. The coordinator then re-prepares, finds the same discrepancy, and repeats indefinitely until the query times out.

The fix re-introduces the TTL guard specifically when `paxosStatePurging == legacy`, checking whether the MRC ballot is older than `legacyPaxosTtlSec(metadata)` and, if so, returning an empty set of "replicas missing MRC."

### Before / After

```java
// BEFORE — no TTL check; always returns replicas with a different ballot
public Iterable<InetAddressAndPort> replicasMissingMostRecentCommit() {
    return Iterables.filter(commitsByReplica.keySet(),
            inetAddress -> !commitsByReplica.get(inetAddress).ballot.equals(mostRecentCommit.ballot));
}
```

```java
// AFTER — skips MRC repair for ballots older than the legacy paxos TTL
public Iterable<InetAddressAndPort> replicasMissingMostRecentCommit(TableMetadata metadata) {
    if (DatabaseDescriptor.paxosStatePurging() == Config.PaxosStatePurging.legacy) {
        long paxosTtlSec = SystemKeyspace.legacyPaxosTtlSec(metadata);
        if (TimeUnit.MICROSECONDS.toSeconds(mostRecentCommit.ballot.unixMicros()) + paxosTtlSec
                < FBUtilities.nowInSeconds())
            return Collections.emptySet();
    }
    return Iterables.filter(commitsByReplica.keySet(),
            inetAddress -> !commitsByReplica.get(inetAddress).ballot.equals(mostRecentCommit.ballot));
}
```

---

## How to Spot

When removing a guard that was added to handle a specific edge case, verify that the removal is safe for every deployment mode, including mixed-mode upgrades where older nodes with different behaviour are still present. Any time a guard is removed with the rationale "the new code doesn't have this race," add an explicit comment or assertion documenting the assumption, and audit mixed-version scenarios separately.

---

## Test

`MixedModePaxosTTLTest.legacyExpiredStateTest`: sets up a 3-node cluster (1 upgraded to 4.1, 2 on 4.0), plants a tombstoned commit on one replica and a live commit on another with a ballot older than the GC-grace TTL, then verifies that a SERIAL read completes without timing out.
