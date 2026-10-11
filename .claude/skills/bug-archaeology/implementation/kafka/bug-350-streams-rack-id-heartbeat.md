## Commit
696729f6d3

## Ticket
KAFKA-20116

## Subsystem
Kafka Streams / Consumer

## Category
Missing null / bounds check

## Root cause
`StreamsRebalanceData` held a `processId`, `endpoint`, `subtopologies`, and `clientTags` but had no `rackId` field. `StreamsGroupHeartbeatRequestManager` never set `rackId` in the heartbeat request even when `client.rack` was configured, preventing rack-aware assignment for Streams groups.

## Before
```java
public StreamsRebalanceData(UUID processId, Optional<HostInfo> endpoint,
        Map<String, Subtopology> subtopologies, Map<String, String> clientTags) { ... }
// HeartbeatState.buildRequestData() never called data.setRackId(...)
```

## After
```java
public StreamsRebalanceData(UUID processId, Optional<HostInfo> endpoint,
        Optional<String> rackId,
        Map<String, Subtopology> subtopologies, Map<String, String> clientTags) {
    this.rackId = Objects.requireNonNull(rackId, "Rack ID cannot be null");
    ...
}
// StreamThread reads CommonClientConfigs.CLIENT_RACK_CONFIG and passes it
// HeartbeatState sets rackId when JOINING
streamsRebalanceData.rackId().ifPresent(data::setRackId);
```

## How to spot
When a heartbeat request has optional fields (rackId, instanceId), check that all applicable fields are wired from config to the request data object. A missing field means server-side rack-aware logic receives no rack information.

## Test
`StreamsGroupHeartbeatRequestManagerTest.testBuildingHeartbeatRequestRackIdSentWhenJoining` verifies `rackId` is set on JOINING heartbeats and absent on subsequent ones.
