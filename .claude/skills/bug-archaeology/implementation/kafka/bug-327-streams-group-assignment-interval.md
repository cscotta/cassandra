## Commit
a6eb6aef21

## Ticket
KAFKA-20291 [3/3]

## Subsystem
Group Coordinator / Streams Groups

## Category
Missing null / bounds check

## Root cause
`GroupMetadataManager` computed a new target assignment on every heartbeat for Streams groups, ignoring the `canComputeNextTargetAssignment` gate that enforces the configured assignment interval. This caused continuous assignment computation, wasting CPU and generating noise in the group log.

## Before
```java
// Streams group heartbeat path – no interval check
computeNewTargetAssignment(group, ...);
```

## After
```java
if (group.canComputeNextTargetAssignment()) {
    computeNewTargetAssignment(group, ...);
}
```

## How to spot
Compare the consumer-group, share-group, and streams-group heartbeat paths line-by-line. Any divergence in guard conditions is likely a bug in one of the three paths.

## Test
`StreamsGroupHeartbeatRequestTest` verifies assignment is not recomputed on every heartbeat.
