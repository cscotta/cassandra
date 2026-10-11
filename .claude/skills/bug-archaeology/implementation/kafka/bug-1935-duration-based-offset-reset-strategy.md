**Commit**: ae3c5dec99 · **Ticket**: KAFKA-18013
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Update AutoOffsetResetStrategy.java to support duration based reset strategy
Update OffsetFetcher related classes and unit tests

Reviewers: Andrew Schofield <aschofield@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
    private enum StrategyType {
        LATEST, EARLIEST, NONE;
    public static boolean isValid(String offsetStrategy) {
        return Arrays.asList(Utils.enumOptions(StrategyType.class)).contains(offsetStrategy);

// After
            "<li>by_duration:<duration>: automatically reset the offset to a configured <duration> from the current timestamp. <duration> must be specified in ISO8601 format (PnDTnHnMn.nS). " +
            "Negative duration is not allowed.</li>" +
    private final Map<TopicPartition, Long> durationResetOffsets;
        this.durationResetOffsets = new HashMap<>();
    public synchronized void updateDurationOffsets(final Map<TopicPartition, Long> newOffsets) {
        durationResetOffsets.putAll(newOffsets);
    }

        } else if (strategy.type() == AutoOffsetResetStrategy.StrategyType.BY_DURATION) {
            offset = durationResetOffsets.get(tp);
            if (offset == null)
                throw new IllegalStateException("MockConsumer didn't have duration offset specified, but tried to seek to timestamp");
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18013: add support for duration based offset reset strategy to kafka consumer (#17972).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
