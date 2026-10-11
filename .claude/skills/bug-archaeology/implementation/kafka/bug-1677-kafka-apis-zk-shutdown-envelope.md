## Commit

- Hash: 5684fc7a2e
- Tickets: KAFKA-18399 (2/N)
- Subject: KAFKA-18399: Remove ZK-mode CONTROLLED_SHUTDOWN and ENVELOPE handlers from KafkaApis (2/N)

## Subsystem

core / KafkaApis / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`KafkaApis` retained `handleControlledShutdownRequest` (ZK-mode broker shutdown), `handleEnvelope` (request forwarding wrapper used in ZK/KRaft hybrid mode), and `isForwardingEnabled` — all exclusively needed when a ZK controller was in the cluster. With ZooKeeper removed, `CONTROLLED_SHUTDOWN` and `ENVELOPE` request types never arrive and the handlers were permanently dead code.

## Before

`handleControlledShutdownRequest`, `handleEnvelope`, `isForwardingEnabled`, and their dispatch cases present in `KafkaApis.scala`.

## After

All three methods and their dispatch cases deleted.

## How to spot

`handleEnvelope` is a reliable marker of ZK/KRaft hybrid-mode forwarding infrastructure. Any method gated on `isForwardingEnabled` is also ZK-mode dead code in a KRaft-only build.

## Test

API handler tests pass; no CONTROLLED_SHUTDOWN or ENVELOPE requests generated in KRaft cluster tests.
