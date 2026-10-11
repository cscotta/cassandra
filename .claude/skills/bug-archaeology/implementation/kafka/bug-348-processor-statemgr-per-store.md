## Commit
5740a26525

## Ticket
KAFKA-19712

## Subsystem
Kafka Streams / State Manager

## Category
State not cleaned up

## Root cause
`ProcessorStateManager` managed offset checkpoints by reading/writing a `.checkpoint` file shared across all stores for a task. After KIP-1035, each `RocksDBStore` manages its own offsets in a per-store column family. `ProcessorStateManager` was updated to wrap user-defined stores (that don't implement `managesOffsets()`) in `LegacyCheckpointingStateStore`, delegate offset persistence to each store's `commit()`, and migrate legacy `.checkpoint` offsets into the stores on first open.

## Before
```java
// Single .checkpoint file for all stores
this.checkpointFile = new OffsetCheckpoint(stateDirectory.checkpointFileFor(taskId));
// checkpoint() writes all offsets to that file
```

## After
```java
// Each store manages its own offsets; LegacyCheckpointingStateStore wraps user stores
// migrateLegacyOffsets() reads old .checkpoint and writes into each store's CF
LegacyCheckpointingStateStore.migrateLegacyOffsets(logPrefix, stateDirectory, taskId, storesToMigrate);
// storeMetadata.store.commit(Map.of(changelogPartition, offset)) called per-store
```

## How to spot
After any offset-persistence migration, verify that (a) old checkpoint files are migrated on first open, (b) new offset format is written on every commit, and (c) the old file is not re-created after migration. EOS tests that check `.checkpoint` path must be updated to use per-store paths (`.checkpoint_<storeName>`).

## Test
`EosIntegrationTest` uses `.checkpoint_<storeName>` path; `ProcessorStateManagerTest` verifies per-store offset delegation.
