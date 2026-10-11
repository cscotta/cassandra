**Commit**: 6094882315 · **Ticket**: KAFKA-17905
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        future.thenApply((KafkaFuture.BaseFunction<Collection<Object>, Void>) results -> {
        future.thenApply((KafkaFuture.BaseFunction<Collection<Object>, Void>) results -> {
import org.apache.kafka.common.KafkaFuture;
            waiter.thenApply((KafkaFuture.BaseFunction<Long, Void>) now -> {
import org.apache.kafka.common.KafkaFuture;
        waiter.thenApply((KafkaFuture.BaseFunction<Long, Void>) now -> {
            haltFuture.thenApply((KafkaFuture.BaseFunction<String, Void>) errorString -> {

// After
        future.thenApply(results -> {
        future.thenApply(results -> {
            waiter.thenApply(now -> {
        waiter.thenApply(now -> {
            haltFuture.thenApply(errorString -> {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17905 remove the specified type of using lambda for basefunction (#17648).

**Test**
Test modified in RefreshingHttpsJwksTest.java covering the corrected behaviour.
