## Commit
2f047bedb7

## Ticket
KAFKA-20340

## Subsystem
Share Groups / Broker

## Category
State not cleaned up

## Root cause
`SharePartition` received both a `GroupConfigManager` directly and also a `ShareGroupConfigProvider` which itself wrapped `GroupConfigManager`. This dual-dependency was redundant: the raw `GroupConfigManager` was only used to call `groupConfig(groupId)`, which `ShareGroupConfigProvider` already exposes. Removing the direct dependency eliminates the redundancy and simplifies the object graph.

## Before
```java
// SharePartition constructor
SharePartition(GroupConfigManager groupConfigManager, ShareGroupConfigProvider provider, ...) {
    this.groupConfigManager = groupConfigManager;
    this.provider = provider;
}
// usage:
groupConfigManager.groupConfig(groupId).get().shareAutoOffsetReset()
```

## After
```java
// Only ShareGroupConfigProvider retained
ShareGroupAutoOffsetResetStrategy offsetResetStrategy = groupConfigManager.groupConfig(groupId)
    .map(GroupConfig::shareAutoOffsetReset)
    .orElseGet(GroupConfig::defaultShareAutoOffsetReset);
```

## How to spot
When the same data object is passed in two forms (raw + wrapped), check whether both are actually used or whether one can be removed. Also inspect Optional chains that could be simplified with `map`/`orElseGet`.

## Test
`SharePartitionTest` verifies offset reset strategy resolution after the refactor.
