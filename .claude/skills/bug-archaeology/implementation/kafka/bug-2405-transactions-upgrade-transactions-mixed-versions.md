**Commit**: 0af75c0e41 · **Ticket**: KAFKA-17458
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    LATEST_3_6, LATEST_3_7, DEV_BRANCH, KafkaVersion, LATEST_STABLE_METADATA_VERSION
    @matrix(from_kafka_version=[str(LATEST_3_1), str(LATEST_3_2), str(LATEST_3_3), str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(DEV_BRANCH)],
    @matrix(from_kafka_version=[str(LATEST_3_1), str(LATEST_3_2), str(LATEST_3_3), str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(DEV_BRANCH)],
    LATEST_3_6, LATEST_3_7, DEV_BRANCH, KafkaVersion, LATEST_STABLE_METADATA_VERSION
        old_kafka_version=[str(LATEST_3_7), str(LATEST_3_6), str(LATEST_3_5), str(LATEST_3_4), str(LATEST_3_3), str(LATEST_3_2), str(LATEST_3_1)],
    LATEST_3_6, LATEST_3_7, DEV_BRANCH, KafkaVersion, LATEST_STABLE_METADATA_VERSION
        from_kafka_version=[str(LATEST_3_7), str(LATEST_3_6), str(LATEST_3_5), str(LATEST_3_4), str(LATEST_3_3), str(LATEST_3_2), str(LATEST_3_1)],

// After
    LATEST_3_6, LATEST_3_7, LATEST_3_8, DEV_BRANCH, KafkaVersion, LATEST_STABLE_METADATA_VERSION
    @matrix(from_kafka_version=[str(LATEST_3_1), str(LATEST_3_2), str(LATEST_3_3), str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(LATEST_3_8), str(DEV_BRANCH)],
    @matrix(from_kafka_version=[str(LATEST_3_1), str(LATEST_3_2), str(LATEST_3_3), str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(LATEST_3_8), str(DEV_BRANCH)],
    LATEST_3_6, LATEST_3_7, LATEST_3_8, DEV_BRANCH, KafkaVersion, LATEST_STABLE_METADATA_VERSION
        old_kafka_version=[str(LATEST_3_8), str(LATEST_3_7), str(LATEST_3_6), str(LATEST_3_5), str(LATEST_3_4), str(LATEST_3_3), str(LATEST_3_2), str(LATEST_3_1)],
    LATEST_3_6, LATEST_3_7, LATEST_3_8, DEV_BRANCH, KafkaVersion, LATEST_STABLE_METADATA_VERSION
        from_kafka_version=[str(LATEST_3_8), str(LATEST_3_7), str(LATEST_3_6), str(LATEST_3_5), str(LATEST_3_4), str(LATEST_3_3), str(LATEST_3_2), str(LATEST_3_1)],
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17458 add 3.8 to transactions_upgrade_test.py, transactions_mixed_versions_test.py, and kraft_upgrade_test.py (#17.

**Test**
Test modified in kraft_upgrade_test.py covering the corrected behaviour.
