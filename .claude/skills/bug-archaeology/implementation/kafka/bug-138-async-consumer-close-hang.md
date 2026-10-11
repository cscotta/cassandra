**Commit**: 92169b8f08 · **Ticket**: KAFKA-19357
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java
**Category**: Deadlock

**Root cause**
When the async consumer was closing, `CoordinatorRequestManager.poll()` returned `EMPTY` unconditionally if the closing flag was set, preventing coordinator lookups from completing. Meanwhile, `CommitRequestManager` was waiting for all pending `commitAsync` futures to complete before `close()` could return. With the coordinator permanently unreachable (poll returning EMPTY), those futures never completed, causing `close()` to hang until the timeout expired. The fix detects the specific condition (closing + coordinator unknown) and proactively fails all pending commit requests with a `CommitFailedException`, unblocking the close path.

**Before/After**
```java
// BEFORE – pending commits could never complete during close if
// coordinator was unavailable; close() hung until timeout
// (no drain-pending-commits logic existed)

// AFTER – drains pending commits when closing with no coordinator
if (closing && pendingRequests.hasUnsentRequests()) {
    CommitFailedException exception = new CommitFailedException(
        "Failed to commit offsets: Coordinator unknown and consumer is closing");
    pendingRequests.drainPendingCommits()
        .forEach(request -> request.future().completeExceptionally(exception));
}
```

**How to spot**
Any `close()` that waits on futures that depend on an asynchronous background task (like a coordinator lookup) must handle the case where the background task is suppressed during shutdown. Look for shutdown flags that short-circuit work that other tasks are waiting on, without providing a failure signal to those waiters.

**Test**
`testCommitAsyncFailsWhenCoordinatorUnavailableDuringClose`: shuts down all brokers, calls `commitAsync`, measures that `consumer.close()` returns in under 1 second and that the callback received a `CommitFailedException`.
