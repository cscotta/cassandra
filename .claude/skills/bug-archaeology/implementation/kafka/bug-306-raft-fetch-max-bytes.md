## Commit
4d3c65898d

## Ticket
KAFKA-19541

## Subsystem
KRaft / Raft

## Category
Wrong constant / default value

## Root cause
`KafkaRaftClient` hardcoded `MAX_FETCH_SIZE_BYTES = MAX_BATCH_SIZE_BYTES` for both regular fetches and snapshot fetches, ignoring any operator-configured value. This meant tuning `quorum.fetch.max.bytes` had no effect. `RaftLog.read()` also lacked a `maxTotalBatchBytes` parameter so callers could not control read size.

## Before
```java
private static final int MAX_FETCH_SIZE_BYTES = MAX_BATCH_SIZE_BYTES;
// RaftLog.read(startOffset, isolation, maxOffsetMetadata)
```

## After
```java
// QuorumConfig exposes fetchMaxBytes() / fetchSnapshotMaxBytes()
int fetchSize = quorumConfig.fetchMaxBytes();
// RaftLog.read(startOffset, isolation, maxOffsetMetadata, maxTotalBatchBytes)
```

## How to spot
Any constant that shadows a configurable value without delegating to the config object. Grep for `= MAX_BATCH_SIZE_BYTES` in Raft code and verify the value is not also reachable via config.

## Test
`KafkaRaftClientTest` and `QuorumConfigTest` verify the new parameter plumbing.
