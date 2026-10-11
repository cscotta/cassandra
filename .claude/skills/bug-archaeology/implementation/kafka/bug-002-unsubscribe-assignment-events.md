**Commit**: 4b9eddc132 · **Ticket**: KAFKA-20428
**Subsystem**: clients — AsyncKafkaConsumer / consumer internals
**Category**: State not cleaned up

**Root cause**
`AsyncKafkaConsumer.unsubscribe()` calls `processBackgroundEvents()` to drain the background queue before completing the unsubscribe. If a reconciliation completed just before unsubscribing, a `PARTITIONS_ASSIGNED` or `STREAMS_TASKS_ASSIGNED` event was sitting in the queue. Processing those assignment events during unsubscribe attempted to update the subscription state with new partitions, which was inconsistent with an ongoing unsubscribe and could throw `IllegalArgumentException`. The events should only be consumed by `poll()`.

**Before/After**
```java
// Before (unsubscribe path)
processBackgroundEvents(unsubscribeEvent.future(), timer,
    e -> (e instanceof GroupAuthorizationException || e instanceof TopicAuthorizationException));

// After
processBackgroundEvents(unsubscribeEvent.future(), timer,
    e -> (e instanceof GroupAuthorizationException || e instanceof TopicAuthorizationException),
    true);   // skipAssignmentEvents=true
```

**How to spot**
Search for callers of `processBackgroundEvents` that do not pass the `skipAssignmentEvents` flag. Assignment events carry side-effects on `SubscriptionState`; any code path other than `poll()` that drains the background queue should explicitly skip them. The race window is narrow: it only opens when a reconciliation completes in the ~milliseconds before `unsubscribe()` is called.

**Test**
`AsyncKafkaConsumerTest#testUnsubscribeWithPendingAssignmentEvent` (parameterized over `PartitionsAssignedEvent` and `StreamsTasksAssignedEvent`) — verifies that when the background queue contains an assignment event and `unsubscribe()` is called, no exception is thrown and the future completes exceptionally rather than being processed.
