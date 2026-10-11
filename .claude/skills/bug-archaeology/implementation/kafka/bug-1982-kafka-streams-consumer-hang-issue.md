**Commit**: d36b24f45f · **Ticket**: KAFKA-17299
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
When Kafka Streams skips overs corrupted messages, it might not resume previously paused partitions,
if more than one record is skipped at once, and if the buffer drop below the max-buffer limit at the same time. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            if (recordInfo.queue().size() == maxBufferedSize) {

// After
            if (recordInfo.queue().size() <= maxBufferedSize) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17299: fix kafka streams consumer hang issue (#17899).

**Test**
No test added.
