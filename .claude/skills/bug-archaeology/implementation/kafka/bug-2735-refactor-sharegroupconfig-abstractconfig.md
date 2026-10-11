**Commit**: 25d775b742 · **Ticket**: KAFKA-15853
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{AbstractKafkaConfig, DelegationTokenManagerConfigs, KRaftConfigs, QuotaConfigs, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ShareGroupConfigs, ZkConfigs}
  /** Share group configuration **/
  val isShareGroupEnabled = getBoolean(ShareGroupConfigs.SHARE_GROUP_ENABLE_CONFIG)
  val shareGroupPartitionMaxRecordLocks = getInt(ShareGroupConfigs.SHARE_GROUP_PARTITION_MAX_RECORD_LOCKS_CONFIG)
  val shareGroupDeliveryCountLimit = getInt(ShareGroupConfigs.SHARE_GROUP_DELIVERY_COUNT_LIMIT_CONFIG)
  val shareGroupMaxGroups = getShort(ShareGroupConfigs.SHARE_GROUP_MAX_GROUPS_CONFIG)
  val shareGroupMaxSize = getShort(ShareGroupConfigs.SHARE_GROUP_MAX_SIZE_CONFIG)
  val shareGroupSessionTimeoutMs = getInt(ShareGroupConfigs.SHARE_GROUP_SESSION_TIMEOUT_MS_CONFIG)
  val shareGroupMinSessionTimeoutMs = getInt(ShareGroupConfigs.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG)
  val shareGroupMaxSessionTimeoutMs = getInt(ShareGroupConfigs.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_CONFIG)
  val shareGroupHeartbeatIntervalMs = getInt(ShareGroupConfigs.SHARE_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG)
  val shareGroupMinHeartbeatIntervalMs = getInt(ShareGroupConfigs.SHARE_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG)

// After
    /**
     * Checks requirement. Throw {@link IllegalArgumentException} if {@code requirement} failed.
     * @param requirement Requirement to check.
     * @param errorMessage String to include in the failure message
     */
import org.apache.kafka.server.config.{AbstractKafkaConfig, DelegationTokenManagerConfigs, KRaftConfigs, QuotaConfigs, ReplicationConfigs, ServerConfigs, ServerLogConfigs, ShareGroupConfig, ZkConfigs}

  private val _shareGroupConfig = new ShareGroupConfig(this)
  def shareGroupConfig: ShareGroupConfig = _shareGroupConfig;

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15853 refactor sharegroupconfig with abstractconfig (#16506).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
