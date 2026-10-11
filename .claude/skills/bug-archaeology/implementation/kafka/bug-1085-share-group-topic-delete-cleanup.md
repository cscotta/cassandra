## Commit

bf53561d16 — KAFKA-19201: Share group state not cleaned up on user topic deletion

## Ticket

KAFKA-19201

## Subsystem

Share groups / group coordinator state management

## Category

State not cleaned up

## Root cause

When user topics were deleted, the group coordinator's `onPartitionsDeleted` callback was called for consumer groups but did not trigger any share-group state cleanup. The share coordinator and group coordinator both retained per-topic share group state (offsets, acknowledgement state, partition assignments) for deleted topics. This caused stale state to be served on subsequent partition-fetch requests if topics were recreated with the same name, and leaked memory in long-running brokers with high topic churn.

## Before / After

```java
// Before — topic deletion triggers no share group cleanup
public void onPartitionsDeleted(List<TopicPartition> partitions) {
    cleanupConsumerGroupState(partitions);
    // share group state not cleaned
}

// After — both consumer and share group state cleaned on deletion
public void onPartitionsDeleted(List<TopicPartition> partitions) {
    cleanupConsumerGroupState(partitions);
    maybeCleanupShareGroupState(partitions);
}
// BrokerMetadataPublisher also calls:
shareCoordinator.onTopicsDeleted(deletedTopicIds);
```

## How to spot

Audit every `onPartitionsDeleted` / `onTopicsDeleted` callback chain. Any coordinator service that maintains per-topic state must be wired into this callback; missing a service means state for deleted topics accumulates silently.

## Test

`GroupMetadataManagerTest` was extended with a case that verifies share group state is absent after the corresponding topic is deleted.
