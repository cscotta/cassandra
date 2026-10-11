**Commit**: c7c0de3889 · **Ticket**: KAFKA-17141
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                log.warn("The DescribeTopicPartitions API is not supported, using Metadata API to describe topics.");

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17141 'describetopicpartitions api is not supported' warning message confuses users (#16612).

**Test**
No test added.
