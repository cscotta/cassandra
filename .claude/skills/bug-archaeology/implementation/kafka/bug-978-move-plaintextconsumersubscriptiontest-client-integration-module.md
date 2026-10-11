**Commit**: 8fd0d33670 · **Ticket**: KAFKA-19042
**Subsystem**: clients/clients-integration-tests
**Category**: Logic error in condition

**Root cause**
Use Java to rewrite PlaintextConsumerSubscriptionTest by new test infra
and move it to client-integration-tests module. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            TestUtils.waitForCondition(() -> {
                try {
                    consumer.poll(Duration.ZERO);
                    return false;
                } catch (NoOffsetForPartitionException e) {
                    return true;
                }
            }, "Continuous poll not fail");

// After

    public static void waitForPollThrowException(
        Consumer<byte[], byte[]> consumer,
        Class<? extends Exception> exceptedException
    ) throws InterruptedException {
        TestUtils.waitForCondition(() -> {
            try {
                consumer.poll(Duration.ZERO);
                return false;
            } catch (Exception e) {
                return exceptedException.isInstance(e);
            }
```

**How to spot**
When reviewing clients/clients-integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19042 move plaintextconsumersubscriptiontest to client-integration-tests module (#19827).

**Test**
Test modified in ClientsTestUtils.java covering the corrected behaviour.
