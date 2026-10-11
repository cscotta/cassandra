**Commit**: 6f4b47d61c · **Ticket**: KAFKA-20089
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
https://issues.apache.org/jira/browse/KAFKA-20089

Changing the error code needs a kip. For now updating javadocs. A kip
will be proposed later. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * @return The list of partitions, which will be empty when the given topic is not found
     * @return The list of partitions, which will be empty when the given topic is not found

// After
     * @return The list of partitions, which will be empty when the given topic is not found.
     *         Note: when both the broker config {@code auto.create.topics.enable} and the consumer
     *         config {@code allow.auto.create.topics} are {@code true}, this method may return an
     *         empty list even though the topic is being auto-created in the background. Callers
     *         should not assume the topic does not exist based solely on an empty result.
     * @return The list of partitions, which will be empty when the given topic is not found.
     *         Note: when both the broker config {@code auto.create.topics.enable} and the consumer
     *         config {@code allow.auto.create.topics} are {@code true}, this method may return an
     *         empty list even though the topic is being auto-created in the background. Callers
     *         should not assume the topic does not exist based solely on an empty result.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20089 clients - update javadocs --  while topic being created in background (#21959).

**Test**
No test added.
