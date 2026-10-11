## Commit
71449aabb6

## Ticket
KAFKA-20106

## Subsystem
Clients / Consumer

## Category
Race condition / missing synchronization

## Root cause
This is the core fix for the same issue described in bug-302, targeted specifically at the `KafkaConsumer`. Before this commit, the assignment update (`assignFromSubscribedAwaitingCallback`) and the `onPartitionsAssigned` callback both fired in the background thread. The fix introduces `PartitionsAssignedEvent` (replacing `ConsumerRebalanceListenerCallbackNeededEvent` for the assign path) and `PartitionsRemovedEvent` (for revoke/lost). The app thread processes `PartitionsAssignedEvent` inside `poll()`, sends an `ApplyAssignmentEvent` to the background to do the subscription-state update, and then runs the callback.

## Before
```java
// Background thread:
updateSubscriptionAwaitingCallback(assigned, added);  // mutates subscription state
signalPartitionsAssigned(added);                      // fires callback
```

## After
```java
// Background thread:
enqueuePartitionsAssignedEvent(assigned, added);  // sends event to app thread

// App thread (inside poll()):
applyNewAssignment(event);                        // sends ApplyAssignmentEvent to background
invokeRebalanceCallbackAndNotifyBackgroundThread(ON_PARTITIONS_ASSIGNED, ...);
```

## How to spot
`IllegalStateException` on `seek()` or `position()` for partitions returned by `consumer.assignment()` when called between two `poll()` invocations is the symptom. Check whether subscription state is mutated outside the app thread.

## Test
`ConsumerMembershipManagerTest`, `AsyncKafkaConsumerTest` — assignment is stable across `poll()` boundaries.
