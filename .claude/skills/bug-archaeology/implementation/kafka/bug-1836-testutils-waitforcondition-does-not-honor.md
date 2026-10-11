**Commit**: baa870a582 · **Ticket**: KAFKA-18214
**Subsystem**: test-common/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        TestUtils.retryOnExceptionWithTimeout(() -> {
            if (active.get() == null)
                throw new RuntimeException("Controller not active");
        });
    private static final long DEFAULT_TIMEOUT_MS = 60_000;
                                        String conditionDetails
    ) throws InterruptedException {
        retryOnExceptionWithTimeout(() -> {
            String conditionDetail = conditionDetails == null ? "" : conditionDetails;
            if (!testCondition.get())
                throw new TimeoutException("Condition not met within timeout " + maxWaitMs + ". " + conditionDetail);
        });

// After
        TestUtils.waitForCondition(() -> {
            return active.get() != null;
        }, 60_000, "Controller not active");
                                        String conditionDetails) throws InterruptedException {
        final long expectedEnd = System.currentTimeMillis() + maxWaitMs;
                if (testCondition.get()) {
                    return;
                }
                String conditionDetail = conditionDetails == null ? "" : conditionDetails;
                throw new TimeoutException("Condition not met: " + conditionDetail);
                    throw new AssertionError(format("Assertion failed with an exception after %s ms", maxWaitMs), e);
            Thread.sleep(Math.min(DEFAULT_POLL_INTERVAL_MS, maxWaitMs));
```

**How to spot**
When reviewing test-common/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18214 testutils#waitforcondition does not honor the maxwaitms (#18145).

**Test**
Test modified in KafkaClusterTestKit.java covering the corrected behaviour.
