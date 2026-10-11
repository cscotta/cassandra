## Commit

- Hash: 1ee715473c
- Tickets: KAFKA-18446
- Subject: KAFKA-18446: Remove MetadataCacheControllerNodeProvider and ZK/KRaft switching dead code

## Subsystem

core / NodeToControllerChannelManager / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`NodeToControllerChannelManager` held `MetadataCacheControllerNodeProvider` (which read the active controller from `ZkMetadataCache`), an `isZkController` flag, `maybeResetNetworkClient`, and a `networkClientFactory` field — all artifacts of the period when a broker could be connected to either a ZK controller or a KRaft controller. With ZooKeeper removed, only the KRaft path is valid and the switching logic is permanently dead.

## Before

`MetadataCacheControllerNodeProvider`, `isZkController`, `maybeResetNetworkClient`, and `networkClientFactory` all present in `NodeToControllerChannelManager.scala`.

## After

All four artefacts deleted; a single KRaft controller node provider remains.

## How to spot

Boolean flags like `isZkController` or factory fields that select between two implementations based on ZK/KRaft mode are dead code in a KRaft-only build. Look for `networkClientFactory`-style parameters used only in one branch of a mode switch.

## Test

Controller channel manager tests pass with the simplified single-path implementation.
