**Commit**: 8db80d1f07 · **Ticket**: KAFKA-17064
**Subsystem**: clients/consumer/internals/AsyncKafkaConsumer
**Category**: Race condition / missing synchronization

**Root cause**
`AsyncKafkaConsumer.assign()` mutated `subscriptions.assignFromUser()` directly on the application thread, then fired a separate `NewTopicsMetadataUpdateRequestEvent` to the background thread. This two-step sequence was not atomic: the background thread's `ApplicationEventProcessor` could observe a partially-updated subscription state between the two events. The fix moves the entire assignment into a single `AssignmentChangeEvent` processed by the background thread, which atomically updates the subscription and triggers the metadata refresh.

**Before/After**
```java
// Before (application thread mutates subscription directly)
applicationEventHandler.add(new AssignmentChangeEvent(subscriptions.allConsumed(), time.milliseconds()));
if (subscriptions.assignFromUser(new HashSet<>(partitions)))
    applicationEventHandler.add(new NewTopicsMetadataUpdateRequestEvent());

// After (background thread performs all mutation)
AssignmentChangeEvent event = new AssignmentChangeEvent(timer.currentTimeMs(), deadlineMs, partitions);
applicationEventHandler.addAndGet(event); // blocks until background processes it
```

**How to spot**
Whenever an application-thread consumer method both mutates `SubscriptionState` directly and also enqueues an event to the background thread, there is a window where the two are out of sync. Any mutation of shared state that should be visible to the background thread must be moved into the event processor to guarantee ordering.

**Test**
`testAssign` updated to verify `addAndGet` is called instead of two separate `add` calls.
