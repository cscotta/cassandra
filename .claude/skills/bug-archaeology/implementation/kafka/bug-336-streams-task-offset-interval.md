## Commit
b43d70885d

## Ticket
KAFKA-18652

## Subsystem
Kafka Streams / Group Coordinator

## Category
Missing null / bounds check

## Root cause
`StreamsHeartbeatResponse` carried a `heartbeatIntervalMs` field but had no `taskOffsetIntervalMs` field. Without it, the broker could not instruct Streams clients how frequently to report warmup-task changelog offsets (KIP-1071). Clients defaulted to -1 and sent offsets on every heartbeat, overloading the broker.

## Before
```java
// StreamsRebalanceData
private final AtomicInteger heartbeatIntervalMs = new AtomicInteger(-1);
// No taskOffsetIntervalMs field
```

## After
```java
private final AtomicInteger taskOffsetIntervalMs = new AtomicInteger(-1);

public void setTaskOffsetIntervalMs(final int taskOffsetIntervalMs) {
    this.taskOffsetIntervalMs.set(taskOffsetIntervalMs);
}
// GroupConfig gains STREAMS_TASK_OFFSET_INTERVAL_MS_CONFIG (default 60000 ms)
// GroupCoordinatorConfig gains STREAMS_GROUP_MIN_TASK_OFFSET_INTERVAL_MS_CONFIG (default 15000 ms)
```

## How to spot
When a heartbeat response needs to communicate a new per-client interval, both the server-side config (`GroupCoordinatorConfig`, `GroupConfig`) and the client-side data holder (`StreamsRebalanceData`) must be updated in tandem. A missing field in either means the interval is never communicated.

## Test
`StreamsGroupHeartbeatRequestTest` verifies both members receive the correct `taskOffsetIntervalMs` and that dynamic config changes propagate.
