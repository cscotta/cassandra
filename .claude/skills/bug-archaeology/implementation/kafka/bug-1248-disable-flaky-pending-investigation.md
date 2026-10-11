**Commit**: cee55dbdec · **Ticket**: KAFKA-18794
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
KafkaShareConsumerTest is proving very flaky. The behaviour of
MockClient does not appear to match the expectations of the test. This
PR disables the flaky tests to reduce build noise. When a proper
solution has been worked out, the tests can be re-enabled.

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.Flaky;
    @Flaky("KAFKA-18488")
    @Flaky("KAFKA-18794")

// After
import org.junit.jupiter.api.Disabled;
    // This test is proving sufficiently flaky that it has been disabled pending investigation
    @Disabled
    // @Flaky("KAFKA-18488")
    // This test is proving sufficiently flaky that it has been disabled pending investigation
    @Disabled
    //@Flaky("KAFKA-18794")
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18794: disable flaky tests pending investigation (#19340).

**Test**
Test modified in KafkaShareConsumerTest.java covering the corrected behaviour.
