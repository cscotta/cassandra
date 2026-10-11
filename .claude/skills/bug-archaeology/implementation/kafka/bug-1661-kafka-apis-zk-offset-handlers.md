## Commit

- Hash: 3cf2e45dc4
- Tickets: KAFKA-18399 (4/N)
- Subject: KAFKA-18399: Remove ZK-mode offset commit/fetch handlers from KafkaApis (4/N)

## Subsystem

core / KafkaApis / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`KafkaApis` still held `commitOffsetsToZookeeper` (the v0 offset-commit path that stored offsets directly in ZK) and `handleOffsetFetchRequestFromZookeeper` (v0 offset-fetch that read them back). These APIs pre-date Kafka's coordinator model and have not been exercised since ZooKeeper support was dropped. Keeping them inflated the dispatch table and obscured the active code paths.

## Before

Two ZK offset-storage handler methods plus their dispatch cases present in `KafkaApis.scala`.

## After

Both methods and their dispatch entries deleted.

## How to spot

Methods referencing `zkClient.setOrCreateConsumerOffset` / `zkClient.getConsumerOffset` or handling `API_KEY = OFFSET_COMMIT` with `version == 0` via a ZK path are v0-protocol dead code in a KRaft-only codebase.

## Test

Offset commit/fetch integration tests pass via the coordinator path; v0-ZK path not exercised.
