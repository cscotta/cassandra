**Commit**: c85e09f7a5 · **Ticket**: KAFKA-19060
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/KafkaConsumer.java
**Category**: Missing null / bounds check

**Root cause**: The `offsetsForTimes` Javadoc stated `null` would be returned for a partition "if there is no such message", but omitted the equally important case where the timeout expires before the broker responds. Callers who only guarded against the no-message case could still dereference a `null` entry when the request times out, causing NPEs in production code. The fix updates both overloads of `offsetsForTimes` to document both null-returning conditions.

**Before/After**:
```java
// BEFORE – only no-message case documented
* @return ... {@code null} will be returned for the partition if there is no
*         such message.

// AFTER – timeout case also documented
* @return ... If the timestamp and offset for a specific partition cannot be
*         found within the default timeout, and no corresponding message exists,
*         the entry in the returned map will be {@code null}
```

**How to spot**: Any API method that returns a `Map<K, V>` where `V` can be `null` must document both conditions that produce null. Search for `null` in Javadoc return sections and verify all null-producing code paths are listed.

**Test**: Pure Javadoc improvement. No test added.
