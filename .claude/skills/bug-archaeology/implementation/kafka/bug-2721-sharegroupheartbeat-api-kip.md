**Commit**: 0b6086ed88 · **Ticket**: KAFKA-16741
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
ShareGroupHeartbeat API support as defined in KIP-932. The heartbeat persists Group and Member information on __consumer_offsets topic. The PR also moves some of the ShareGroupConfigs to GroupCoordinatorConfigs as they should only be used in group coordinator. Reviewers: Andrew Schofield <aschofield@confluent.io>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
              files="(RecordHelpersTest|GroupMetadataManager|GroupMetadataManagerTest|OffsetMetadataManagerTest|GroupCoordinatorServiceTest|GroupCoordinatorShardTest).java"/>
        case ShareGroupConfig.SHARE_GROUP_SESSION_TIMEOUT_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case ShareGroupConfig.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case ShareGroupConfig.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case ShareGroupConfig.SHARE_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case ShareGroupConfig.SHARE_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case ShareGroupConfig.SHARE_GROUP_MAX_HEARTBEAT_INTERVAL_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case ShareGroupConfig.SHARE_GROUP_MAX_SIZE_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
    props.put(ShareGroupConfig.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG, "20")
    props.put(ShareGroupConfig.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_CONFIG, "10")
    props.put(ShareGroupConfig.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG, "10")
    props.put(ShareGroupConfig.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_CONFIG, "20")

// After
              files="(RecordHelpersTest|CoordinatorRecordHelpers|GroupMetadataManager|GroupMetadataManagerTest|OffsetMetadataManagerTest|GroupCoordinatorServiceTest|GroupCoordinatorShardTest).java"/>
        case GroupCoordinatorConfig.SHARE_GROUP_SESSION_TIMEOUT_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case GroupCoordinatorConfig.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case GroupCoordinatorConfig.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case GroupCoordinatorConfig.SHARE_GROUP_HEARTBEAT_INTERVAL_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case GroupCoordinatorConfig.SHARE_GROUP_MIN_HEARTBEAT_INTERVAL_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case GroupCoordinatorConfig.SHARE_GROUP_MAX_HEARTBEAT_INTERVAL_MS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
        case GroupCoordinatorConfig.SHARE_GROUP_MAX_SIZE_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", 0, -1)
    props.put(GroupCoordinatorConfig.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG, "20")
    props.put(GroupCoordinatorConfig.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_CONFIG, "10")
    props.put(GroupCoordinatorConfig.SHARE_GROUP_MIN_SESSION_TIMEOUT_MS_CONFIG, "10")
    props.put(GroupCoordinatorConfig.SHARE_GROUP_MAX_SESSION_TIMEOUT_MS_CONFIG, "20")
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16741: add sharegroupheartbeat api support - 2/n (kip-932) (#16573).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
