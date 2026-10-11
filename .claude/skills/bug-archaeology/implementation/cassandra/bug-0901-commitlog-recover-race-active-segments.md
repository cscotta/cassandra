# Bug 0901: Race Condition in CommitLog.recover — Active Segments Replayed

**Commit**: 6add3c9acc · **Ticket**: CASSANDRA-11743
**Subsystem**: CommitLog
**Category**: Race condition / missing synchronization

---

## Root Cause
`CommitLog.recover()` used `allocator.manages(name)` to exclude segments that the allocator was currently managing from the replay file list. However, the allocator initializes asynchronously: it launches reserve-segment creation on a background thread, and there was a brief window during startup where a segment file could exist on disk before the allocator had registered it. During this window, `manages(name)` returned `false` even for an active (already-writing) segment, causing it to be included in replay. The previous workaround called `allocator.allocatingFrom()` to block until at least one segment was ready, but this was fragile and order-dependent. The fix replaces the allocator query with a timestamp-based cutoff: at static initialization time `CommitLogSegment` records the id of the next segment to be created (`replayLimitId`), and `shouldReplay(name)` returns `true` only for segments whose id pre-dates that cutoff. Segments created after node startup are always excluded.

### Before / After
```java
// BEFORE — relies on allocator liveness, fragile
allocator.allocatingFrom();  // force at least one segment to be allocated
FilenameFilter unmanagedFilesFilter = (dir, name) ->
    CommitLogDescriptor.isValid(name) && !allocator.manages(name);

// AFTER — stable timestamp cutoff baked in at class initialization
static { replayLimitId = idBase = Math.max(System.currentTimeMillis(), maxId + 1); }

static boolean shouldReplay(String name) {
    return CommitLogDescriptor.fromFileName(name).id < replayLimitId;
}

FilenameFilter unmanagedFilesFilter = (dir, name) ->
    CommitLogDescriptor.isValid(name) && CommitLogSegment.shouldReplay(name);
```

---

## How to Spot
Any code that uses an asynchronously-initialized object's state (e.g. `allocator.manages(...)`) as a filter for safety-critical decisions is susceptible to this pattern. Prefer recording a stable snapshot of "what existed before startup" rather than querying a live object whose internal state may not yet be consistent. This is especially important when the filter guards which data gets replayed or skipped.

---

## Test
No test added in this commit.
