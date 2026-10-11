# Bug 0655: View Mutation Batchlog Not Cleaned Up for Local-and-Remote Paired Mutations

**Commit**: 12103653f3 · **Ticket**: CASSANDRA-13069
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: State not cleaned up

---

## Root Cause

`StorageProxy.mutateMV()` writes pending view mutations to a local batchlog so they can be replayed if the node crashes. The batchlog entry is removed once all mutations have been acknowledged via a `BatchlogCleanup` counter. The old code only stored remote (non-local) mutations in the batchlog but initialized `BatchlogCleanup` with the total mutation count, so each local mutation that was applied directly (bypassing the response-handler path) never decremented the counter. The counter therefore never reached zero and the batchlog entry was never removed, causing unbounded batchlog growth and replay of already-applied mutations. Additionally, local mutations were applied even when no paired endpoint existed, which was incorrect. The fix calls `cleanup.ackMutation()` for every locally applied mutation and changes the batchlog to store only the truly non-local mutations.

### Before / After

```java
// BEFORE – local mutations applied but cleanup counter never decremented
mutation.apply(writeCommitLog);
// cleanup not called; batchlog entry remains
```

```java
// AFTER – ack after local apply, remove from non-local set
mutation.apply(writeCommitLog);
nonLocalMutations.remove(mutation);
cleanup.ackMutation();
```

---

## How to Spot

Any two-phase "store-then-remove" pattern backed by a reference counter must account for every path that satisfies a unit of work. When some paths skip the normal response-handler flow (e.g., local fast-path), each such bypass must call the equivalent acknowledgement explicitly, or the counter will stall and the stored record will never be cleaned up.

---

## Test

No test added.
