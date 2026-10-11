# Bug 0602: Speculative Retry Threshold Scheduled Per-CFS Instead of Globally

**Commit**: 8b7e96761f · **Ticket**: CASSANDRA-14338
**Subsystem**: `src/java/org/apache/cassandra/db/ColumnFamilyStore.java`, `src/java/org/apache/cassandra/service/CassandraDaemon.java`
**Category**: Wrong constant / default value

---

## Root Cause

Each `ColumnFamilyStore` instance scheduled its own `ScheduledFuture` to recompute `sampleLatencyNanos` via `SpeculativeRetryPolicy.calculateThreshold`. The `isDynamic()` method was used to conditionally skip scheduling for non-percentile policies (ALWAYS, NEVER, FIXED), but this meant every table that was percentile-based ran its own independent timer, causing unnecessary resource usage and making the recalculation tied to the per-CFS lifecycle rather than the daemon lifecycle. Additionally, when a CFS was closed the future had to be cancelled separately. The fix removes `isDynamic()` and the per-CFS `ScheduledFuture`, and instead schedules a single global task in `CassandraDaemon` that iterates all `ColumnFamilyStore` instances via `Keyspace.all()` and calls the new `updateSpeculationThreshold()` method, which unconditionally recomputes the threshold (cheap for non-percentile policies).

### Before / After

```java
// BEFORE — per-CFS scheduled future, only for dynamic policies
SpeculativeRetryPolicy retryPolicy = metadata.get().params.speculativeRetry;
if (!retryPolicy.isDynamic())
{
    sampleLatencyNanos = retryPolicy.calculateThreshold(metric.coordinatorReadLatency);
    latencyCalculator = null;
}
else
{
    latencyCalculator = ScheduledExecutors.optionalTasks.scheduleWithFixedDelay(...);
}
```

```java
// AFTER — single global task in CassandraDaemon, no per-CFS future
// CassandraDaemon:
ScheduledExecutors.optionalTasks.scheduleWithFixedDelay(
    () -> Keyspace.all().forEach(k ->
        k.getColumnFamilyStores().forEach(ColumnFamilyStore::updateSpeculationThreshold)),
    DatabaseDescriptor.getReadRpcTimeout(),
    DatabaseDescriptor.getReadRpcTimeout(),
    TimeUnit.MILLISECONDS);

// ColumnFamilyStore.updateSpeculationThreshold():
public void updateSpeculationThreshold()
{
    sampleLatencyNanos = metadata().params.speculativeRetry
                                   .calculateThreshold(metric.coordinatorReadLatency);
}
```

---

## How to Spot

When a recurring task logically operates on a global collection (all tables in all keyspaces), scheduling it once per element in that collection is wasteful and creates lifecycle-management complexity. Look for `ScheduledFuture` fields on per-instance classes and ask whether the task could instead be aggregated into a single daemon-level scheduler.

---

## Test

No test added.
