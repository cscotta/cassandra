**Commit**: 2dffe32c2a · **Ticket**: KAFKA-19249

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/MockConsumer.java

**Category**: State not cleaned up

**Root cause**
`Consumer.close(Duration)` was deprecated in Kafka 4.1 in favour of `Consumer.close(CloseOptions)`, but several production and test call sites still used the deprecated overload. `MockConsumer.close()` (the zero-argument form) delegated to `close(Duration.ofMillis(DEFAULT_CLOSE_TIMEOUT_MS))`, keeping the deprecated path live. The fix updates `MockConsumer.close()` to delegate to `close(CloseOptions.timeout(...))`, and migrates `IntegrationTestHarness`, `DynamicBrokerReconfigurationTest`, and `ConsumerTask` to the new `CloseOptions` API.

**Before**
```java
// Before
public void close() {
    close(Duration.ofMillis(DEFAULT_CLOSE_TIMEOUT_MS));
}
```

**After**
```java
// After
public void close() {
    close(CloseOptions.timeout(
        Duration.ofMillis(DEFAULT_CLOSE_TIMEOUT_MS)));
}
```

**How to spot**
Search for `consumer.close(Duration` calls across the codebase; each is a remaining use of the deprecated overload and should be replaced with `CloseOptions.timeout(...)`.

**Test**
Run all affected integration tests after migration and confirm no deprecation warnings are emitted for `Consumer.close(Duration)`.
