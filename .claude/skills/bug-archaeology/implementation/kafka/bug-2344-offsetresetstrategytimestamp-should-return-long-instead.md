**Commit**: 265c5160ac · **Ticket**: KAFKA-17585
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            Long timestamp = offsetResetStrategyTimestamp(partition);
            if (timestamp != null)
                offsetResetTimestamps.put(partition, timestamp);
    private Long offsetResetStrategyTimestamp(final TopicPartition partition) {

// After
            offsetResetTimestamps.put(partition, offsetResetStrategyTimestamp(partition));
    private long offsetResetStrategyTimestamp(final TopicPartition partition) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17585 `offsetresetstrategytimestamp` should return `long` instead of `long` (#17252).

**Test**
No test added.
