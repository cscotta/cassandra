# Bug 1324: CommitLog getPendingTasks Incorrectly Increments Counter

**Commit**: 2215c491c9 · **Ticket**: CASSANDRA-8862
**Subsystem**: db/commitlog/AbstractCommitLogService
**Category**: Logic error in condition

---

## Root Cause

`AbstractCommitLogService.getPendingTasks()` called `pending.incrementAndGet()` instead of `pending.get()`. Every call to this monitoring method was therefore a side effect that incremented the pending-task counter, making the reported pending count grow monotonically and independently of any actual work. This caused JMX metrics and any logic that polled `getPendingTasks()` to see inflated, ever-increasing values rather than the true snapshot of outstanding work.

### Before / After

```java
// BEFORE
public long getPendingTasks()
{
    return pending.incrementAndGet();  // side-effects the counter on every read
}
```

```java
// AFTER
public long getPendingTasks()
{
    return pending.get();  // pure read with no side effect
}
```

---

## How to Spot

Methods named `get*`, `count*`, or `pending*` should be pure reads. Any use of `incrementAndGet()`, `decrementAndGet()`, or other mutating atomic operations inside a getter is a strong code-smell. Code review should flag any `AtomicLong`/`AtomicInteger` mutation inside a method that is semantically a query rather than a command — the naming convention alone is a reliable heuristic.

---

## Test

No test added.
