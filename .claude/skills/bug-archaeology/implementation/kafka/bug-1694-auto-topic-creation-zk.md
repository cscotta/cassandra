## Commit

- Hash: 5efaae65c6
- Tickets: KAFKA-18432
- Subject: KAFKA-18432: Remove ZK-mode auto-topic-creation path from AutoTopicCreationManager

## Subsystem

core / AutoTopicCreationManager

## Category

State not cleaned up

## Root cause

`AutoTopicCreationManager` had a `createTopicsInZk` code path that called `AdminManager` and `KafkaController` — ZK-specific components — when the broker was in ZooKeeper mode. The companion object factory had optional parameters for `AdminManager` and `KafkaController` to support both modes. With ZooKeeper removed, both the ZK path and its optional wiring are dead code. The `channelManager` field (used for the KRaft forwarding path) was made mandatory.

## Before

```scala
object AutoTopicCreationManager {
  def apply(config, metadataCache, channelManager = None,
            adminManager = None, controller = None): AutoTopicCreationManager
}
// createTopicsInZk() branch inside the class
```

## After

```scala
class AutoTopicCreationManager(config, metadataCache, channelManager: NodeToControllerChannelManager)
// createTopicsInZk() method deleted
```

## How to spot

Companion-object factory methods with `Option`-typed parameters for ZK vs KRaft components — once one mode is removed, the `Option` becomes meaningless and the corresponding path dead.

## Test

Auto-topic-creation integration tests pass through the KRaft channel-manager path.
