## Commit
f380a042d8

## Ticket
KAFKA-20164

## Subsystem
KRaft / Raft

## Category
Missing override of default method

## Root cause
`RaftClient.Listener.handleLoadSnapshot()` was called for both committed snapshots and bootstrap snapshots. Bootstrap snapshots contain uncommitted data; mixing them with committed snapshots caused `QuorumController` and `MetadataLoader` to apply uncommitted state as if it were committed. A new `handleLoadBootstrap(SnapshotReader)` method was added to `Listener` to distinguish the two cases.

## Before
```java
// Bootstrap snapshots dispatched via handleLoadSnapshot — conflates committed/uncommitted
listener.handleLoadSnapshot(reader);
```

## After
```java
// New method for bootstrap-only snapshots
default void handleLoadBootstrap(SnapshotReader<T> reader) { }
// committed snapshots still use handleLoadSnapshot
listener.handleLoadBootstrap(bootstrapReader);
listener.handleLoadSnapshot(committedReader);
```

## How to spot
Any `Listener` or callback interface that receives both committed and uncommitted events through the same method is a correctness risk. Use distinct method names and document the commitment guarantees for each.

## Test
`KafkaRaftClientTest` and `QuorumControllerTest` assert that bootstrap and committed snapshots are delivered to the correct handler.
