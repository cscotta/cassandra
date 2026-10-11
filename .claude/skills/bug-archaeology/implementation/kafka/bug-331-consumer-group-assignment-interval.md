## Commit
422845feef

## Ticket
KAFKA-20291 [1/3]

## Subsystem
Group Coordinator / Consumer Groups

## Category
Missing null / bounds check

## Root cause
`GroupMetadataManager` computed a new target assignment on every heartbeat for consumer groups without checking `canComputeNextTargetAssignment()`. This caused continuous assignment computation regardless of the configured `consumer.assignment.interval.ms` value.

## Before
```java
// Consumer group heartbeat path
computeNewTargetAssignment(group, ...);  // always runs
```

## After
```java
if (group.canComputeNextTargetAssignment()) {
    computeNewTargetAssignment(group, ...);
}
```

## How to spot
The assignment interval configuration (`consumer.group.assignment.interval.ms`) is meaningless if its guard is never checked. Search for `computeNewTargetAssignment` calls and verify each is guarded by `canComputeNextTargetAssignment()`.

## Test
`ConsumerGroupHeartbeatRequestTest` verifies the interval is enforced between consecutive assignments.
