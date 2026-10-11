# Bug 1929: Paxos Not Always Replaying When It Should

**Commit**: 50c9d77e1b · **Ticket**: CASSANDRA-6012
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java, src/java/org/apache/cassandra/service/paxos/PrepareCallback.java
**Category**: Logic error in condition

---

## Root Cause

`PrepareCallback` tracked a single `inProgressCommit` field that was updated to the highest-ballot in-progress commit seen across all prepare responses. Due to clock skew, a replica could return an in-progress commit with a very old ballot that contained an actual update, while a more recent ballot from another replica had an empty update. Because the tracking selected only by ballot ordering, the commit with the actual update could be discarded in favor of a more recent but empty one. The fix splits tracking into two fields: `mostRecentInProgressCommit` (highest ballot regardless of content, used for ballot selection) and `mostRecentInProgressCommitWithUpdate` (highest ballot that has a non-empty update, used to decide what to replay).

### Before / After

```java
// BEFORE
public Commit inProgressCommit;
// ...
if (response.inProgressCommit.isAfter(inProgressCommit))
    inProgressCommit = response.inProgressCommit;
```

```java
// AFTER
public Commit mostRecentInProgressCommit;
public Commit mostRecentInProgressCommitWithUpdate;
// ...
if (response.inProgressCommit.isAfter(mostRecentInProgressCommit))
    mostRecentInProgressCommit = response.inProgressCommit;
// Only track updates for non-empty commits
if (response.inProgressCommit.isAfter(mostRecentInProgressCommitWithUpdate) && !response.inProgressCommit.update.isEmpty())
    mostRecentInProgressCommitWithUpdate = response.inProgressCommit;
```

---

## How to Spot

When a consensus protocol tracks "the latest X" using a single field ordered by ballot/timestamp, check whether "latest" and "most significant for the action" can diverge. Here, clock skew meant the highest ballot and the only non-empty update were in different responses, requiring separate tracking.

---

## Test

No test added.
