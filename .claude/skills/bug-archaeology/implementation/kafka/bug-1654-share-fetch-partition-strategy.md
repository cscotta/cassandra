## Commit

- Hash: 4e24c50ba3
- Tickets: KAFKA-18404
- Subject: KAFKA-18404: Refactor DelayedShareFetch to use PartitionMaxBytesStrategy

## Subsystem

core / share groups / DelayedShareFetch

## Category

Wrong constant / default value

## Root cause

`DelayedShareFetch` was computing per-partition `maxBytes` by bundling the entire `FetchRequest.PartitionData` (which includes both the fetch offset and a hard-coded `maxBytes` ceiling) in its pending-fetch map. This embedded the sizing policy inside the data structure, making it impossible to swap strategies. The fix extracts sizing into an injectable `PartitionMaxBytesStrategy` and reduces the map value to just the fetch offset (`Long`), removing the accidental hard-coding.

## Before

```java
LinkedHashMap<TopicIdPartition, FetchRequest.PartitionData> partitionsToFetch;
// maxBytes baked into PartitionData construction — hard to change
```

## After

```java
LinkedHashMap<TopicIdPartition, Long> partitionsToFetch; // only fetch offset
PartitionMaxBytesStrategy partitionMaxBytesStrategy;     // injected strategy
```

## How to spot

Data structures that carry both identity (offset) and sizing policy together. When the policy needs to change per-deployment, the coupling makes it impossible without changing the map's value type — a sign that the policy should be separated.

## Test

`DelayedShareFetchTest` updated to exercise the injected strategy; `PartitionMaxBytesStrategyTest` added.
