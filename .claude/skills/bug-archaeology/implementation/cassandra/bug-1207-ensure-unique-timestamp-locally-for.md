# Bug 1207: Paxos Ballot Clock Can Produce Duplicate UUIDs Under Contention

**Commit**: 0bdf92350b · **Ticket**: CASSANDRA-9649
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Race condition / missing synchronization

---

## Root Cause

The Paxos prepare loop generated a ballot by calling `UUIDGen.getTimeUUID(ballotMillis)` where `ballotMillis` was computed as `max(System.currentTimeMillis(), 1 + lastBallotMillis)`. `UUIDGen.getTimeUUID(long)` produces a UUID whose sub-millisecond component is always zero, so two concurrent Paxos rounds starting in the same millisecond would generate identical ballots, causing one to silently lose the prepare phase or create an incorrect commit. The fix replaces the hand-rolled timestamp calculation with `UUIDGen.getTimeUUID()`, which uses the library's internal monotonic clock that guarantees uniqueness within the same process even across calls in the same millisecond.

### Before / After

```java
// BEFORE – two calls in the same ms yield identical UUIDs
long ballotMillis = summary == null
    ? System.currentTimeMillis()
    : Math.max(System.currentTimeMillis(), 1 + UUIDGen.unixTimestamp(summary.mostRecentInProgressCommit.ballot));
UUID ballot = UUIDGen.getTimeUUID(ballotMillis);
```

```java
// AFTER – monotonic generator guarantees uniqueness
UUID ballot = UUIDGen.getTimeUUID();
```

---

## How to Spot

Any Paxos or leader-election implementation that generates a ballot/epoch using wall-clock time alone is vulnerable to duplicate IDs when two threads or nodes execute within the same clock tick. Reviewers should confirm that ballot generation goes through a monotonically incrementing generator (with sub-millisecond disambiguation) rather than wrapping `System.currentTimeMillis()` directly, and that the generator's uniqueness guarantee covers the same-process concurrent case.

---

## Test

No test added.
