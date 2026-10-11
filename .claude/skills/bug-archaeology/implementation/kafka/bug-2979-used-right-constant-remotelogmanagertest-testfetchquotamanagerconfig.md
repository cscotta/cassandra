**Commit**: cdd4455cb8 · **Ticket**: KAFKA-16866
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_MAX_BYTES_PER_SECOND, defaultConfig.quotaBytesPerSecond());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_QUOTA_WINDOW_NUM, defaultConfig.numQuotaSamples());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_QUOTA_WINDOW_SIZE_SECONDS, defaultConfig.quotaWindowSizeSeconds());

// After
import static org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig.DEFAULT_REMOTE_LOG_MANAGER_FETCH_MAX_BYTES_PER_SECOND;
import static org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig.DEFAULT_REMOTE_LOG_MANAGER_FETCH_QUOTA_WINDOW_NUM;
import static org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig.DEFAULT_REMOTE_LOG_MANAGER_FETCH_QUOTA_WINDOW_SIZE_SECONDS;
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_FETCH_MAX_BYTES_PER_SECOND, defaultConfig.quotaBytesPerSecond());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_FETCH_QUOTA_WINDOW_NUM, defaultConfig.numQuotaSamples());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_FETCH_QUOTA_WINDOW_SIZE_SECONDS, defaultConfig.quotaWindowSizeSeconds());
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16866 used the right constant in remotelogmanagertest#testfetchquotamanagerconfig (#16152).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
