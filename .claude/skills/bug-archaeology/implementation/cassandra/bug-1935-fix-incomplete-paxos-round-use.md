# Bug 1935: Re-propose of Incomplete Paxos Round Uses Stale Ballot

**Commit**: 04cd81cdbb · **Ticket**: CASSANDRA-5985
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

When a coordinator discovered an in-progress Paxos round from a previous proposer, it attempted to finish that round by calling `proposePaxos(inProgress, ...)` with the original (stale) ballot from the in-progress commit. This meant the re-proposal carried an old ballot that any replica which had already promised a higher ballot would reject — yet the code would still call `commitPaxos` even after a failed proposal, potentially committing a value without quorum acceptance. The fix creates a fresh `Commit` object (`refreshedInProgress`) using the new ballot and the original update, so the re-proposal is made with the current ballot. A failed proposal now correctly skips the commit and backs off with a random sleep to give the competing proposer time to complete.

### Before / After

```java
// BEFORE
if (proposePaxos(inProgress, liveEndpoints, requiredParticipants))
    commitPaxos(inProgress, ConsistencyLevel.QUORUM);
continue;
```

```java
// AFTER
Commit refreshedInProgress = Commit.newProposal(inProgress.key, ballot, inProgress.update);
if (proposePaxos(refreshedInProgress, liveEndpoints, requiredParticipants))
{
    commitPaxos(refreshedInProgress, ConsistencyLevel.QUORUM);
}
else
{
    Tracing.trace("Some replicas have already promised a higher ballot than ours; aborting");
    Uninterruptibles.sleepUninterruptibly(FBUtilities.threadLocalRandom().nextInt(100), TimeUnit.MILLISECONDS);
}
continue;
```

---

## How to Spot

In multi-phase commit protocols, when completing an in-progress round discovered during a prepare phase, always rebind the stale proposal to the current ballot before re-proposing. Using a stale ballot in the propose phase violates the protocol invariant and can result in a commit being sent even when quorum was not achieved.

---

## Test

No test added.
