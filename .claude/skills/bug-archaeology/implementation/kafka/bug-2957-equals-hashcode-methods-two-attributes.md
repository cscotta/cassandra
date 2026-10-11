**Commit**: 64c50a274b · **Ticket**: KAFKA-16880
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                            remoteLogMetadataCustomMetadataMaxBytes, remoteLogIndexFileCacheTotalSizeBytes, remoteLogManagerThreadPoolSize, remoteLogManagerTaskIntervalMs,
import org.junit.jupiter.api.Assertions;
        String remoteLogMetadataManagerClass = useDefaultRemoteLogMetadataManagerClass ? DEFAULT_REMOTE_LOG_METADATA_MANAGER_CLASS_NAME : "dummy.remote.log.metadata.class";
        RemoteLogManagerConfig expectedRemoteLogManagerConfig
                = new RemoteLogManagerConfig(true, "dummy.remote.storage.class", "dummy.remote.storage.class.path",
                                             remoteLogMetadataManagerClass, "dummy.remote.log.metadata.class.path",
                                             "listener.name", 1024 * 1024L, 1, 1, 1, 60000L, 100L, 60000L, 0.3, 10, 100, 100,
                                             rsmPrefix, rsmProps, rlmmPrefix, rlmmProps, Long.MAX_VALUE, 11, 1,
                                             Long.MAX_VALUE, 11, 1);
        Assertions.assertEquals(expectedRemoteLogManagerConfig, remoteLogManagerConfig);

// After
                && remoteLogManagerCopierThreadPoolSize == that.remoteLogManagerCopierThreadPoolSize
                && remoteLogManagerExpirationThreadPoolSize == that.remoteLogManagerExpirationThreadPoolSize
                            remoteLogMetadataCustomMetadataMaxBytes, remoteLogIndexFileCacheTotalSizeBytes, remoteLogManagerThreadPoolSize,
                            remoteLogManagerCopierThreadPoolSize, remoteLogManagerExpirationThreadPoolSize, remoteLogManagerTaskIntervalMs,
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
        RemoteLogManagerConfig expectedRemoteLogManagerConfig = getRemoteLogManagerConfig(useDefaultRemoteLogMetadataManagerClass,
                rsmPrefix,
                rlmmPrefix,
                rsmProps,
                rlmmProps);
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16880 update equals and hashcode methods for two attributes  (#16173).

**Test**
Test modified in RemoteLogManagerConfigTest.java covering the corrected behaviour.
