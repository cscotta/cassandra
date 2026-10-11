**Commit**: 8c794fcd31 · **Ticket**: KAFKA-19821
**Subsystem**: storage/src
**Category**: Resource leak

**Root cause**
When a duplicate batch is detected, the entire MemoryRecords instance is
skipped to prevent appending duplicate data to the log. This operation
is silent, adding a log.info message here to provide better
observability. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
                                logger.trace("Duplicate batch detected, returning AppendInfo from duplicate batch with last offset: {}, first offset: {}, next offset: {}, skipped messages: {}",
                                        appendInfo.lastOffset(), appendInfo.firstOffset(), localLog.logEndOffset(), validRecords);
```

**How to spot**
When reviewing storage/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19821 duplicated batches should be logged (#20740).

**Test**
No test added.
