# Bug 1101: Fix MV Builder When Multiple MVs Were Added

**Commit**: 196b7ab540 · **Ticket**: CASSANDRA-10156
**Subsystem**: src/java/org/apache/cassandra/db/view/MaterializedViewBuilder.java
**Category**: State not cleaned up

---

## Root Cause

`MaterializedViewBuilder.run()` unconditionally called `SystemKeyspace.finishMaterializedViewBuildStatus()` at the end of its loop regardless of whether the builder had been stopped mid-way. When multiple Materialized Views are created simultaneously, each builder runs on a shared compaction executor; if an earlier builder is cancelled (e.g., because the second MV was added before the first finished), the stopped builder would still mark its view as "built" in the system keyspace. This caused the view to appear fully populated before its data was actually complete.

### Before / After

```java
// BEFORE
SystemKeyspace.finishMaterializedViewBuildStatus(ksname, viewName);
```

```java
// AFTER
if (!isStopped)
    SystemKeyspace.finishMaterializedViewBuildStatus(ksname, viewName);
```

---

## How to Spot

Any time a long-running background job has an early-exit path (a `stopped`, `cancelled`, or `interrupted` flag), every side-effectful action at the end of the job — especially persistent status writes — must be guarded by that flag. If "task complete" state is written unconditionally, cancellation silently becomes indistinguishable from successful completion in any downstream observer.

---

## Test

No test added.
