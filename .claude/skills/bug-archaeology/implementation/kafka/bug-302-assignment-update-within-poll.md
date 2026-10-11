## Commit
69f7515079

## Ticket
KAFKA-20106

## Subsystem
Kafka Streams / Consumer internals

## Category
Race condition / missing synchronization

## Root cause
Assignment updates (`subscriptionState.assignFromSubscribed*`) happened in the background thread when the async commit / revoke callback chain completed, so `consumer.assignment()` could change between two calls made by application code outside of `poll()`. The fix moves the subscription-state update and the `onPartitionsAssigned` callback invocation into the application thread via a new `PartitionsAssignedEvent`, which is processed inside `consumer.poll()`.

## Before
```java
// AbstractMembershipManager — background thread
private void updateSubscriptionAwaitingCallback(...) {
    subscriptions.assignFromSubscribedAwaitingCallback(assignedPartitions, addedPartitions);
    notifyAssignmentChange(assignedPartitions.topicPartitions());
}
// then signalPartitionsAssigned() invoked callback immediately in background
```

## After
```java
// ConsumerMembershipManager — background thread enqueues event
protected CompletableFuture<Void> signalPartitionsAssigned(
        TopicIdPartitionSet assignedPartitions, SortedSet<TopicPartition> addedPartitions) {
    return enqueuePartitionsAssignedEvent(assignedPartitions.topicPartitions(), addedPartitions);
}
// AsyncKafkaConsumer.ApplicationThreadEventProcessor — app thread inside poll()
private void process(final PartitionsAssignedEvent event) {
    applyNewAssignment(event);          // sends ApplyAssignmentEvent to background
    // then optionally runs onPartitionsAssigned callback
}
```

## How to spot
Any place where subscription state is mutated from a non-poll thread. Search for `assignFromSubscribed` callers outside of `ApplicationEventProcessor`-style handlers. Also watch for `IllegalStateException` on `seek()`/`position()` for partitions returned by `consumer.assignment()`.

## Test
`ConsumerMembershipManagerTest`, `AsyncKafkaConsumerTest` – verify assignment change only observable inside `poll()`.
