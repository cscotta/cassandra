**Commit**: 9c7d81b436 · **Ticket**: KAFKA-10190
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

    def getOrDefault(prop: String): Long = {
      if (properties.containsKey(prop))
        properties.getProperty(prop).toLong
      else
        QuotaConfigs.QUOTA_BYTES_PER_SECOND_DEFAULT
    }
      quotaManagers.leader.updateQuota(upperBound(getOrDefault(QuotaConfigs.LEADER_REPLICATION_THROTTLED_RATE_CONFIG).toDouble))
      quotaManagers.follower.updateQuota(upperBound(getOrDefault(QuotaConfigs.FOLLOWER_REPLICATION_THROTTLED_RATE_CONFIG).toDouble))
      quotaManagers.alterLogDirs.updateQuota(upperBound(getOrDefault(QuotaConfigs.REPLICA_ALTER_LOG_DIRS_IO_MAX_BYTES_PER_SECOND_CONFIG).toDouble))
import org.apache.kafka.clients.admin.AlterConfigOp.OpType.SET
          SET)

// After
    val updatedDynamicBrokerConfigs = brokerConfig.dynamicConfig.currentDynamicBrokerConfigs
    val updatedDynamicDefaultConfigs = brokerConfig.dynamicConfig.currentDynamicDefaultConfigs

    def getOrDefault(prop: String): Long = updatedDynamicBrokerConfigs get prop match {
      case Some(value) => value.toLong
      case None => updatedDynamicDefaultConfigs get prop match {
        case Some(defaultValue) => defaultValue.toLong
        case None => QuotaConfigs.QUOTA_BYTES_PER_SECOND_DEFAULT
      }
    }
    quotaManagers.leader.updateQuota(upperBound(getOrDefault(QuotaConfigs.LEADER_REPLICATION_THROTTLED_RATE_CONFIG).toDouble))
    quotaManagers.follower.updateQuota(upperBound(getOrDefault(QuotaConfigs.FOLLOWER_REPLICATION_THROTTLED_RATE_CONFIG).toDouble))
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10190 set dynamic broker configs for entity default (#16280).

**Test**
Test modified in DynamicConfigChangeTest.scala covering the corrected behaviour.
