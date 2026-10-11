## Commit
a80dedab97

## Ticket
KAFKA-20066

## Subsystem
Broker / Group Coordinator / Offset Management

## Category
Logic error in condition

## Root cause
Two bugs: (1) In `KafkaApis.handleOffsetCommitRequest`, topic IDs were only resolved for API version ≥ 10 (`if (version >= 10)`). Removing that guard ensures topic IDs are always resolved regardless of request version. (2) `GroupCoordinatorShard` never called `OffsetMetadataManager.onMetadataUpdate()`, so topic IDs obtained from metadata updates were never propagated to the offset commit fencing logic.

## Before
```java
// KafkaApis
if (version >= 10) { topicId = metadata.topicId(topicName); }

// GroupCoordinatorShard
// onMetadataUpdate() never forwarded to OffsetMetadataManager
```

## After
```java
// KafkaApis – version guard removed
topicId = metadata.topicId(topicName);

// GroupCoordinatorShard
@Override
public void onNewMetadataImage(...) {
    offsetMetadataManager.onMetadataUpdate(image, delta);
}
```

## How to spot
When an `if (version >= N)` guard wraps logic that should be unconditional, it is a red flag. Also search for `onMetadataUpdate` implementations on coordinator shards to verify all dependent managers are notified.

## Test
`OffsetMetadataManagerTest` verifies topic-ID fencing works across API versions after the fix.
