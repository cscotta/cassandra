# Bug 0667: PendingRepairManager getNextBackgroundTask Crashes When All Sessions Need Cleanup

**Commit**: c066f126e2 · **Ticket**: CASSANDRA-13769
**Subsystem**: src/java/org/apache/cassandra/db/compaction/PendingRepairManager.java
**Category**: Missing null / bounds check

---

## Root Cause

`getNextBackgroundTask` builds a list of session UUIDs that have candidate compaction tasks and then sorts it. When every pending repair session is already finalized and needs only cleanup (not compaction), the pre-sort filtering loop would exclude all sessions, leaving the `sessions` list empty. The code then proceeded to call `sessions.sort(...)` and `sessions.get(0)` on the empty list, throwing an `IndexOutOfBoundsException`. The invariant "there is always at least one session" was assumed but never enforced.

### Before / After

```java
// BEFORE — proceeds to sort and index into potentially empty list
sessions.sort((o1, o2) -> numTasks.get(o2) - numTasks.get(o1));
UUID sessionID = sessions.get(0);  // IOOBE if sessions is empty
```

```java
// AFTER — early return on empty list
if (sessions.isEmpty())
    return null;

sessions.sort((o1, o2) -> numTasks.get(o2) - numTasks.get(o1));
UUID sessionID = sessions.get(0);
```

---

## How to Spot

Any time a list is built by selectively adding entries and then immediately indexed without checking emptiness is a potential IOOBE. The pattern `list.sort(...); list.get(0)` should always be preceded by a `list.isEmpty()` guard unless the caller guarantees non-emptiness through invariants that are themselves verified.

---

## Test

A new test `getNextBackgroundTaskAllCleanup` creates a finalized repair session (all sstables needing cleanup, no compaction tasks) and asserts that `getNextBackgroundTask` returns `null` rather than throwing.
