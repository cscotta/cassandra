**Commit**: f86659423d · **Ticket**: KAFKA-19042
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
The PR do following:
1. rewrite to new test infra
2. rewrite to java
3. move to clients-integration-tests

Reviewers: Ken Huang <s7133700@gmail.com>, Kuan-Po Tseng
<brandboat@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import java.util.function.Supplier;
    public static void pollUntilTrue(
            Consumer<byte[], byte[]> consumer,
            Supplier<Boolean> testCondition,
            long waitTimeMs, String msg
    ) throws InterruptedException {
        TestUtils.waitForCondition(() -> {
            consumer.poll(Duration.ofMillis(100));
            return testCondition.get();
        }, waitTimeMs, msg);
    }

```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19042 move plaintextconsumerassigntest to clients-integration-tests module (#19773).

**Test**
Test modified in ClientsTestUtils.java covering the corrected behaviour.
