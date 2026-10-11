# Bug 1946: Periodic memtable flush reschedules incorrectly for clean memtables

**Commit**: b0280cb92d · **Ticket**: CASSANDRA-5931
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Logic error in condition

---

## Root Cause

The periodic flush scheduler checked whether `forceFlush()` returned `null` to decide whether to reschedule. However `forceFlush()` returned `null` both when the memtable was clean (nothing to flush) and in other edge cases, and the return value was not a reliable signal. More critically, when a non-clean memtable was flushed, the code never arranged for the next periodic flush to be scheduled — the new memtable's constructor was supposed to call `scheduleFlush()`, but the original code called `forceFlush()` first and then checked the return value to decide whether to also call `scheduleFlush()`, which meant the rescheduling could be skipped. The fix inverts the logic: check `isClean()` directly before deciding to flush, and rely on the new memtable constructor to reschedule when a real flush happens.

### Before / After

```java
// BEFORE — unreliable null-return check; could skip rescheduling
Future<?> future = forceFlush();
if (future == null)
    scheduleFlush();
```

```java
// AFTER — explicit cleanliness check; flush path relies on new memtable ctor
if (isClean())
    scheduleFlush();
else
    forceFlush(); // scheduleFlush() will be called by the constructor of the new memtable.
```

---

## How to Spot

When a scheduling loop uses the return value of a side-effecting method as the branching condition, verify that the return value is truly the right signal and that all paths through the loop correctly re-arm the next scheduled event. Missing a re-arm on the "work done" branch means periodic tasks silently stop running.

---

## Test

No test added.
