## Commit

- Hash: aa22676c48
- Tickets: KAFKA-18425
- Subject: KAFKA-18425: Remove OffsetTrackingListener ZK migration helper

## Subsystem

core / metadata / OffsetTrackingListener / ZooKeeper removal

## Category

State not cleaned up

## Root cause

`OffsetTrackingListener` was a 48-line `RaftClient.Listener` whose sole purpose was to track the highest metadata log offset seen — used exclusively during the KIP-866 ZooKeeper-to-KRaft migration to allow ZK brokers to determine when they had caught up to the KRaft controller's metadata log. With the migration path removed (ZooKeeper support dropped), no code constructs or registers an `OffsetTrackingListener`, making it permanently dead code.

## Before

`OffsetTrackingListener.scala` present in `kafka.server.metadata`; kept `@volatile _highestOffset` updated via `handleCommit` and `handleLoadSnapshot`.

## After

`OffsetTrackingListener.scala` deleted entirely.

## How to spot

`RaftClient.Listener` implementations that exist only to track offsets for a migration state machine (rather than to process records) are migration-era infrastructure that should be removed once the migration path is gone.

## Test

No test needed; the class had no callers remaining after the ZK migration path was removed.
