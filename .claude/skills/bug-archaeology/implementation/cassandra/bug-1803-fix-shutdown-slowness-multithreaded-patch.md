# Bug 1803: Commit Log Service Hangs on Shutdown When Sync Is Already Overdue

**Commit**: 5edf94842e · **Ticket**: CASSANDRA-6578
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/AbstractCommitLogService.java
**Category**: Logic error in condition

---

## Root Cause

The commit log sync thread's sleep-avoidance path checked only `if (sleep < 0)` to decide whether to skip sleeping and immediately loop. When `run` was set to `false` to signal shutdown, the thread could still enter `Thread.sleep()` for a full sync interval if the calculated `sleep` value was non-negative, delaying shutdown by up to the full sync period. The fix adds `|| !run` to the condition so that a shutdown signal is also treated as a reason to skip sleeping and proceed to the next loop iteration, where the exit condition is checked.

### Before / After

```java
// BEFORE
if (sleep < 0)
    continue;
```

```java
// AFTER
if (sleep < 0 || !run)
    continue;
```

---

## How to Spot

In any loop that sleeps for a configurable interval, check whether every exit condition (shutdown flags, interrupts) is also checked before the sleep call, not just after it. A shutdown flag set by another thread will not wake a sleeping thread unless the sleep is on an `Object.wait` / `Condition.await` that can be notified.

---

## Test

No test added.
