## Commit

- Hash: 9f5d9f3cd4
- Tickets: KAFKA-18399 (7/N)
- Subject: KAFKA-18399: Remove ZK-mode topic/partition management handlers from KafkaApis (7/N)

## Subsystem

core / KafkaApis / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`KafkaApis` still contained `handleCreateTopicsRequest`, `handleDeleteTopicsRequest`, and `handleCreatePartitionsRequest` branches that delegated to ZooKeeper-backed admin paths. With ZooKeeper support removed from the broker, these dead branches were never reachable but increased code surface and maintenance cost.

## Before

Three ZK-specific handler methods present in `KafkaApis.scala` and wired into the dispatch table.

## After

Methods and their dispatch cases deleted entirely; KRaft-only paths remain.

## How to spot

Handler methods in `KafkaApis` that require a non-null `ZkSupport` or call `adminManager` / `zkClient` are exclusively ZK-mode code and should have been removed as part of the KRaft migration series.

## Test

Existing API handler tests pass without the deleted branches; KRaft integration tests cover the retained paths.
