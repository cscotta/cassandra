## Commit

- Hash: 11459ae7e9
- Tickets: KAFKA-18453
- Subject: KAFKA-18453: Add StreamsTopology record class to Streams group coordinator

## Subsystem

group-coordinator / streams / topology management

## Category

Logic error in condition

## Root cause

The Streams group coordinator received topology information as raw fields scattered across coordinator records, with no central representation. This meant that topology-epoch checks and subtopology lookups had to be re-implemented in every place that needed topology information, creating opportunities for inconsistent handling (e.g. using a stale epoch or an incomplete subtopology map). Introducing `StreamsTopology` as a single immutable record encapsulates the epoch and subtopology map together, enforcing consistent access.

## Before

Topology epoch and subtopology map accessed as independent fields in coordinator state; no unified record.

## After

```java
public record StreamsTopology(int epoch, Map<String, Subtopology> subtopologies) {
    public Set<String> requiredTopics() { ... }
    public static StreamsTopology fromRecord(StreamsGroupTopologyValue record) { ... }
}
```

## How to spot

Coordinator state objects where logically related fields (version/epoch + data map) travel separately through the codebase — they should be co-located in a single record or value object to enforce invariants.

## Test

`StreamsTopologyTest` verifies `requiredTopics()` and `fromRecord()` factory for various topology configurations.
