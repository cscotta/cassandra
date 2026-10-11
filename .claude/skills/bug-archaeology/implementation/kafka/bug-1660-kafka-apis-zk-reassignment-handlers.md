## Commit

- Hash: 6a8ffe7f64
- Tickets: KAFKA-18399 (5/N)
- Subject: KAFKA-18399: Remove ZK-mode reassignment request handlers from KafkaApis (5/N)

## Subsystem

core / KafkaApis / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`KafkaApis` retained `handleAlterPartitionReassignmentsRequest` and `handleListPartitionReassignmentsRequest` ZooKeeper-backed handlers. These delegated to `ZkAdminManager` and `KafkaController` ZK APIs that no longer exist in the KRaft-only build, making them permanently dead code.

## Before

Two ZK reassignment handler methods and their dispatch entries present in `KafkaApis.scala`.

## After

Methods and dispatch entries removed; KRaft-only reassignment flow via `KRaftAlterPartitionReassignmentsHandler` remains.

## How to spot

Any `KafkaApis` handler that calls into `zkSupport` or `adminManager` (the ZK-backed one) for partition reassignment is dead code post-KRaft migration.

## Test

Partition reassignment integration tests exercise the KRaft path; no ZK variant needed.
