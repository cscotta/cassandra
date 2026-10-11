## Commit

- Hash: 81b1508acb
- Tickets: KAFKA-18399 (3/N)
- Subject: KAFKA-18399: Remove ZK SCRAM handler and simplify forwardToController in KafkaApis (3/N)

## Subsystem

core / KafkaApis / MetadataSupport / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`KafkaApis` contained `handleAlterUserScramCredentialsRequest` for the ZK-backed SCRAM path. `MetadataSupport` had both `maybeForwardToController` (returning `Option`) and `forwardToControllerOrFail` (a hard-fail variant), duplicating logic maintained only for ZK/KRaft conditional dispatch. With ZooKeeper gone, the conditional wrapper is unnecessary and `maybeForwardToController` can be collapsed into a single `forwardToController`.

## Before

ZK SCRAM handler + two forwarding helpers in `MetadataSupport`.

## After

ZK SCRAM handler deleted; `maybeForwardToController` → `forwardToController`; `forwardToControllerOrFail` removed.

## How to spot

Dual forwarding helpers (`maybeForwardToController` / `forwardToControllerOrFail`) in broker metadata support that differ only in ZK vs. KRaft dispatch mode — one is always dead in a single-mode build.

## Test

SCRAM credential management tests pass through the KRaft controller path.
