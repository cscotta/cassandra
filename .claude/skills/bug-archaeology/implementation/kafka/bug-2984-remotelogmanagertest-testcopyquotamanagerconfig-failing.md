**Commit**: b0fb2ac06d · **Ticket**: KAFKA-16866
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Justine Olshan <jolshan@confluent.io>, Satish Duggana <satishd@apache.org>

**Before/After**
```java
// Before
        assertEquals(Long.MAX_VALUE, defaultConfig.quotaBytesPerSecond());
        assertEquals(61, defaultConfig.numQuotaSamples());
        assertEquals(1, defaultConfig.quotaWindowSizeSeconds());
        assertEquals(Long.MAX_VALUE, defaultConfig.quotaBytesPerSecond());
        assertEquals(11, defaultConfig.numQuotaSamples());
        assertEquals(1, defaultConfig.quotaWindowSizeSeconds());

// After
import static org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig.DEFAULT_REMOTE_LOG_MANAGER_COPY_MAX_BYTES_PER_SECOND;
import static org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig.DEFAULT_REMOTE_LOG_MANAGER_COPY_QUOTA_WINDOW_NUM;
import static org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig.DEFAULT_REMOTE_LOG_MANAGER_COPY_QUOTA_WINDOW_SIZE_SECONDS;
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_MAX_BYTES_PER_SECOND, defaultConfig.quotaBytesPerSecond());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_QUOTA_WINDOW_NUM, defaultConfig.numQuotaSamples());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_QUOTA_WINDOW_SIZE_SECONDS, defaultConfig.quotaWindowSizeSeconds());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_MAX_BYTES_PER_SECOND, defaultConfig.quotaBytesPerSecond());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_QUOTA_WINDOW_NUM, defaultConfig.numQuotaSamples());
        assertEquals(DEFAULT_REMOTE_LOG_MANAGER_COPY_QUOTA_WINDOW_SIZE_SECONDS, defaultConfig.quotaWindowSizeSeconds());
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16866 remotelogmanagertest.testcopyquotamanagerconfig failing (#16146).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
