## Commit
2700824abd

## Ticket
KAFKA-20288

## Subsystem
Group Coordinator / Config

## Category
State not cleaned up

## Root cause
`GroupConfigManager` stored dynamic configs keyed by `groupId`. When all dynamic configs for a group were removed via `IncrementalAlterConfigs` with `OpType.DELETE`, the update arrived as an empty `Properties` map. The manager updated the config to an empty map but left the group's key in `configMap`, causing `listConfigResources` to still return the group as having dynamic config.

## Before
```java
public void updateGroupConfig(String groupId, Properties newGroupConfig) {
    // validate and store, but never remove empty entries
    configMap.put(groupId, evaluate(newGroupConfig));
}
```

## After
```java
public void updateGroupConfig(String groupId, Properties newGroupConfig) {
    if (newGroupConfig.isEmpty()) {
        configMap.remove(groupId);
        return;
    }
    configMap.put(groupId, evaluate(newGroupConfig));
}
```

## How to spot
Any map that stores per-key state but never removes entries when the value becomes empty will leak. Search for `configMap.put` calls without a corresponding removal on empty input.

## Test
`GroupConfigManagerTest.testGroupIsRemovedWhenDynamicConfigsAreRemoved` and `DynamicConfigTest.testGroupIsRemovedWhenDynamicConfigsAreRemoved` (integration test).
