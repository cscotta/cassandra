**Commit**: 872647fe06 · **Ticket**: KAFKA-19585
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/AsyncKafkaConsumer.java
**Category**: Missing null / bounds check

**Root cause**
When the `AsyncKafkaConsumer` constructor threw an exception, the outer `try/catch` in `KafkaConsumer` called `consumer.close()`. Several private methods invoked during `close()` — `autoCommitOnClose`, `runRebalanceCallbacksOnClose`, `leaveGroupOnClose`, `stopFindCoordinatorOnClose`, and `awaitPendingAsyncCommitsAndExecuteCommitCallbacks` — only checked `groupMetadata.get().isEmpty()` before dereferencing `applicationEventHandler`, `rebalanceListenerInvoker`, or `offsetCommitCallbackInvoker`. If the constructor failed before those fields were initialised, each dereference threw a `NullPointerException`, generating noisy logs.

**Before/After**
```java
// Before: only checks group membership, not whether handler was initialised
private void autoCommitOnClose(final Timer timer) {
    if (groupMetadata.get().isEmpty())
        return;
    ...applicationEventHandler.add(...);   // NPE if applicationEventHandler is null
}

// After: also guards against uninitialised components
private void autoCommitOnClose(final Timer timer) {
    if (groupMetadata.get().isEmpty() || applicationEventHandler == null)
        return;
    ...
}
```

**How to spot**
Wherever a `close()` method invokes other instance methods that dereference lazily-initialised fields, those methods must guard against `null`. Constructor-failure paths exercise exactly this scenario. Review all paths reachable from `close()` for unchecked field dereferences and add null-guards matching each field's initialisation order in the constructor.

**Test**
`testFailConstructor` configures an invalid `metric.reporter.classes`, expects `KafkaException` from the constructor, and then asserts that no `NullPointerException` was logged by checking `LogCaptureAppender` events.
