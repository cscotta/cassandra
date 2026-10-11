## Commit

- Hash: c28d9a3486
- Tickets: KAFKA-18435
- Subject: KAFKA-18435: Remove ZooKeeper, netty, commons-io, and commons-cli dependencies

## Subsystem

build / dependency management / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`build.gradle` still declared compile-time dependencies on `zookeeper`, `netty-*`, `commons-io`, and `commons-cli` — all carried exclusively by ZooKeeper-mode code. Scala source stubs for `ZkNodeChangeNotificationListener`, `PartitionStateMachine`, and `ReplicaStateMachine` still referenced `KafkaZkClient` method signatures. These stubs were replaced with `throw new UnsupportedOperationException()` bodies and the external dependencies removed, completing the dependency-graph clean-up portion of the KRaft migration.

## Before

```groovy
implementation "org.apache.zookeeper:zookeeper:..."
implementation "io.netty:netty-handler:..."
implementation "commons-io:commons-io:..."
implementation "commons-cli:commons-cli:..."
```

## After

All four dependency declarations removed; ZK stub class bodies replaced with `UnsupportedOperationException`.

## How to spot

`build.gradle` entries for ZooKeeper, Netty (when only used by ZK transport), or Apache Commons libraries that appear only in ZK-mode code paths — they are safe to remove once all ZK code is deleted or stubbed out.

## Test

Full build passes without the removed dependencies; no ZK-related compilation unit requires them.
