**Commit**: 89214d033e · **Ticket**: KAFKA-17214
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
    V_0_11_0_0, V_0_10_1_0, KafkaVersion
    KafkaVersion
    LATEST_3_7, DEV_BRANCH, KafkaVersion

// After
    LATEST_3_8, V_0_11_0_0, V_0_10_1_0, KafkaVersion
    @parametrize(broker_version=str(LATEST_3_8))
    LATEST_3_8, KafkaVersion
    @parametrize(broker_version=str(LATEST_3_8))
    LATEST_3_7, LATEST_3_8, DEV_BRANCH, KafkaVersion
    @matrix(producer_version=[str(LATEST_3_8)], consumer_version=[str(LATEST_3_8)], compression_types=[["none"]], timestamp_type=[str("CreateTime")], metadata_quorum=quorum.all_non_upgrade)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17214: add 3.8.0 version to core and client system tests (#16726).

**Test**
Test modified in client_compatibility_features_test.py covering the corrected behaviour.
