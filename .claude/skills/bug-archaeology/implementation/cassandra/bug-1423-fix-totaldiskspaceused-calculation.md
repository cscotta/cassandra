# Bug 1423: Fix totalDiskSpaceUsed Calculation

**Commit**: 62386f13e5 · **Ticket**: CASSANDRA-8205
**Subsystem**: io/sstable/SSTableDeletingTask
**Category**: Wrong constant / default value

---

## Root Cause

`SSTableDeletingTask` captured the SSTable's on-disk byte size in its constructor via `referent.bytesOnDisk()` and stored it as a final field. The task is created before the SSTable is actually deleted, so the stored size reflected the pre-deletion state. However, `totalDiskSpaceUsed` is decremented by calling `tracker.notifyDeleting(referent)`, which reads the size from the live `SSTableReader` at the time of notification rather than from the cached field. When the two values diverged (for example due to concurrent compaction affecting size accounting), the metric subtracted the wrong amount and drifted from reality. The fix removes the cached `size` field entirely and reads `referent.bytesOnDisk()` at the start of `run()`, immediately before invoking `notifyDeleting`, so the same live value is used throughout the deletion.

### Before / After

```java
// BEFORE — size captured eagerly at construction time, potentially stale by run()
private final long size;

public SSTableDeletingTask(SSTableReader referent) {
    ...
    this.size = referent.bytesOnDisk();
}

public void run() {
    if (tracker != null)
        tracker.notifyDeleting(referent);  // uses referent internally, not the cached size
    ...
}
```

```java
// AFTER — size read lazily at deletion time, consistent with notifyDeleting
public void run() {
    long size = referent.bytesOnDisk();   // read here, before any state changes

    if (tracker != null)
        tracker.notifyDeleting(referent);
    ...
}
```

---

## How to Spot

When a value is computed at object construction but consumed only at a later execution point, ask whether the value could change between those two moments. For disk-size metrics that feed into counters, any gap between measurement and use is a window for drift. The rule of thumb is: measure as close as possible to the action that depends on the measurement, especially when the action mutates the system state that the measurement reflects.

---

## Test

`ColumnFamilyMetricTest.testSizeMetric` writes rows, flushes, checks that `totalDiskSpaceUsed` equals the sum of all SSTable sizes, then truncates and verifies the metric returns to zero.
