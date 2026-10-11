**Commit**: 7337631675 · **Ticket**: KAFKA-19840
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Disabling the test to prevent flakiness in build. Complete analysis @
https://issues.apache.org/jira/browse/KAFKA-19840. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.common.test.api.Flaky;
import org.junit.jupiter.api.Disabled;
    @Flaky("KAFKA-19840") // https://issues.apache.org/jira/browse/KAFKA-19840
    @Disabled("KAFKA-19840") // https://issues.apache.org/jira/browse/KAFKA-19840
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: disable testsharegroupmaxsizeconfigexceeded and mark testacknowledgementcommitcallbackthrowsexception as flaky (#.

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
