# Bug 0429: SERIAL Read and Non-Applying CAS Break Paxos Linearizability

**Commit**: 2d0b168047 · **Ticket**: CASSANDRA-12126
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java, service/paxos/
**Category**: Logic error in condition

---

## Root Cause

A SERIAL read (and a non-applying CAS) calls `beginAndRepairPaxos` to replay any in-progress commit, but it contacts only a quorum of nodes. A minority of replicas may still hold an older unfinished in-progress proposal that was never replayed. Because the SERIAL read does not itself commit anything, that old proposal can later be resurrected by a different coordinator contacting those minority nodes, violating linearizability: an operation believed "committed" at quorum can be overwritten by a proposal that logically precedes it.

The fix makes every SERIAL read and every non-applying CAS commit an empty `PartitionUpdate` before returning. An empty commit advances the most-recently-committed (MRC) ballot on a quorum, ensuring that any in-progress proposal with an older ballot will be discarded by future coordinators. An optimization skips actually storing or replaying empty commits (they carry no data), so the performance impact is limited to a single additional Paxos round; the feature flag `cassandra.unsafe.disable-serial-reads-linearizability` allows opting out for clusters upgrading from pre-fix versions.

### Before / After

```java
// BEFORE: serial read simply repaired paxos then read
final Pair<UUID, Integer> pair = beginAndRepairPaxos(start, key, metadata,
        liveEndpoints, requiredParticipants,
        consistencyLevel, consistencyForCommitOrFetch, false, state);
result = fetchRows(group.commands, consistencyForCommitOrFetch);
```

```java
// AFTER: serial read proposes an empty update first
Supplier<Pair<PartitionUpdate, RowIterator>> updateProposer =
    disableSerialReadLinearizability
    ? () -> null
    : () -> Pair.create(PartitionUpdate.emptyUpdate(metadata, key), null);
doPaxos(metadata, key, consistencyLevel,
        consistencyForReplayCommitOrFetch, ConsistencyLevel.ANY,
        state, start, casReadMetrics, updateProposer);
result = fetchRows(group.commands, consistencyForReplayCommitOrFetch);
```

---

## How to Spot

In any multi-phase consensus protocol, a "read-only" or "no-op" path that skips the commit phase must still advance a durable marker (ballot, epoch, or sequence number) that prevents older in-flight proposals from being resurrected. Reviewers should ask: after this code path completes, can a prior incomplete proposal still be completed by a different participant? If yes, linearizability may be broken.

---

## Test

`CASTest` (in the distributed test suite) covers linearizability scenarios for SERIAL reads and non-applying CAS operations, verifying that no stale in-progress proposal can be replayed after a serial read has succeeded.
