## Commit
a40c8769ce

## Ticket
KAFKA-19712

## Subsystem
Kafka Streams / State Manager

## Category
Logic error in condition

## Root cause
`StateManager` had two separate methods `flush()` and `checkpoint()` called in sequence, with a `StateManagerUtil.checkpointNeeded()` guard that could skip the checkpoint. The guard used an `offsetSnapshotSinceLastFlush` field that required careful initialization (null vs. empty map) to control first-run behavior. This complexity caused bugs when the field was not initialized correctly on task recycling. The fix replaces both methods with a single `commit()` method and removes `enforceCheckpoint` and `offsetSnapshotSinceLastFlush` entirely.

## Before
```java
// StateManager interface
void flush();
void checkpoint();
// AbstractTask
public void maybeCheckpoint(final boolean enforceCheckpoint) {
    if (StateManagerUtil.checkpointNeeded(enforceCheckpoint, offsetSnapshotSinceLastFlush, ...)) {
        stateMgr.flush();
        stateMgr.checkpoint();
        offsetSnapshotSinceLastFlush = new HashMap<>(...);
    }
}
```

## After
```java
// StateManager interface
void commit();
// AbstractTask
public void maybeCheckpoint() {
    stateMgr.commit();
}
```

## How to spot
Two-phase operations (flush + checkpoint) with a skip-guard that depends on mutable state are fragile. Consolidating into a single idempotent `commit()` simplifies the invariant: always commit when called.

## Test
`DefaultStateUpdaterTest`, `StreamTaskTest`, `StandbyTaskTest` verify the simplified commit path.
