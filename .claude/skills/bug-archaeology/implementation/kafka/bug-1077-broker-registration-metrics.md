## Commit

37963256d1 — KAFKA-18666: Add per-broker BrokerRegistrationState and heartbeat metrics to controller

## Ticket

KAFKA-18666

## Subsystem

KRaft controller / broker registration metrics

## Category

Missing null / bounds check

## Root cause

The KRaft controller exposed no per-broker metrics for registration state, time since last heartbeat, or the number of brokers in controlled-shutdown. Operators had no way to detect a broker that was registered but not sending heartbeats (indicating a network partition or process freeze) without scraping broker-side metrics separately. The absence of these controller-side metrics meant that split-brain and stuck-shutdown scenarios were invisible in controller dashboards.

## Before / After

```java
// Before — no per-broker metrics in ControllerMetadataMetrics
// (metrics class existed but lacked these gauges)

// After — new gauges added
metrics.addBrokerRegistrationStateGauge(brokerId, state);
metrics.addTimeSinceLastHeartbeatGauge(brokerId, heartbeatTracker);
// ControlledShutdownBrokerCount gauge added at controller level
```

## How to spot

Inspect `ControllerMetadataMetrics` for the presence of `BrokerRegistrationState`, `TimeSinceLastHeartbeatReceivedMs`, and `ControlledShutdownBrokerCount` gauges. Their absence means the controller cannot be monitored for broker-liveness regressions without polling individual broker JMX endpoints.

## Test

`ControllerMetadataMetricsTest` was extended to assert that these gauges are registered with correct initial values and update when broker state changes.
