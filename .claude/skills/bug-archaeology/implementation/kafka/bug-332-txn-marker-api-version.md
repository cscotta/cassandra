## Commit
a0203dc0fc

## Ticket
KAFKA-20322

## Subsystem
Broker / Transaction Coordinator

## Category
Wrong constant / default value

## Root cause
`TransactionMarkerChannelManager` called `NodeApiVersions` with `discoverBrokerVersions=false`. With discovery disabled, `WriteTxnMarkersRequest` used the highest client-known version regardless of what the target broker actually supported, potentially sending a version the broker could not handle. Setting `discoverBrokerVersions=true` causes the channel to negotiate the API version before sending.

## Before
```java
new NodeApiVersions(Collections.emptyList(), Collections.emptyList(), false);
// discoverBrokerVersions=false -> use max known client version
```

## After
```java
new NodeApiVersions(Collections.emptyList(), Collections.emptyList(), true);
// discoverBrokerVersions=true -> negotiate with broker
```

## How to spot
Search for `discoverBrokerVersions=false` in channel manager constructors. Any channel that sends protocol requests to peer brokers should have version discovery enabled.

## Test
`TransactionMarkerChannelManagerTest` asserts that the negotiated version is used on an upgraded cluster.
