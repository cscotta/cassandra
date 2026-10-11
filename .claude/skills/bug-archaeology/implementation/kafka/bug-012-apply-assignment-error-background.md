**Commit**: 6918a5611d · **Ticket**: KAFKA-20382
**Subsystem**: clients — AsyncKafkaConsumer / AbstractMembershipManager
**Category**: State not cleaned up

**Root cause**
When the application thread processed a `PartitionsAssignedEvent` or `StreamsTasksAssignedEvent`, it called `applicationEventHandler.addAndGet(applyEvent)` to apply the new assignment. If this call threw (e.g., because the app thread was interrupted), the exception propagated directly to `poll()`, but no notification was sent to the background thread. The background was left with an open reconciliation that was never marked complete, stalling future reconciliations indefinitely. The fix wraps the `addAndGet` call: on any exception, a `ConsumerRebalanceListenerCallbackCompletedEvent` (or `StreamsOnTasksAssignedCallbackCompletedEvent`) carrying the error is sent to the background before rethrowing.

**Before/After**
```java
// Before — exception escapes with no notification to background
applicationEventHandler.addAndGet(applyEvent);

// After — background is notified on failure
try {
    applicationEventHandler.addAndGet(applyEvent);
} catch (Exception e) {
    KafkaException error = ConsumerUtils.maybeWrapAsKafkaException(e, "Failed to apply...");
    applicationEventHandler.add(new ConsumerRebalanceListenerCallbackCompletedEvent(
        ON_PARTITIONS_ASSIGNED, event.future(), Optional.of(error)));
    throw error;
}
```

**How to spot**
Any two-thread protocol where the background thread waits on a future that can only be completed by the application thread is at risk: if the app thread exits the code path without completing the future (on exception), the background hangs. Audit all `CompletableFuture` instances in background events for paths where `completeExceptionally` is not called on every failure branch.

**Test**
`AsyncKafkaConsumerTest#testPartitionsAssignedEventSendsErrorWhenApplyAssignmentFails` and `testStreamsTasksAssignedEventSendsErrorWhenApplyAssignmentFails` — verify that when `addAndGet(ApplyAssignmentEvent)` throws, the correct completion-with-error event is forwarded to the background thread.
