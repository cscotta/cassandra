# Bug 1499: Fix race in background compaction check

**Commit**: 22a4629d25 · **Ticket**: CASSANDRA-7745
**Subsystem**: db/compaction/CompactionManager
**Category**: Race condition / missing synchronization

---

## Root Cause

`CompactionManager.submitBackground` submits a `BackgroundCompactionTask` to the executor and tracks in-progress column family stores in the `compactingCF` set. The original ordering added the task to the executor _before_ registering the CFS in `compactingCF`. A concurrently running task could complete and remove the CFS from `compactingCF` in the window between submission and registration, causing the check that prevents duplicate compaction submission to miss the CFS and schedule a redundant compaction.

### Before / After

```java
// BEFORE
do {
    futures.add(executor.submit(new BackgroundCompactionTask(cfs)));
    compactingCF.add(cfs);
} while (executor.getActiveCount() + futures.size() < executor.getMaximumPoolSize());
```

```java
// AFTER
do {
    compactingCF.add(cfs);
    futures.add(executor.submit(new BackgroundCompactionTask(cfs)));
} while (executor.getActiveCount() + futures.size() < executor.getMaximumPoolSize());
```

---

## How to Spot

Whenever a resource is added to a tracking set to signal "in use", that registration must happen before the resource becomes visible to other threads (i.e., before submission to an executor). Reversed ordering creates a window where the invariant is violated.

---

## Test

No test added.
