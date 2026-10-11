**Commit**: 6437135bc0 · **Ticket**: KAFKA-19451

**Subsystem**: Remote Storage / RemoteIndexCacheTest

**Category**: Flaky test

**Root cause**
`RemoteIndexCacheTest` called `Files.walk()` to assert that a file no longer existed after requesting deletion, but `deleteIfExists()` completes asynchronously in a background thread. The test checked for file absence immediately after issuing the delete, creating a race where the file might still be present. The fix adds an explicit wait (e.g., `TestUtils.waitForCondition`) until `deleteIfExists` has completed before asserting file absence.

**Before/After**
```java
// Before: immediate assertion after async delete
cache.remove(key);
assertFalse(Files.exists(indexFile)); // races with async delete

// After: wait for deletion to complete
cache.remove(key);
TestUtils.waitForCondition(
    () -> !Files.exists(indexFile),
    "Index file was not deleted in time");
```

**How to spot**
Assertions on file-system state that follow asynchronous delete operations are prone to races. Always use a polling wait condition rather than an immediate assertion when the operation under test runs in a separate thread.

**Test**
`RemoteIndexCacheTest` passes consistently after the wait condition is introduced, even under high scheduler load.
