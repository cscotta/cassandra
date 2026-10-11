# Bug 0350: Batchlog Entry Incorrectly Removed for Local Mutations During View Replication

**Commit**: 57f53f53ae · **Ticket**: CASSANDRA-16175
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: State not cleaned up

---

## Root Cause

During materialized view replication, `StorageProxy` writes a batchlog entry to allow recovery if the view update is only partially applied. For local mutations that are applied directly in-process, the code called `cleanup.ackMutation()` after applying each mutation. `ackMutation()` decrements a counter and, when it reaches zero, invokes the cleanup callback which removes the batchlog entry. However, the batchlog entry was only written for remote mutations; local mutations were handled separately and no batchlog entry was ever created for them. Calling `ackMutation()` for a local mutation could fire the cleanup callback prematurely (before all remote mutations were acknowledged), deleting the batchlog entry while remote mutations were still in flight.

### Before / After

```java
// BEFORE
mutation.apply(writeCommitLog);
nonLocalMutations.remove(mutation);
cleanup.ackMutation();   // could fire cleanup callback prematurely
```

```java
// AFTER
mutation.apply(writeCommitLog);
nonLocalMutations.remove(mutation);
// won't trigger cleanup
cleanup.decrement();   // decrements counter only, never invokes callback
```

---

## How to Spot

When a completion callback is shared between two code paths that have different invariants (one creates the batchlog entry, one does not), the callback must only be invoked by the path that owns the resource. Splitting the decrement from the trigger-and-cleanup action (as done here) is the correct pattern; alternatively, guard the callback invocation with a check that the resource was actually created.

---

## Test

Adds `ViewTest.testNoBatchlogCleanupForLocalMutations`: inserts into a table with a materialized view, flushes the batchlog, and asserts that no new batchlog SSTable was created (i.e. no spurious batchlog write occurred).
