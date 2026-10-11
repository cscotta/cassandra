**Commit**: 99eacf1b61 · **Ticket**: KAFKA-16914
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
KIP-932 introduces a bunch of broker and dynamic configs for share groups. This PR adds those new configs. The changes include:

1. Defined ShareGroupConfigs class which stores various share group configurations.

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{DelegationTokenManagerConfigs, KRaftConfigs, ServerConfigs, QuotaConfigs, ReplicationConfigs, ServerLogConfigs, ZkConfigs}

// After
import org.apache.kafka.server.config.{DelegationTokenManagerConfigs, KRaftConfigs, ServerConfigs, QuotaConfigs, ReplicationConfigs, ServerLogConfigs, ZkConfigs, ShareGroupConfigs}
      /** Share Group Configurations **/
      // Internal configuration used by integration and system tests.
      .defineInternal(ShareGroupConfigs.SHARE_GROUP_ENABLE_CONFIG, BOOLEAN, ShareGroupConfigs.SHARE_GROUP_ENABLE_DEFAULT, null, MEDIUM, ShareGroupConfigs.SHARE_GROUP_ENABLE_DOC)
      .define(ShareGroupConfigs.SHARE_GROUP_DELIVERY_COUNT_LIMIT_CONFIG, INT, ShareGroupConfigs.SHARE_GROUP_DELIVERY_COUNT_LIMIT_DEFAULT, between(2, 10), MEDIUM, ShareGroupConfigs.SHARE_GROUP_DELIVERY_COUNT_LIMIT_DOC)
      .define(ShareGroupConfigs.SHARE_GROUP_RECORD_LOCK_DURATION_MS_CONFIG, INT, ShareGroupConfigs.SHARE_GROUP_RECORD_LOCK_DURATION_MS_DEFAULT, atLeast(1), MEDIUM, ShareGroupConfigs.SHARE_GROUP_RECORD_LOCK_DURATION_MS_DOC)
      .define(ShareGroupConfigs.SHARE_GROUP_MIN_RECORD_LOCK_DURATION_MS_CONFIG, INT, ShareGroupConfigs.SHARE_GROUP_MIN_RECORD_LOCK_DURATION_MS_DEFAULT, atLeast(1), MEDIUM, ShareGroupConfigs.SHARE_GROUP_MIN_RECORD_LOCK_DURATION_MS_DOC)
      .define(ShareGroupConfigs.SHARE_GROUP_MAX_RECORD_LOCK_DURATION_MS_CONFIG, INT, ShareGroupConfigs.SHARE_GROUP_MAX_RECORD_LOCK_DURATION_MS_DEFAULT, atLeast(1), MEDIUM, ShareGroupConfigs.SHARE_GROUP_MAX_RECORD_LOCK_DURATION_MS_DOC)
      .define(ShareGroupConfigs.SHARE_GROUP_PARTITION_MAX_RECORD_LOCKS_CONFIG, INT, ShareGroupConfigs.SHARE_GROUP_PARTITION_MAX_RECORD_LOCKS_DEFAULT, between(100, 10000), MEDIUM, ShareGroupConfigs.SHARE_GROUP_PARTITION_MAX_RECORD_LOCKS_DOC)
      .define(ShareGroupConfigs.SHARE_GROUP_SESSION_TIMEOUT_MS_CONFIG, INT, ShareGroupConfigs.SHARE_GROUP_SESSION_TIMEOUT_MS_DEFAULT, atLeast(1), MEDIUM, ShareGroupConfigs.SHARE_GROUP_SESSION_TIMEOUT_MS_DOC)
      .define(ShareGroupConfigs.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG, INT, ShareGroupConfigs.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_DEFAULT, atLeast(1), MEDIUM, ShareGroupConfigs.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_DOC)
      .define(ShareGroupConfigs.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_CONFIG, INT, ShareGroupConfigs.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_DEFAULT, atLeast(1), MEDIUM, ShareGroupConfigs.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_DOC)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16914: added share group dynamic and broker configs (#16268).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
