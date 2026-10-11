## Commit
823f0cfc8c

## Ticket
KAFKA-20257

## Subsystem
Kafka Streams / State Manager

## Category
State not cleaned up

## Root cause
`GlobalStateManagerImpl` used a single `.checkpoint` file in the global state directory for all global stores. After KIP-1035, each store should manage its own offsets. `GlobalStateManagerImpl` was updated to wrap global stores in `LegacyCheckpointingStateStore`, migrate offsets from the legacy `.checkpoint` file into each store's CF on startup, and pass per-store offset maps to `store.commit()`. The old `checkpointFile` field and the `checkpoint()` write path were removed.

## Before
```java
private final OffsetCheckpoint checkpointFile;
// checkpoint() wrote all global store offsets to a single file
// initialize() read that single file to seed all stores
```

## After
```java
// Per-store: LegacyCheckpointingStateStore wraps each global store
// migrateLegacyOffsets() reads old .checkpoint and seeds per-store CFs
LegacyCheckpointingStateStore.migrateLegacyOffsets(logPrefix, stateDirectory, null, wrappedStores);
// store.commit(Map.of(partition, offset)) called with per-store offset map
```

## How to spot
The integration test `GlobalKTableEOSIntegrationTest` references `.checkpoint` by path — update to `.checkpoint_<storeName>`. Any test asserting the global checkpoint file exists must be updated.

## Test
`GlobalStateManagerImplTest`, `GlobalKTableEOSIntegrationTest` — verify checkpoint migration and per-store offset persistence.
