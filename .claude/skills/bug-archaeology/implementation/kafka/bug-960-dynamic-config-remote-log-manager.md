**Commit**: 12d8a1bbf8 · **Ticket**: KAFKA-19237
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Deprecate the `remote.log.manager.thread.pool.size` configuration and introduce a new dynamic configuration:
`remote.log.manager.follower.thread.pool.size`. Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>, Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
          RemoteLogManagerConfig.REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE_PROP.equals(k)) {
        val oldValue = server.config.getInt(k)

// After
          RemoteLogManagerConfig.REMOTE_LOG_MANAGER_EXPIRATION_THREAD_POOL_SIZE_PROP.equals(k) ||
          RemoteLogManagerConfig.REMOTE_LOG_MANAGER_FOLLOWER_THREAD_POOL_SIZE_PROP.equals(k)) {
        val oldValue: Int = {
          // This logic preserves backward compatibility in scenarios where
          // `remote.log.manager.thread.pool.size` is configured in config file,
          // but `remote.log.manager.follower.thread.pool.size` is set dynamically.
          // This can be removed once `remote.log.manager.thread.pool.size` is removed.
          if (RemoteLogManagerConfig.REMOTE_LOG_MANAGER_FOLLOWER_THREAD_POOL_SIZE_PROP.equals(k))
            server.config.remoteLogManagerConfig.remoteLogManagerFollowerThreadPoolSize()
          else
            server.config.getInt(k)
        }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19237: add dynamic config remote.log.manager.follower.thread.pool.size (#19809).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
