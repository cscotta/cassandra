**Commit**: a6718dbbdb · **Ticket**: KAFKA-16725
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
A few of the share group configs in KIP-932 were defined with limits that do not match KIP-932. This PR corrects the limits. Reviewers:  Manikumar Reddy <manikumar.reddy@gmail.com>, Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
    require(shareGroupMaxRecordLockDurationMs >= shareGroupMinRecordLockDurationMs,
      s"${ShareGroupConfigs.SHARE_GROUP_MAX_RECORD_LOCK_DURATION_MS_CONFIG} must be greater than or equals " +
        s"to ${ShareGroupConfigs.SHARE_GROUP_MIN_RECORD_LOCK_DURATION_MS_CONFIG}")
    // Max should be greater than or equals to min.
    props.put(ShareGroupConfigs.SHARE_GROUP_MIN_RECORD_LOCK_DURATION_MS_CONFIG, "20")
    props.put(ShareGroupConfigs.SHARE_GROUP_MAX_RECORD_LOCK_DURATION_MS_CONFIG, "10")
    assertThrows(classOf[IllegalArgumentException], () => KafkaConfig.fromProps(props))
    props.put(ShareGroupConfigs.SHARE_GROUP_MIN_RECORD_LOCK_DURATION_MS_CONFIG, "10")
    props.put(ShareGroupConfigs.SHARE_GROUP_MAX_RECORD_LOCK_DURATION_MS_CONFIG, "20")
    props.put(ShareGroupConfigs.SHARE_GROUP_RECORD_LOCK_DURATION_MS_CONFIG, "5")
    assertThrows(classOf[IllegalArgumentException], () => KafkaConfig.fromProps(props))
    props.put(ShareGroupConfigs.SHARE_GROUP_RECORD_LOCK_DURATION_MS_CONFIG, "25")

// After
    props.put(ShareGroupConfigs.SHARE_GROUP_MIN_RECORD_LOCK_DURATION_MS_CONFIG, "10")
    assertThrows(classOf[ConfigException], () => KafkaConfig.fromProps(props))
    props.put(ShareGroupConfigs.SHARE_GROUP_MAX_RECORD_LOCK_DURATION_MS_CONFIG, "10000000")
    assertThrows(classOf[ConfigException], () => KafkaConfig.fromProps(props))
    props.put(ShareGroupConfigs.SHARE_GROUP_MIN_RECORD_LOCK_DURATION_MS_CONFIG, "1000")
    props.put(ShareGroupConfigs.SHARE_GROUP_MAX_RECORD_LOCK_DURATION_MS_CONFIG, "3600000")
    props.put(ShareGroupConfigs.SHARE_GROUP_RECORD_LOCK_DURATION_MS_CONFIG, "999")
    assertThrows(classOf[ConfigException], () => KafkaConfig.fromProps(props))
    props.put(ShareGroupConfigs.SHARE_GROUP_RECORD_LOCK_DURATION_MS_CONFIG, "3600001")
    assertThrows(classOf[ConfigException], () => KafkaConfig.fromProps(props))
    props.put(ShareGroupConfigs.SHARE_GROUP_RECORD_LOCK_DURATION_MS_CONFIG, "30000")
    assertDoesNotThrow(() => KafkaConfig.fromProps(props))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16725: adjust share group configs to match kip (#16368).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
