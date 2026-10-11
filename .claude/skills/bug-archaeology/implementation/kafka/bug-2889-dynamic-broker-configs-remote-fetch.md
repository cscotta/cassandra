**Commit**: b5fb6543a2 · **Ticket**: KAFKA-15265
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kamal Chandraprakash<kamal.chandraprakash@gmail.com>, Satish Duggana <satishd@apache.org>

**Before/After**
```java
// Before
      if (reconfigurableConfigs.contains(k)) {
        if (k.equals(RemoteLogManagerConfig.REMOTE_LOG_INDEX_FILE_CACHE_TOTAL_SIZE_BYTES_PROP)) {
          val newValue = v.asInstanceOf[Long]
          val oldValue = getValue(server.config, k)
          if (newValue != oldValue && newValue <= 0) {
            val errorMsg = s"Dynamic remote log manager config update validation failed for $k=$v"
            throw new ConfigException(s"$errorMsg, value should be at least 1")
          }
    val oldValue = oldConfig.getLong(RemoteLogManagerConfig.REMOTE_LOG_INDEX_FILE_CACHE_TOTAL_SIZE_BYTES_PROP)
    val newValue = newConfig.getLong(RemoteLogManagerConfig.REMOTE_LOG_INDEX_FILE_CACHE_TOTAL_SIZE_BYTES_PROP)
    if (oldValue != newValue) {
      val remoteLogManager = server.remoteLogManagerOpt

// After
import org.apache.kafka.common.metrics.Quota;
    public void updateCopyQuota(long quota) {
        LOGGER.info("Updating remote copy quota to {} bytes per second", quota);
        rlmCopyQuotaManager.updateQuota(new Quota(quota, true));
    }

    public void updateFetchQuota(long quota) {
        LOGGER.info("Updating remote fetch quota to {} bytes per second", quota);
        rlmFetchQuotaManager.updateQuota(new Quota(quota, true));
    }

      if (RemoteLogManagerConfig.REMOTE_LOG_INDEX_FILE_CACHE_TOTAL_SIZE_BYTES_PROP.equals(k) ||
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15265: dynamic broker configs for remote fetch/copy quotas (#16078).

**Test**
Test modified in DynamicBrokerConfigTest.scala covering the corrected behaviour.
