## Commit
4ce6c0467f

## Ticket
KAFKA-20306

## Subsystem
Kafka Streams / Admin Tools

## Category
Incorrect filtering / wrong query result

## Root cause
`StreamsGroupCommand.getCommittedOffsets()` filtered only `sourceTopics` when deciding which offsets to return or reset, omitting `repartitionSourceTopics`. As a result, reset and delete operations silently ignored committed offsets for repartition topics, leaving stale offsets in the coordinator.

## Before
```java
return committedOffsets.entrySet().stream()
    .filter(e -> sourceTopics.contains(e.getKey().topic()))
    .collect(...);
```

## After
```java
// getInputTopicOffsets() handles source-only for reset/delete
// getCommittedOffsets() now includes repartition source topics
Set<String> allInputTopics = new HashSet<>(sourceTopics);
allInputTopics.addAll(repartitionSourceTopics);
return committedOffsets.entrySet().stream()
    .filter(e -> allInputTopics.contains(e.getKey().topic()))
    .collect(...);
```

## How to spot
When filtering committed offsets by topic name, ensure all input-topic categories are represented. Repartition topics look like internal topics but are consumed by source nodes and must be included in any reset/delete operation.

## Test
`StreamsGroupCommandTest` verifies repartition offsets are included in the result set.
