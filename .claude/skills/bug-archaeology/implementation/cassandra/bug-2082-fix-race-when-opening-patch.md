# Bug 2082: Race Condition Scheduling MeteredFlusher in ColumnFamilyStore Static Initializer

**Commit**: ffc9bece48 · **Ticket**: CASSANDRA-5350
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java, src/java/org/apache/cassandra/service/AbstractCassandraDaemon.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`ColumnFamilyStore` scheduled `MeteredFlusher` via a static initializer block. Static initializers run when the class is first loaded, which can happen during early startup — before `StorageService.optionalTasks` executor is fully initialized and ready to accept work. This created a race where loading `ColumnFamilyStore` (e.g., during system table bootstrap) would attempt to submit a task to an executor that was not yet started.

The fix removes the static initializer and instead schedules `MeteredFlusher` explicitly inside `AbstractCassandraDaemon.setup()`, at a point where the executor is guaranteed to be running.

### Before / After

```java
// BEFORE — in ColumnFamilyStore.java
static
{
    // (can block if flush queue fills up, so don't put on scheduledTasks)
    StorageService.optionalTasks.scheduleWithFixedDelay(new MeteredFlusher(), 1000, 1000, TimeUnit.MILLISECONDS);
}
```

```java
// AFTER — in AbstractCassandraDaemon.java, inside setup()
// MeteredFlusher can block if flush queue fills up, so don't put on scheduledTasks
StorageService.optionalTasks.scheduleWithFixedDelay(new MeteredFlusher(), 1000, 1000, TimeUnit.MILLISECONDS);
```

---

## How to Spot

Static initializers that touch global mutable state (thread pools, service registries, singletons) are inherently order-sensitive and introduce implicit coupling between class-loading order and runtime initialization. Any `static { ... }` block that calls into another class's static fields should be scrutinized for initialization-order races.

---

## Test

No test added.
