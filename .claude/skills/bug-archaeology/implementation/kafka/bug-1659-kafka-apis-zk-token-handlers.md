## Commit

- Hash: 33556aedc3
- Tickets: KAFKA-18399 (6/N)
- Subject: KAFKA-18399: Remove ZK-mode delegation-token request handlers from KafkaApis (6/N)

## Subsystem

core / KafkaApis / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`KafkaApis` retained `handleCreateTokenRequestZk`, `handleRenewTokenRequestZk`, and `handleExpireTokenRequestZk` — ZooKeeper-backed delegation-token handlers. After ZooKeeper removal these were unreachable dead code. Additionally, `handleRenewTokenRequest` was misplaced in the file; fixing both issues together reduced confusion.

## Before

Three ZK token handler methods plus a misplaced KRaft token renewal handler present in `KafkaApis.scala`.

## After

ZK handlers deleted; `handleRenewTokenRequest` moved to the logically consistent location among other token handlers.

## How to spot

Handler methods suffixed `*Zk` or gated on `zkSupport.isDefined` in `KafkaApis` are exclusively ZK-mode and are dead code in a KRaft-only build.

## Test

Token API integration tests continue to pass; no ZK paths exercised.
