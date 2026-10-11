**Commit**: 408ce19c62 · **Ticket**: KAFKA-18245
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{ServerConfigs, ReplicationConfigs, ServerLogConfigs}

// After
import org.apache.kafka.common.test.api.Flaky
import org.apache.kafka.server.config.{ReplicationConfigs, ServerConfigs, ServerLogConfigs}
  @Flaky("KAFKA-18245")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18245 add flaky annotation to testmetricsduringtopiccreatedelete (#18187).

**Test**
Test modified in MetricsDuringTopicCreationDeletionTest.scala covering the corrected behaviour.
