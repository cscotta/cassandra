**Commit**: 1ebca7817b · **Ticket**: KAFKA-19539
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Repartition topic records should be purged up to the currently committed
offset once `repartition.purge.interval.ms` duration has passed. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            if ((now - lastPurgeMs) > purgeTimeMs) {
                // try to purge the committed records for repartition topics if possible
                taskManager.maybePurgeCommittedRecords();
                lastPurgeMs = now;
            }


// After
        if ((now - lastPurgeMs) > purgeTimeMs) {
            // try to purge the committed records for repartition topics if possible
            taskManager.maybePurgeCommittedRecords();
            lastPurgeMs = now;
        }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldAlsoPurgeBeforeTheCommitInterval(final boolean stateUpdaterEnabled, final boolean processingThreadsEnabled) {
        final long purgeInterval = 1000L;
        final long commitInterval = Long.MAX_VALUE;
        final Properties props = configProps(false, stateUpdaterEnabled, processingThreadsEnabled);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19539: kafka streams should also purge internal topics based on user commit requests (#20234).

**Test**
Test modified in StreamThreadTest.java covering the corrected behaviour.
