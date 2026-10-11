**Commit**: d0f4c2f844 · **Ticket**: KAFKA-18441
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Signed-off-by: PoAn Yang <payang@apache.org>
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.Flaky;
    @Flaky("KAFKA-18441")

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18441: remove flaky tag on kafkaadminclienttest#testadminclientapisauthenticationfailure (#18847).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
