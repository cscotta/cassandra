**Commit**: 734d309a29 · **Ticket**: KAFKA-16852
**Subsystem**: storage/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>, Chia-Ping Tasi <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    public static final String REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE_PROP = "remote.log.manager.copier.thread.pool.size";
    public static final String REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE_DOC = "Size of the thread pool used in " +
            "scheduling tasks to copy segments.";
    public static final int DEFAULT_REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE = 10;

    public static final String REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE_PROP = "remote.log.manager.expiration.thread.pool.size";
    public static final String REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE_DOC = "Size of the thread pool used in" +
            " scheduling tasks to clean up remote log segments.";
    public static final int DEFAULT_REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE = 10;

                  .defineInternal(REMOTE_LOG_MANAGER_COPIER_THREAD_POOL_SIZE_PROP,
                        INT,
```

**How to spot**
When reviewing storage/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16852 adding two thread pools kafka-16852 (#16154).

**Test**
Test modified in RemoteLogManagerConfigTest.java covering the corrected behaviour.
