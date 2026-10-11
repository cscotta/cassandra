**Commit**: 965743c35b · **Ticket**: KAFKA-19131
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
The remote storage reader thread pool use same count for both maximum
and core size. If users adjust the pool size larger than original value,
it throws `IllegalArgumentException`. Updated both value to fix the
issue. ---------

Signed-off-by: PoAn Yang <payang@apache.org>

Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
        remoteStorageReaderThreadPool.setCorePoolSize(newSize);

// After
        int currentMaximumSize = remoteStorageReaderThreadPool.getMaximumPoolSize();
        if (newSize > currentMaximumSize) {
            remoteStorageReaderThreadPool.setMaximumPoolSize(newSize);
            remoteStorageReaderThreadPool.setCorePoolSize(newSize);
        } else {
            remoteStorageReaderThreadPool.setCorePoolSize(newSize);
            remoteStorageReaderThreadPool.setMaximumPoolSize(newSize);
        }
    // Visible for testing
    int readerThreadPoolSize() {
        return remoteStorageReaderThreadPool.getCorePoolSize();
    }
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19131: adjust remote storage reader thread maximum pool size to avoid illegal argument (#19532).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
