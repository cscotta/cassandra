## Commit

- Hash: af3f9e3a5a
- Tickets: KAFKA-18426
- Subject: KAFKA-18426: Remove FinalizedFeatureChangeListener ZK watcher

## Subsystem

core / FinalizedFeatureChangeListener / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`FinalizedFeatureChangeListener` was a 265-line class that polled a ZooKeeper `FeatureZNode` for finalized feature changes and pushed updates into `ZkMetadataCache`. In KRaft mode, finalized features are propagated via the metadata log and the `ZkMetadataCache` / `FeatureZNode` infrastructure no longer exists. The class was permanently dead code in a KRaft-only build, yet it still compiled, held a reference to `KafkaZkClient`, and was wired into the broker startup sequence.

## Before

`FinalizedFeatureChangeListener.scala` fully present; instantiated during ZK broker startup; registered as a ZK state-change handler and ZNode change handler.

## After

`FinalizedFeatureChangeListener.scala` deleted entirely.

## How to spot

Classes whose sole purpose is to watch a ZooKeeper ZNode and populate a `ZkMetadataCache` are exclusively ZK-mode infrastructure. Any such class is dead code once `KafkaZkClient` is removed from the broker.

## Test

Feature-flag propagation tests pass via the KRaft metadata log path; no ZK watcher needed.
