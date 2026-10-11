# Bug 0805: CommitLog resetUnsafe Replays Active Segments in Tests

**Commit**: 079029a442 · **Ticket**: CASSANDRA-12802
**Subsystem**: CommitLog
**Category**: State not cleaned up

---

## Root Cause
`CommitLog.resetUnsafe()` is called between test cases to discard all existing commit log state. The file filter used during `recover()` previously excluded only segments currently managed by the allocator (`!instance.allocator.manages(name)`). After a reset, the allocator was re-initialized with a new empty state, so segments created before the reset were no longer tracked—yet they still existed on disk and passed the filter, causing them to be replayed into the freshly reset commit log. This produced spurious data in subsequent tests. The fix introduces a `replayLimitId` watermark: at reset time, `resetReplayLimit()` advances the watermark to the current segment ID, and `shouldReplay(name)` rejects any segment whose ID is at or above the watermark. Additionally, `resetUnsafe()` now synchronizes on itself and waits for all pending `SSTableDeletingTask` deletions to drain before resetting, removing a race where GlobalTidy mutations could allocate into a segment that was being closed.

### Before / After
```java
// BEFORE — filter excluded only allocator-managed segments; post-reset segments slipped through
return CommitLogDescriptor.isValid(name) && !instance.allocator.manages(name);

// AFTER — filter excludes segments at or above the replay watermark
return CommitLogDescriptor.isValid(name) && CommitLogSegment.shouldReplay(name);

// resetUnsafe now advances the watermark before allowing new segments
static void resetReplayLimit() {
    replayLimitId = getNextId();
}
```

---

## How to Spot
Test-only reset paths are a frequent source of cross-test contamination. When reviewing `resetUnsafe`-style methods, check whether all durable state (on-disk files, segment IDs, global counters) is consistently invalidated so that nothing from a prior run can re-enter through a path that only partially clears state. Also look for races between asynchronous cleanup tasks (like `SSTableDeletingTask`) and synchronous reset methods—any task that can write to the commit log after the reset starts may corrupt the new clean state.

---

## Test
`RecoveryManagerTruncateTest` was fixed as a direct result; the test previously failed intermittently due to pre-reset segments being replayed.
