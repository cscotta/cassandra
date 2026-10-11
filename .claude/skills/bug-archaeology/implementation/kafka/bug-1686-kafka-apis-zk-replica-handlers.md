## Commit

- Hash: 307059c770
- Tickets: KAFKA-18399 (1/N)
- Subject: KAFKA-18399: Remove ZK-mode LeaderAndIsr, StopReplica, UpdateMetadata handlers from KafkaApis (1/N)

## Subsystem

core / KafkaApis / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`KafkaApis` still dispatched `LEADER_AND_ISR`, `STOP_REPLICA`, and `UPDATE_METADATA` request types to ZooKeeper-backed handler implementations that delegated to the ZK replica state machine. These inter-broker control plane messages are exclusively sent by the controller; in a KRaft-only cluster, the controller communicates via the metadata log rather than these legacy RPCs. The dead handlers consumed dispatch table space and were a source of confusion during code reviews.

## Before

`handleLeaderAndIsrRequest`, `handleStopReplicaRequest`, `handleUpdateMetadataRequest` (ZK variants) present in `KafkaApis.scala` with dispatch entries.

## After

All three ZK handler methods and their dispatch cases deleted.

## How to spot

Handlers for LEADER_AND_ISR / STOP_REPLICA / UPDATE_METADATA that call into `replicaManager.becomeLeaderOrFollower()` via a ZK-backed path — in KRaft these transitions are driven by the metadata log, not by these RPCs.

## Test

KRaft integration tests exercise the metadata-log-driven replica transitions; no ZK RPC path needed.
