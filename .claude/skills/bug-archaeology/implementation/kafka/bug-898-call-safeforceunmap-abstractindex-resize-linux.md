**Commit**: ea7b145860 · **Ticket**: KAFKA-19390

**Subsystem**: Storage / AbstractIndex (mmap lifecycle)

**Category**: Resource leak

**Root cause**
`AbstractIndex.resize()` called `safeForceUnmap()` on the old memory-mapped buffer only on Windows and z/OS, not on Linux. On Linux, the old mmap was relied upon to be collected by the GC, but Mixed GC pauses could defer collection long enough for the broker to exhaust the OS `vm.max_map_count` limit and crash. The fix calls `safeForceUnmap()` unconditionally on all platforms and adds a `ReentrantReadWriteLock` around remap operations to prevent concurrent access to a buffer that is being unmapped, integrated with the existing `LockUtils` helpers.

**Before/After**
```java
// Before: unmap only on Windows/z/OS
if (OperatingSystem.IS_WINDOWS || OperatingSystem.IS_ZOS) {
    safeForceUnmap(oldBuffer);
}

// After: unmap on all platforms under write lock
LockUtils.inLock(remapLock.writeLock(), () -> {
    safeForceUnmap(oldBuffer);
    // remap to new size
});
```

**How to spot**
Platform-guarded `safeForceUnmap` calls that skip Linux are a red flag; Linux is the dominant production platform and mmap exhaustion (`ENOMEM` from `mmap(2)`) is a real operational issue. Check for `IS_WINDOWS || IS_ZOS` guards on resource-release operations.

**Test**
`AbstractIndexTest` verifies that after multiple resize operations on Linux, no stale mmaps remain and the read lock correctly blocks concurrent readers during remapping.
