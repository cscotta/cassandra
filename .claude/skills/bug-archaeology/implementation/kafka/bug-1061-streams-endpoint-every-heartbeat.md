## Commit

f397cbc14c — KAFKA-19256: IQ endpoint partition map sent on every StreamsGroup heartbeat

## Ticket

KAFKA-19256

## Subsystem

Kafka Streams / group coordinator heartbeat

## Category

State not cleaned up

## Root cause

`StreamsGroupHeartbeatRequestManager` serialised the full interactive-query endpoint-to-partition map and included it in every heartbeat request, even when the map had not changed since the last heartbeat. The group coordinator was receiving and re-processing redundant endpoint information on each heartbeat cycle, causing unnecessary coordinator work and inflated heartbeat payload sizes. The fix introduces `endpointInformationEpoch` on both the client (`StreamsGroupHeartbeatRequestManager`) and server (`StreamsGroup`) sides; the client increments the epoch only on actual changes and omits the map when the epoch matches the server-acknowledged epoch.

## Before / After

```java
// Before — always includes endpoint map
heartbeatRequest.setEndpointInformation(serializeEndpoints(endpointMap));

// After — only includes map when epoch advanced
if (localEpoch != serverAcknowledgedEpoch) {
    heartbeatRequest.setEndpointInformation(serializeEndpoints(endpointMap));
    heartbeatRequest.setEndpointInformationEpoch(localEpoch);
}
```

## How to spot

Look for heartbeat or metadata-sync request builders that unconditionally attach large payloads (maps, lists, topology descriptors) without comparing a local version counter against the server-acknowledged version. Any such builder will transmit stale data on every heartbeat.

## Test

`StreamsGroupHeartbeatRequestManagerTest` was extended with cases that assert the endpoint map is absent on subsequent heartbeats when no change occurred.
