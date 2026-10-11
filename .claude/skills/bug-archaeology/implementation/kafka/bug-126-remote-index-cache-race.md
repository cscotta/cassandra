**Commit**: 2cd733c9b3 · **Ticket**: KAFKA-17184
**Subsystem**: storage/src/main/java/org/apache/kafka/storage/internals/log/RemoteIndexCache.java
**Category**: Race condition / missing synchronization

**Root cause**
The `RemoteIndexCache` has two layers of locking: an outer `ReadWriteLock` coordinating reader and deleter threads, and an inner per-entry lock. The deleter thread called `markForCleanup()` on an entry and then removed it from the cache, both while holding only the read lock. A reader thread that had already obtained a reference to the entry would then hit an `IllegalStateException` when trying to use it, because the entry was marked before the cache map was updated. The fix moves the mark step to happen before the cache removal (so concurrent readers know the state) and introduces a two-flag design (`markedForCleanup` + `cleanStarted`) so that readers holding a stale reference can still read renamed-but-not-deleted index files.

**Before/After**
```java
// BEFORE – mark and removal ordering allowed a reader to see
// markedForCleanup=true while still holding a valid reference
private void enqueueEntryForCleanup(Entry entry, Uuid key) {
    entry.markForCleanup();
    expiredIndexes.offer(entry);   // separate ShutdownableThread drains this
}

// AFTER – mark happens first; cleanup itself (file deletion) is scheduled
// after a configurable delay, so readers still see the renamed files
private void enqueueEntryForCleanup(Entry entry) {
    entry.markForCleanup();
    cleanerScheduler.scheduleOnce("delete-index", () -> entry.cleanup(), fileDeleteDelayMs);
}
```

**How to spot**
Look for any cache where entries are marked invalid before they are removed from the shared map. If readers can hold references across the remove-and-mark boundary without an exclusive lock, they may observe the "invalid" state and throw, even though the underlying data is still accessible. The symptom is a spurious `IllegalStateException` in remote-log reader threads under concurrent eviction.

**Test**
`testFetchIndexAccessibleWhenMarkedForCleanup`: acquires an entry, calls `cache.remove()`, verifies that the entry object still answers `lookupOffset()` (files were only renamed, not deleted), then calls `cleanup()` and asserts that subsequent access throws `IllegalStateException`.
