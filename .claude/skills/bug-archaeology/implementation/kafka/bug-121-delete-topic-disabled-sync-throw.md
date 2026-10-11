**Commit**: ecb5b6bd7e · **Ticket**: KAFKA-19269
**Subsystem**: core / server / ControllerApis
**Category**: Logic error in condition

**Root cause**
`ControllerApis.deleteTopics()` threw `InvalidRequestException` or `TopicDeletionDisabledException` synchronously using `throw` inside a method that returns a `CompletableFuture`. The caller expected the future to carry the failure; instead, the exception propagated up the call stack and bypassed the `CompletableFuture` error-handling chain entirely. In `handleDeleteTopics`, the uncaught exception was caught by the generic handler and logged as "Unexpected error", which is misleading because deleting topics with `delete.topic.enable=false` is expected, not unexpected.

**Before/After**
```java
// Before — throws synchronously, bypasses the future chain
if (!config.deleteTopicEnable) {
    if (apiVersion < 3) {
        throw new InvalidRequestException("Topic deletion is disabled.")
    } else {
        throw new TopicDeletionDisabledException()
    }
}

// After — wraps exception in a failed future so the caller handles it correctly
if (!config.deleteTopicEnable) {
    if (apiVersion < 3) {
        return CompletableFuture.failedFuture(
            new InvalidRequestException("This version does not support topic deletion."))
    } else {
        return CompletableFuture.failedFuture(new TopicDeletionDisabledException())
    }
}
```

**How to spot**
In any `async`/`CompletableFuture`-returning method, synchronous `throw` statements before the first `await`/`CompletableFuture` chain call skip the intended error path. All control-flow exits from such methods must use `CompletableFuture.failedFuture(...)`.

**Test**
`ControllerApisTest.testDeleteTopicsDisabled` — updated to use `TestUtils.assertFutureThrows` instead of `assertThrows`, verifying that the exception is carried in the returned future rather than thrown synchronously.
