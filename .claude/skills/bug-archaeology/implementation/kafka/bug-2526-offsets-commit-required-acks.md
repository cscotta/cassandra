**Commit**: 944c1353a9 · **Ticket**: KAFKA-16736
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Jeff Kim <jeff.kim@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import scala.annotation.nowarn
  @nowarn("cat=deprecation")
    config.groupCoordinatorConfig.offsetCommitTimeoutMs,
    config.groupCoordinatorConfig.offsetCommitRequiredAcks
      requiredAcks = config.offsetCommitRequiredAcks,


    if (originals.containsKey(GroupCoordinatorConfig.OFFSET_COMMIT_REQUIRED_ACKS_CONFIG)) {
      warn(s"${GroupCoordinatorConfig.OFFSET_COMMIT_REQUIRED_ACKS_CONFIG} is deprecated and it will be removed in Apache Kafka 4.0.")
    }
import scala.annotation.nowarn
  @nowarn("cat=deprecation")

// After
    config.groupCoordinatorConfig.offsetCommitTimeoutMs
      requiredAcks = -1,
      config.groupCoordinatorConfig.offsetCommitTimeoutMs)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16736 remove `offsets.commit.required.acks` in 4.0 (#16938).

**Test**
Test modified in GroupMetadataManagerTest.scala covering the corrected behaviour.
