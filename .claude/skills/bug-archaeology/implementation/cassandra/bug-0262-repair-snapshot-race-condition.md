# Bug 0262: Race Condition on Repair Snapshot Creation

**Commit**: 35ef5b9957 · **Ticket**: CASSANDRA-17955
**Subsystem**: src/java/org/apache/cassandra/db/repair/CassandraTableRepairManager.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`CassandraTableRepairManager.snapshot()` was a `synchronized` method that called `cfs.snapshot()` directly from the repair coordination thread. However, snapshot operations (which involve filesystem work including hardlink creation) were also dispatched via `ActiveRepairService.clearSnapshotExecutor`. The snapshot could therefore race with a concurrent snapshot cleanup submitted to that executor, producing duplicate-hardlink errors or incomplete snapshots. The fix serialises all snapshot creation through the same single-threaded `snapshotExecutor` (renamed from `clearSnapshotExecutor`) that already serialises cleanup, eliminating the race.

### Before / After

```java
// BEFORE — snapshot taken inline on repair thread, racing the executor
public synchronized void snapshot(String name, Collection<Range<Token>> ranges, boolean force) {
    if (force || !cfs.snapshotExists(name)) {
        cfs.snapshot(name, predicate, true, false);
    }
}
```

```java
// AFTER — snapshot submitted to the shared single-threaded executor and waited on
public synchronized void snapshot(String name, Collection<Range<Token>> ranges, boolean force) {
    try {
        ActiveRepairService.instance.snapshotExecutor.submit(() -> {
            if (force || !cfs.snapshotExists(name))
                cfs.snapshot(name, predicate, true, false);
        }).get();
    } catch (Exception ex) {
        throw new RuntimeException("Unable to take snapshot ...", ex);
    }
}
```

---

## How to Spot

When a class has a dedicated single-threaded executor for a resource-sensitive operation (here, filesystem snapshot management), look for any code path that touches the same resource outside that executor. A `synchronized` method alone does not prevent races against tasks already queued on the executor, since `synchronized` only serialises callers of that method, not the executor's queue. The canonical fix is to route all access through the same executor so ordering is guaranteed.

---

## Test

No test added; the fix is structural — routing snapshot creation through the existing serialising executor.
