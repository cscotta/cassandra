**Commit**: 51bb036794 · **Ticket**: KAFKA-19589

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/AsyncKafkaConsumer.java

**Category**: Logic error in condition

**Root cause**
`AsyncKafkaConsumer.poll()` always waited for `OffsetsRequestManager.updateFetchPositions()` to complete on the network thread before collecting buffered data, even when all assigned partitions already had up-to-date positions. This cross-thread wait caused roughly 60 % higher CPU utilisation compared to `ClassicKafkaConsumer`. The new `PositionsValidator` class reads the `SubscriptionState` directly on the application thread to determine whether all partitions are already positioned; if so, the cross-thread wait is skipped entirely.

**Before**
```java
// Before: always await network thread position update
CompletableFuture<Void> posFuture = updateFetchPositions();
posFuture.get(remainingMs, TimeUnit.MILLISECONDS);
```

**After**
```java
// After: skip wait when positions already valid
if (!positionsValidator.allPositionsValid(subscriptions)) {
    CompletableFuture<Void> posFuture = updateFetchPositions();
    posFuture.get(remainingMs, TimeUnit.MILLISECONDS);
}
```

**How to spot**
In `AsyncKafkaConsumer`, look for unconditional `CompletableFuture.get()` calls in the `poll()` hot path; any future that guards work that is typically a no-op (positions are already set 99.99 % of the time) is a performance regression waiting to be short-circuited.

**Test**
Benchmark `AsyncKafkaConsumer.poll()` CPU utilisation before and after the patch and confirm it approaches the `ClassicKafkaConsumer` baseline under normal (already-positioned) conditions.
