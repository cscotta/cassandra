**Commit**: eb39031cd0 · **Ticket**: KAFKA-16766
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
* Timeout exception fetching offsets

* Tests

**Before/After**
```java
// Before
            return applicationEventHandler.addAndGet(listOffsetsEvent)
                            Map.Entry::getKey,
                            entry -> entry.getValue().buildOffsetAndTimestamp()));
            offsetAndTimestampMap = applicationEventHandler.addAndGet(listOffsetsEvent);
            return offsetAndTimestampMap.entrySet()
                            Map.Entry::getKey,
                            entry -> entry.getValue().offset()));

// After
            try {
                return applicationEventHandler.addAndGet(listOffsetsEvent)
                        Map.Entry::getKey,
                        entry -> entry.getValue().buildOffsetAndTimestamp()));
            } catch (TimeoutException e) {
                throw new TimeoutException("Failed to get offsets by times in " + timeout.toMillis() + "ms");
            }
            try {
                offsetAndTimestampMap = applicationEventHandler.addAndGet(listOffsetsEvent);
                return offsetAndTimestampMap.entrySet()
                        Map.Entry::getKey,
                        entry -> entry.getValue().offset()));
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16766: offset fetch timeout exception in new consumer consistent with legacy (#16125).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
