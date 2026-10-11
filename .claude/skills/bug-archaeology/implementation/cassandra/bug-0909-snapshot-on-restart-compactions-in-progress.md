# Bug 0909: Snapshot Created at Restart for compactions_in_progress Table

**Commit**: 5a923f65c2 · **Ticket**: CASSANDRA-10962
**Subsystem**: ColumnFamilyStore / SystemKeyspace
**Category**: State not cleaned up

---

## Root Cause
`SystemKeyspace.discardCompactionsInProgress()` calls `truncateBlocking()` to wipe the `compactions_in_progress` internal table at node restart. `truncateBlocking()` unconditionally consulted `DatabaseDescriptor.isAutoSnapshot()`, and if auto-snapshots were enabled (the default), it would create a snapshot of this internal bookkeeping table on every single startup. These snapshots are useless (the table is transient state), accumulate on disk silently, and could cause startup to be perceptibly slower on nodes with many restarts. The fix adds an overload `truncateBlocking(boolean takeSnapshot)` and changes `discardCompactionsInProgress` to call `truncateBlocking(false)`, bypassing the snapshot step for this internal table regardless of global configuration.

### Before / After
```java
// BEFORE — always respects global auto-snapshot flag
public static void discardCompactionsInProgress() {
    ColumnFamilyStore compactionLog = ...;
    compactionLog.truncateBlocking();   // may snapshot
}

// AFTER — explicit no-snapshot
public static void discardCompactionsInProgress() {
    ColumnFamilyStore compactionLog = ...;
    compactionLog.truncateBlocking(false);  // never snapshot
}
```

---

## How to Spot
When internal or system tables are truncated programmatically (as part of bootstrap, cleanup, or restart routines), check whether the truncation path honours the global auto-snapshot setting. Internal state tables that hold ephemeral data (in-progress tracking, temp bookkeeping) should almost never be snapshotted. Look for calls to `truncateBlocking()` or similar without an explicit snapshot argument and verify whether the table in question is user-visible data or internal transient state.

---

## Test
No test added.
