## Commit

f01e5aa964 — KAFKA-19145: fetchEpochEndOffsets returns Scala Option instead of Java Map

## Ticket

KAFKA-19145

## Subsystem

Replication / leader endpoint Scala-to-Java migration

## Category

Wrong serialization / deserialization

## Root cause

`LeaderEndPoint.fetchEpochEndOffsets` returned `Map[TopicPartition, EpochEndOffset]` as a Scala `Map`. Java callers in `TierStateMachine` and `AbstractFetcherThread` received the Scala `Map` as a raw `scala.collection.Map`, not a `java.util.Map`. Calling standard Java map operations (`get`, `containsKey`) on a Scala map without conversion either failed at runtime or silently returned `null` for keys that were present in the map. The fix migrates `LeaderEndPoint` and the fetcher classes to Java, using `java.util.Map` throughout and removing all Scala collection conversions.

## Before / After

```scala
// Before — returns Scala Map, Java callers get wrong type
def fetchEpochEndOffsets(
    partitions: Map[TopicPartition, EpochData]
): Map[TopicPartition, EpochEndOffset]
```

```java
// After — returns Java Map, no conversion needed
Map<TopicPartition, EpochEndOffset> fetchEpochEndOffsets(
    Map<TopicPartition, OffsetForLeaderEpochRequestData.OffsetForLeaderPartition> partitions);
```

## How to spot

Search for Scala methods that return `Map[K, V]` and are called from Java classes. Java code receiving a Scala `Map` cannot use `java.util.Map` methods directly; any `.get(key)` call returns a Scala `Option`, not `null`, causing silent logic errors when callers treat `null` as "key absent."

## Test

`TierStateMachineTest` (Java) was updated to call `fetchEpochEndOffsets` and assert that the returned `java.util.Map` contains the expected partition entries.
