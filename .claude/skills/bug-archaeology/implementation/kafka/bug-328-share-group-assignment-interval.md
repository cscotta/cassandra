## Commit
0f0b418190

## Ticket
KAFKA-20291 [2/3]

## Subsystem
Group Coordinator / Share Groups

## Category
Missing null / bounds check

## Root cause
Same batch-assignment-interval bug as bug-327 but for share groups. `GroupMetadataManager` computed a new target assignment on every heartbeat for share groups without checking `canComputeNextTargetAssignment()`.

## Before
```java
// Share group heartbeat path – no interval check
computeNewTargetAssignment(group, ...);
```

## After
```java
if (group.canComputeNextTargetAssignment()) {
    computeNewTargetAssignment(group, ...);
}
```

## How to spot
Same as bug-327: compare consumer, share, and streams paths for symmetry of guards.

## Test
`ShareGroupHeartbeatRequestTest` and `ShareConsumerTest` verify assignment interval is respected.
