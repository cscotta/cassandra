# Bug 1576: Fix Inconsistent Use of consistencyForCommit Allowing Consistency Bypass

**Commit**: 2b973b9ed1 · **Ticket**: CASSANDRA-7345
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Wrong constant / default value

---

## Root Cause

`beginAndRepairPaxos` was not accepting `consistencyForCommit` as a parameter. When it needed to commit an in-progress Paxos round during the repair phase, it hardcoded `ConsistencyLevel.QUORUM` instead of using the caller-supplied commit consistency. This meant that a `LOCAL_SERIAL` read or CAS operation, which should commit via `LOCAL_QUORUM`, was silently escalated to a full cross-datacenter `QUORUM` commit.

### Before / After

```java
// BEFORE – hardcoded QUORUM ignores local serial intent
private static UUID beginAndRepairPaxos(..., ConsistencyLevel consistencyForPaxos)
{
    ...
    commitPaxos(refreshedInProgress, ConsistencyLevel.QUORUM);
}
```

```java
// AFTER – passes through the correct commit consistency
private static UUID beginAndRepairPaxos(..., ConsistencyLevel consistencyForPaxos, ConsistencyLevel consistencyForCommit)
{
    ...
    commitPaxos(refreshedInProgress, consistencyForCommit);
}
// caller computes it once and reuses for fetchRows too
final ConsistencyLevel consistencyForCommitOrFetch =
    consistency_level == ConsistencyLevel.LOCAL_SERIAL
        ? ConsistencyLevel.LOCAL_QUORUM : ConsistencyLevel.QUORUM;
```

---

## How to Spot

When a method that enforces a consistency policy delegates to a helper that commits or acknowledges writes, verify that the helper receives — rather than hardcodes — the consistency level. Any hardcoded `ConsistencyLevel.QUORUM` inside a path that may be entered via `LOCAL_SERIAL` is a candidate for this class of bug.

---

## Test

No test added.
