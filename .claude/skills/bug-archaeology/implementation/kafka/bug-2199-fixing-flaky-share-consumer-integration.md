**Commit**: 0d6d3effd0 · **Ticket**: KAFKA-17809
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The tests fails sometime while reading for timed out records. Moved the timeout from Thread.sleep to waituntilcondition and increased it so records can be read in cases where it took more than expected. Reviewers: ShivsundarR <rshiva1405@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
        // Allowing acquisition lock timeout to expire.
        Thread.sleep(20000);

        // The 3rd record should be reassigned to 2nd consumer when it polls.
        ConsumerRecords<byte[], byte[]> records2 = shareConsumer2.poll(Duration.ofMillis(5000));
        assertEquals(1, records2.count());
        assertEquals(2L, records2.iterator().next().offset());
        TestUtils.waitForCondition(() -> {
            int records1 = shareConsumer1Records.addAndGet(shareConsumer1.poll(Duration.ofMillis(2000)).count());
            return records1 == 2;
        }, DEFAULT_MAX_WAIT_MS, 100L, () -> "Failed to consume records for share consumer 1");
        TestUtils.waitForCondition(() -> {

// After
        // The 3rd record should be reassigned to 2nd consumer when it polls, kept higher wait time
        // as time out for locks is 15 secs.
        TestUtils.waitForCondition(() -> {
            ConsumerRecords<byte[], byte[]> records2 = shareConsumer2.poll(Duration.ofMillis(200));
            return records2.count() == 1 && records2.iterator().next().offset() == 2L;
        }, 30000, 100L, () -> "Didn't receive timed out record");
        TestUtils.waitForCondition(() -> shareConsumer1Records.addAndGet(shareConsumer1.poll(Duration.ofMillis(2000)).count()) == 2,
            DEFAULT_MAX_WAIT_MS, 100L, () -> "Failed to consume records for share consumer 1");
        TestUtils.waitForCondition(() -> shareConsumer.poll(Duration.ofMillis(2000)).count() == 1,
            DEFAULT_MAX_WAIT_MS, 100L, () -> "Failed to consume records for share consumer, metadata sync failed");
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17809: fixing flaky share consumer integration test (#17515).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
