## Commit

847968e530 — KAFKA-19281: ShareCoordinator periodic jobs run even when share groups are disabled

## Ticket

KAFKA-19281

## Subsystem

Share coordinator / feature gating

## Category

State not cleaned up

## Root cause

`ShareCoordinatorService` started its record-pruning and cold-partition snapshotting jobs unconditionally during `startup()`, regardless of whether the `share.version` feature was enabled. When share groups were disabled, these jobs executed against non-existent or uninitialized state, causing spurious errors and wasted compute. Because the jobs were started before the feature check, there was also no path that would stop them if the feature was later disabled via a metadata update. The fix introduces a `shouldRunPeriodicJob` flag that is set only when the feature is observed as enabled, and periodic job scheduling is deferred until that flag is set.

## Before / After

```java
// Before — jobs start unconditionally
public void startup() {
    scheduleRecordPruningJob();
    scheduleColdPartitionSnapshotJob();
}

// After — jobs start only when feature is enabled
public void onShareVersionEnabled() {
    shouldRunPeriodicJob = true;
    scheduleRecordPruningJob();
    scheduleColdPartitionSnapshotJob();
}
```

## How to spot

Search for `schedule*` calls in `startup()` or `start()` methods of coordinator services. Any such call that is not gated on the relevant feature flag will execute against uninitialized state when the feature is disabled, and will not stop when the feature is toggled off.

## Test

`ShareCoordinatorServiceTest` was extended to verify that no periodic job callbacks are invoked when the share version feature remains disabled.
