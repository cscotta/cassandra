**Commit**: a577d30d38 · **Ticket**: KAFKA-17076
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
In each round of cleaning, retain the last batch even if it's empty, so that logEndOffset info will not get lost after compaction. Reviewers: Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
      cleanSegments(log, group, offsetMap, currentTime, stats, transactionMetadata, legacyDeleteHorizonMs)
                                 legacyDeleteHorizonMs: Long): Unit = {
            log.config.maxMessageSize, transactionMetadata, lastOffsetOfActiveProducers, stats, currentTime = currentTime)
          else if (discardBatchRecords)
    cleaner.cleanSegments(log, segments, map, 0L, stats, new CleanedTransactionMetadata, -1)

// After
      cleanSegments(log, group, offsetMap, currentTime, stats, transactionMetadata, legacyDeleteHorizonMs, upperBoundOffset)
   * @param upperBoundOffsetOfCleaningRound The upper bound offset of this round of cleaning
                                 legacyDeleteHorizonMs: Long,
                                 upperBoundOffsetOfCleaningRound: Long): Unit = {
            log.config.maxMessageSize, transactionMetadata, lastOffsetOfActiveProducers, upperBoundOffsetOfCleaningRound, stats, currentTime = currentTime)
   * @param upperBoundOffsetOfCleaningRound Next offset of the last batch in the source segment
                             upperBoundOffsetOfCleaningRound: Long,
          else if (batch.nextOffset == upperBoundOffsetOfCleaningRound) {
            // retain the last batch of the cleaning round, even if it's empty, so that last offset information
            // is not lost after cleaning.
            BatchRetention.RETAIN_EMPTY
          } else if (discardBatchRecords)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17076: retain last batch in each round of cleaning (#17193).

**Test**
Test modified in LogCleanerTest.scala covering the corrected behaviour.
