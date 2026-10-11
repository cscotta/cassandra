# Bug 1476: StressAction Calls Wrong Stop Method for Duration-Based Runs

**Commit**: eec381e0d6 · **Ticket**: n/a
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/StressAction.java
**Category**: Logic error in condition

---

## Root Cause

In `StressAction`, when a stress run was configured with a duration (rather than an operation count), the code called `workQueue.stop()` after sleeping for the duration. However the field controlling the work lifecycle had been renamed or refactored to `workManager`, so calling `workQueue.stop()` either invoked the wrong object's stop method or referenced a stale/incorrect variable. The consequence is that the stress run would not terminate correctly at the end of the requested duration. The fix changes the call to `workManager.stop()`.

### Before / After

```java
// BEFORE
Uninterruptibles.sleepUninterruptibly(duration, durationUnits);
workQueue.stop();
```

```java
// AFTER
Uninterruptibles.sleepUninterruptibly(duration, durationUnits);
workManager.stop();
```

---

## How to Spot

When refactoring a field name, search for all references to the old name — IDE rename refactors miss hand-rolled string matches and subtle aliased references. Duration-based termination paths are easy to miss in testing because most tests use op-count-based runs.

---

## Test

No test added.
