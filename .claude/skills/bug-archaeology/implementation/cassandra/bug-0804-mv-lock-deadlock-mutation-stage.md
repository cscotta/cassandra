# Bug 0804: Deadlock When All Mutation Workers Block on MV Lock

**Commit**: d38a732ce1 · **Ticket**: CASSANDRA-12689
**Subsystem**: Materialized Views / Write Path
**Category**: Deadlock

---

## Root Cause
When a mutation with a Materialized View update cannot immediately acquire the MV row lock, the original code re-submitted the mutation as a new task to the `MUTATION` stage executor and returned a `CompletableFuture` to the caller. If the caller was itself executing on the `MUTATION` stage and was blocking synchronously on that future (e.g., via `Uninterruptibles.getUninterruptibly()`), all mutation worker threads could end up blocked waiting for futures whose continuations were queued behind them but could never execute—a classic thread-pool deadlock. The fix introduces `applyNotDeferrable()`, which retries the lock on the same calling thread (sleeping briefly between attempts) instead of re-queuing, used by all blocking callers (`Mutation.apply(boolean)` and `PaxosState`).

### Before / After
```java
// BEFORE — blocking caller submits deferred retry to the same executor pool it occupies
Uninterruptibles.getUninterruptibly(applyFuture(durableWrites));
// applyFuture internally re-queues to MUTATION stage on lock failure → deadlock

// AFTER — non-deferrable path spins on current thread
public CompletableFuture<?> applyNotDeferrable(Mutation mutation, boolean writeCommitLog) {
    return apply(mutation, writeCommitLog, true, false, false, null);
}
// isDeferrable=false causes apply() to Thread.sleep(10) and retry instead of re-queuing
```

---

## How to Spot
Any time code submits a task to an executor and then blocks the same executor thread waiting for the result, a deadlock is possible if the thread pool is bounded and fully occupied. Look for patterns of `getUninterruptibly(stage.execute(...))` or `future.get()` called from within a task already running on the same stage. In Cassandra's `MUTATION` stage especially, re-queuing under lock contention is safe only when the queuing thread is not itself blocking on the result.

---

## Test
A `TEST_FAIL_MV_LOCKS_COUNT` system property hook was added to force lock acquisition failures in testing, enabling targeted reproduction of the deadlock scenario.
