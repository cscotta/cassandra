**Commit**: db62c7cdff · **Ticket**: KAFKA-19157
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This PR adds the config group.share.max.share.sessions to
ShareGroupConfig

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        // Share groups are enabled in two cases: 1) The internal configuration to enable it is
        // explicitly set; or 2) the share rebalance protocol is enabled.

// After
        case ShareGroupConfig.SHARE_GROUP_MAX_SHARE_SESSIONS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
import static org.apache.kafka.common.config.ConfigDef.Range.atLeast;
    public static final String SHARE_GROUP_MAX_SHARE_SESSIONS_CONFIG = "group.share.max.share.sessions";
    public static final int SHARE_GROUP_MAX_SHARE_SESSIONS_DEFAULT = 2000;
    public static final String SHARE_GROUP_MAX_SHARE_SESSIONS_DOC = "The maximum number of share sessions per broker.";

            .define(SHARE_GROUP_MAX_SHARE_SESSIONS_CONFIG, INT, SHARE_GROUP_MAX_SHARE_SESSIONS_DEFAULT, atLeast(1), MEDIUM, SHARE_GROUP_MAX_SHARE_SESSIONS_DOC)
    private final int shareGroupMaxShareSessions;
        // Share groups are enabled in two cases:
        // 1. The internal configuration to enable it is explicitly set
        // 2. the share rebalance protocol is enabled.
        shareGroupMaxShareSessions = config.getInt(ShareGroupConfig.SHARE_GROUP_MAX_SHARE_SESSIONS_CONFIG);
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19157: added group.share.max.share.sessions config (#19503).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
