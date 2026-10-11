**Commit**: 43350274e8 · **Ticket**: KAFKA-19156
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR removes the group.share.max.groups config. This config was used
to calculate the maximum size of share session cache. But with the new
config group.share.max.share.sessions in place with exactly this
purpose, the ShareSessionCache initialization has also been passed the
new config. Refer: [KAFKA-19156](https://issues.apache.org/jira/browse/KAFKA-19156)

Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      val shareFetchSessionCache : ShareSessionCache = new ShareSessionCache(
        config.shareGroupConfig.shareGroupMaxGroups * config.groupCoordinatorConfig.shareGroupMaxSize
      )
        case ShareGroupConfig.SHARE_GROUP_MAX_GROUPS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
import static org.apache.kafka.common.config.ConfigDef.Type.SHORT;
    public static final String SHARE_GROUP_MAX_GROUPS_CONFIG = "group.share.max.groups";
    public static final short SHARE_GROUP_MAX_GROUPS_DEFAULT = 10;
    public static final String SHARE_GROUP_MAX_GROUPS_DOC = "The maximum number of share groups.";

            .define(SHARE_GROUP_MAX_GROUPS_CONFIG, SHORT, SHARE_GROUP_MAX_GROUPS_DEFAULT, between(1, 100), MEDIUM, SHARE_GROUP_MAX_GROUPS_DOC)

// After
      val shareFetchSessionCache : ShareSessionCache = new ShareSessionCache(config.shareGroupConfig.shareGroupMaxShareSessions())
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19156: streamlined share group configs, with usage in sharesessioncache (#19505).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
