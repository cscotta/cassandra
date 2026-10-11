# Bug 2155: Race Condition in Default Superuser Setup

**Commit**: 26e71c1ce4 · **Ticket**: CASSANDRA-5049
**Subsystem**: src/java/org/apache/cassandra/auth/Auth.java, src/java/org/apache/cassandra/service/StorageService.java
**Category**: Race condition / missing synchronization

---

## Root Cause

The original code scheduled superuser initialization as a delayed `Runnable` fired after `RING_DELAY` milliseconds from within `Auth.setup()`. This heuristic delay was not anchored to actual ring membership: the task could fire before the node had joined the ring and could write to auth tables, and on multi-node clusters the delay was not coordinated across nodes, allowing multiple nodes to race to insert the default superuser. The fix moves the superuser setup call to the point in `StorageService` where the node is confirmed to be serving reads, guaranteeing auth tables are available and the ring is established before the write occurs.

### Before / After

```java
// BEFORE (Auth.java) — heuristic delay, uncoordinated
Runnable runnable = new Runnable() {
    public void run() {
        if (QueryProcessor.process(...AUTH_KS, USERS_CF...).isEmpty())
            insertUser(DEFAULT_SUPERUSER_NAME, true);
    }
};
StorageService.tasks.schedule(runnable, StorageService.RING_DELAY, TimeUnit.MILLISECONDS);
```

```java
// AFTER (StorageService.java) — called after ring join is confirmed
logger.info("Startup completed! Now serving reads.");
Auth.setupSuperuser();   // only runs once ring membership is established
```

---

## How to Spot

Initialization tasks that touch distributed state should be triggered by an explicit lifecycle event (ring joined, schema ready) rather than a fixed-duration `schedule()` call. A `schedule(runnable, RING_DELAY, ...)` pattern is a red flag: if the delay is wrong or the cluster is slow, the task races with the actual readiness condition.

---

## Test

No test added.
